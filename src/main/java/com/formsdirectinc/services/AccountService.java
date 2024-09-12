package com.formsdirectinc.services;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.security.CryptoDelegate;
import com.formsdirectinc.services.account.AccountDelegate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Base64;

@Service
public class AccountService {
    private static Logger LOGGER = Logger.getLogger(AccountService.class);

    @Autowired
    private CryptoDelegate cryptoDelegate;

    @Autowired
    private AccountDelegate accountDelegate;

    /**
     * Validate a URL that's not absolute, either relative or absolute, to make
     * sure it's correctly formed, and add scheme and server, and context path
     * if URL is relative.
     *
     * @param request     HttpServletRequest from which we get the context path
     * @param relativeURL String URL that we validate and convert to absolute
     * @return An absolute version of the URL provided
     * @throws UnsupportedEncodingException
     * @throws MalformedURLException
     */
    public String absolutizeURL(HttpServletRequest request, String relativeURL) throws UnsupportedEncodingException, MalformedURLException {

        //
        // Decode the relativeURL parameter.
        //

        relativeURL = URLDecoder.decode(relativeURL, "UTF-8");

        //
        // If the path is not relative, add the context path.
        //

        StringBuilder file = new StringBuilder();

        if (!relativeURL.startsWith("/")) {
            file.append(request.getContextPath());
            file.append("/");
        }

        file.append(relativeURL);

        //
        // Create the URL
        //

        URL url = new URL(request.getScheme(), request.getServerName(),
                file.toString());

        LOGGER.debug("Validated relativeURL parameter: " + url.toString());
        return url.toString();
    }

    public String getBaseUrl(HttpServletRequest request) {
        String base = String.format("%s://%s", request.getScheme(), request.getServerName());
        int port = request.getServerPort();
        if (request.getScheme().equals("http") && port == 80) {
            port = 0;
        } else if (request.getScheme().equals("https") && port == 443) {
            port = 0;
        }
        if (port != 0) {
            base += ":" + port;
        }
        return base;
    }

    //
    // Test if user ID cookie, id.i, is set
    //

