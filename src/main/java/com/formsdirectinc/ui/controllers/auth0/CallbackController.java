package com.formsdirectinc.ui.controllers.auth0;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.environment.Environment;
import com.formsdirectinc.security.CryptoDelegate;
import com.formsdirectinc.services.AccountService;
import com.formsdirectinc.services.AuthService;
import com.formsdirectinc.services.BrowserDataService;
import com.formsdirectinc.services.RegistrationService;
import com.formsdirectinc.services.account.AccountCreationException;
import com.formsdirectinc.services.account.AccountDelegate;
import com.formsdirectinc.services.account.ConsentStatus;
import com.formsdirectinc.services.account.ConsentType;
import com.formsdirectinc.spring.config.RegistrationConfig;
import com.formsdirectinc.ui.auth.Authenticator;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CallbackController extends Authenticator {

    private static Logger LOGGER = Logger.getLogger(CallbackController.class);

    private final String SIGNUP_URI = "createaccounts.do?next=%s";
    private final String LOGIN_URI = "logincheck.do?next=%s";

    @Autowired
    protected AccountDelegate accountDelegate;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CryptoDelegate cryptoDelegate;

    @Autowired
    private BrowserDataService browserDataService;

    @Autowired
    private String UserAccountService;

    @Autowired
    private AuthService authService;

    @Autowired
    private Environment environment;

    @RequestMapping(value = "/callback.do", method = RequestMethod.GET)
    protected void getCallback(final HttpServletRequest request,
                               final HttpServletResponse response,
                               @RequestParam(value = "state", required = false) String next,
                               @RequestParam(value = "code", required = false) String authorizationCode,
                               @CookieValue(value = "lang", defaultValue = "en") String languageCookie,
                               @CookieValue(value = "id.i", required = false) String customerIDCookie,
                               @CookieValue(value = "eq.i", required = false) String eqFlowsCookie,
                               @CookieValue(value = "referer", required = false) String refererCookie,
                               @RequestHeader(name = "Accept-Language", required = false) String acceptLanguageHeader,
                               @RequestHeader(name = "User-Agent", required = false) String userAgentHeader,
                               @RequestHeader(name = "geoip_city", required = false) String geoIpCityHeader,
                               @RequestHeader(name = "geoip_region_name", required = false) String geoIpRegionHeader,
                               @RequestHeader(name = "geoip_country_name", required = false) String geoIpCountryHeader) {

        CustomerSignup customerSignup = null;

        try {

            MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(next).build().getQueryParams();

            if (customerIDCookie != null) {
                String userID = registrationService.decryptUserIdCookieValue(customerIDCookie);
                customerSignup = accountDelegate.retrieveCustomerSignup(Long.parseLong(userID));
                if (customerSignup != null) {
                    response.sendRedirect(accountService.absolutizeURL(request, next));
                }
            }

            JSONObject tokens = new JSONObject(authService.getTokens(request, authorizationCode));

            // id_token is null or empty redirect user to LOGIN_URI
            if (!tokens.has("id_token")) {
                LOGGER.error("The required token `id_token` to fetch the customer profile is missing.  Hence redirecting to LOGIN_URI." + tokens);
                response.sendRedirect(accountService.absolutizeURL(request, String.format(LOGIN_URI, next)));
            }

            // Decoding IdToken From response to get the user's details
            DecodedJWT decodedJWT = JWT.decode(tokens.getString("id_token"));

            LOGGER.info("Email "+decodedJWT.getClaims().get("email"));
            LOGGER.info("Sub as authId "+decodedJWT.getClaims().get("sub").asString());
            if (decodedJWT.getClaims().get("email") != null) {
                String emailId = decodedJWT.getClaims().get("email").asString();
                customerSignup = accountDelegate.retrieveProfile(emailId);
            } else if (decodedJWT.getClaims().get("sub") != null) {
                String authId = decodedJWT.getClaims().get("sub").asString();
                customerSignup = accountDelegate.retrieveProfileByAuthId(authId);
            }

            if (customerSignup == null) {

                String application = null;
                if (parameters.get("application") != null) {
                    List<String> applicationParam = parameters.get("application");
                    application = applicationParam.get(0);
                }


                customerSignup = new CustomerSignup();
                String authId = decodedJWT.getClaims().get("sub").asString();


                if (decodedJWT.getClaims().get("email") != null) {
                    customerSignup.setEmailId(decodedJWT.getClaims().get("email").asString());
                }

                if (decodedJWT.getClaims().get("given_name") != null) {
                    String firstName = decodedJWT.getClaims().get("given_name").asString();
                    customerSignup.setFirstName(firstName);
                }

                if (decodedJWT.getClaims().get("family_name") != null) {
                    String lastName = decodedJWT.getClaims().get("family_name").asString();
                    customerSignup.setLastName(lastName);

                }

                customerSignup.setAuthId(authId);
                customerSignup.setInterestedProduct(application);
                customerSignup.setLanguage(languageCookie);
                customerSignup.setSignupIPAddress(request.getRemoteAddr());
                customerSignup.setSite(environment.getProperty("website"));
                customerSignup.setProfileComplete(false);
                customerSignup.setIsGuestUser(false);

                try {
                    accountDelegate.createAccount(customerSignup);
                    LOGGER.info("User " + customerSignup.getAuthId() + " registered for "
                            + customerSignup.getInterestedProduct() + " from IP: " + customerSignup.getSignupIPAddress());

                } catch (AccountCreationException ae) {
                    LOGGER.error("Error creating account for customer: " + customerSignup.getAuthId() + " for product: "
                            + customerSignup.getInterestedProduct() + " from IP: " + customerSignup.getSignupIPAddress());
                }

                // Save browser information
                browserDataService.storeBrowserData(request.getRemoteAddr(), refererCookie, acceptLanguageHeader, userAgentHeader,
                        geoIpCityHeader, geoIpRegionHeader, geoIpCountryHeader, customerSignup);

                // Storing user visit source information
                Unirest.get("https://" + request.getServerName() + "/registration/users/visitSource").asString();

                // Store Customer Consent
                try {
                        Map<ConsentType, ConsentStatus> consents = new HashMap<>();
                        consents.put(ConsentType.EMAIL, (decodedJWT.getClaims().get("email") != null) ? ConsentStatus.OPT_IN : ConsentStatus.OPT_OUT);
                        consents.put(ConsentType.SMS, ConsentStatus.OPT_OUT);
                        registrationService.createCustomerConsent(customerSignup, consents, "Signup");
                } catch (Exception e) {
                    LOGGER.error("Error creating customer consent: " + customerSignup.getEmailId());
                }

            }

            // Storing Auth_id for existing users
            if (accountDelegate.retrieveProfileByAuthId(decodedJWT.getClaims().get("sub").asString()) == null) {
                customerSignup.setAuthId(decodedJWT.getClaims().get("sub").asString());
                if (!customerSignup.getProfileComplete()) {
                    customerSignup.setFirstName(decodedJWT.getClaims().get("given_name").asString());
                    customerSignup.setLastName(decodedJWT.getClaims().get("family_name").asString());
                }
                accountDelegate.updateProfile(customerSignup);
            }

            // Set id.i cookie
            setUserIDCookie(customerSignup.getId(), response);
            LOGGER.debug("Id.i cookie is created for: " + customerSignup.getEmailId());


            // Set authCookie so that the signup event will be fired on step-two registration page.
            Cookie authCookie = new Cookie("st", "true");
            authCookie.setMaxAge(-1);
            authCookie.setPath("/");
            response.addCookie(authCookie);

            // Updating EQ data
            if (StringUtils.isNotBlank(eqFlowsCookie)) {
                registrationService.updateEQDataCustomerId(customerSignup, eqFlowsCookie);
                response.addCookie(accountService.setCookieForDeletion("eq.i"));
            }

            response.sendRedirect(accountService.absolutizeURL(request, next));

        } catch (Exception e) {
            LOGGER.error("Error creating account for customer: {}", e);
            try {
                response.sendRedirect(accountService.absolutizeURL(request, String.format(SIGNUP_URI, next)));
            } catch (IOException io) {
                e.printStackTrace();
            }
        }
    }
}