    public boolean userIDCookieSet(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("id.i")) {
                    return true;
                }
            }
        }

        return false;
    }

    //
    // Determine if a customerSignup Object exists in session scope, and if its
    // emailId property
    // is set (this is required because there are JSP pages that use
    // <jsp:useBean> for CustomerSignup
    // rather indiscriminately.) If these two conditions are fulfilled, the user
    // is assumed to be signed-in.
    //

    public boolean isCustomerSignupInSession(HttpSession session) {
        return (null != session.getAttribute("customerSignup") && ((CustomerSignup) session
                .getAttribute("customerSignup")).getId() > 0);
    }

    //
    // If the single sign-on cookie is set, sign-in the user - place the
    // CustomerSignup object in session scope
    //

    public String getCookieValue(String cookieName, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    try {
                        String cookieValue = cookie.getValue();
                        cookieValue = cookieValue.replaceAll("\\+", "%2b");
                        return URLDecoder.decode(cookieValue, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        LOGGER.error("Error decoding the cookie", e);
                        throw new RuntimeException(e);
                    }

                }
            }
        }

        return null;
    }

    //
    // If the user ID cookie "id.i" exists, decrypt the cookie to get the user
    // ID. Unmarshal the
    // CustomerSignup for the user ID and return.
    //

    public CustomerSignup retrieveCustomerSignupFromCookie(HttpServletRequest request) {
        CustomerSignup customerSignup;

        String encUserId = getCookieValue("id.i", request);

        if (encUserId != null) {
            try {
                customerSignup = accountDelegate.retrieveCustomerSignup(Long
                        .parseLong(cryptoDelegate.decrypt(
                                encUserId)));
            } catch (Exception e) {
                LOGGER.error("Error decrypting encrypted user ID cookie", e);
                throw new RuntimeException(e);
            }

            return customerSignup;
        }

        return null;
    }

    public void setLanguageCookie(String language, HttpServletResponse response) {
        //
        // Our default language is Engrish
        //

        if (language == null) {
            language = "en";
        }

        Cookie langCookie = new Cookie("lang", language);
        langCookie.setMaxAge(180 * 24 * 60 * 60); // 6 months
        langCookie.setPath("/"); // Applies to the whole site, not just this
        // web-app

        LOGGER.debug("Setting lang cookie with value: " + language);
        response.addCookie(langCookie);
    }

    public boolean signInUserWithCookie(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        CustomerSignup customerSignup = retrieveCustomerSignupFromCookie(request);

        if (customerSignup != null) {
            session.setAttribute("customerSignup", customerSignup);
            setLanguageCookie(customerSignup.getLanguage(), response);
            return true;
        }

        return false;
    }

    public boolean signedIn(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        // 1. Does cookie exist? If not, return false
        if (!userIDCookieSet(request)) {
            return false;
        }

        if (!isCustomerSignupInSession(session)) {
            signInUserWithCookie(request, response, session);
            return true;
        }
        return true;
    }

    public void setUserIDCookie(long userID, HttpServletResponse response) {
        LOGGER.debug("Setting user ID cookie for user ID: " + userID);
        try {
            Cookie uidCookie = new Cookie("id.i", cryptoDelegate.encrypt(Long
                    .toString(userID)));
            uidCookie.setMaxAge(-1);
            uidCookie.setPath("/");

            response.addCookie(uidCookie);

        } catch (Exception e) {
            LOGGER.error("Exception setting user ID cookie for user ID: "
                    + userID, e);
        }
    }

    public void setUserIDCookieCrossSite(long userID, HttpServletResponse response) {
        LOGGER.debug("Setting user ID cookie for user ID: " + userID);
        try {
            Cookie uidCookie = new Cookie("id.i", cryptoDelegate.encrypt(Long
                    .toString(userID)));
            uidCookie.setMaxAge(-1);
            uidCookie.setPath("/");
            uidCookie.setSecure(true);
            String headerValue = String.format("%s=%s; %s=%s; %s; %s=%s", uidCookie.getName(), uidCookie.getValue(), "path", "/", "Secure", "SameSite", "None");
            response.addHeader("Set-Cookie", headerValue);

        } catch (Exception e) {
            LOGGER.error("Exception setting user ID cookie for user ID: "
                    + userID, e);
        }
    }

    public void deleteUserIDCookie(long userID, HttpServletResponse response) {
        LOGGER.debug("Delete user ID cookie with userID: " + userID);
        try {
            Cookie uidCookie = new Cookie("id.i", null);
            uidCookie.setMaxAge(0);
            uidCookie.setPath("/");
            response.addCookie(uidCookie);
        } catch (Exception e) {
            LOGGER.error("Exception deleting user ID cookie with user ID: " + userID, e);
        }
    }

    public String getLanguageFromCookie(HttpServletRequest request) {
        // If lang cookie is present then return language as ethier "es" or "en"
        // otherwise return language as "en"

        String language = getCookieValue("lang", request);
        return (language == null ? "en" : language);
    }

    public String generatePasswordResetURL(String emailId, String userId, int timeToExpireInSecs, String language) {

        String linkExpiryTimeMillis = Long.toString(System.currentTimeMillis() + (timeToExpireInSecs * 1000));
        String signature = generateSignature(userId, linkExpiryTimeMillis);
        String token = encrypt(String.format("%s|%s|%s|%s", signature, userId, emailId, linkExpiryTimeMillis));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("token", token);
        params.set("lang", language);

        String preSignedURL = ServletUriComponentsBuilder.fromCurrentContextPath().path("/resetpassword.do")
                .queryParams(params)
                .build().toUriString();

        return preSignedURL;
    }

    public String generateSignature(String userId, String expirationTime) {
        return cryptoDelegate.urlSafeHMAC(String.format("%s%s", userId, expirationTime));
    }

    public String encrypt(String plainText) {
        return new String(Base64.getUrlEncoder().encode(cryptoDelegate.encryptExternal(plainText).getBytes()));
    }

    public String decrypt(String encodedText) {
        return cryptoDelegate.decryptExternal(new String(Base64.getUrlDecoder().decode(encodedText.getBytes())));
    }

    public long currentTimeInMilliSecs() {
        return System.currentTimeMillis();
    }

    public Cookie setCookieForCreation(String cookieName, String cookieValue) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        cookie.setSecure(true);

        return cookie;
    }

    public Cookie setCookieForDeletion(String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }
}
