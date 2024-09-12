package com.formsdirectinc.interceptor;

import com.formsdirectinc.tenant.TenantContext;
import com.formsdirectinc.tenant.TenantContextHolder;
import com.formsdirectinc.tenant.config.TenantInterceptor;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;

public class LanguageInterceptor extends HandlerInterceptorAdapter {

    protected static Logger LOGGER = Logger.getLogger(LanguageInterceptor.class);
    private HashSet<String> languageCodes = new HashSet<String>() {{
       add("en");
       add("es");
    }};
    private static final int LANGUAGE_COOKIE_AGE = 180 * 24 * 60 * 60; // 180 days in seconds

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        LOGGER.info("Setting Language >>>>");

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession();

        String requestedLanguage = "en";
        String languageFromCookie = null;

        Cookie[] cookies = httpRequest.getCookies();
        languageFromCookie = getCookie(cookies,"lang");

        if (httpRequest.getParameter("lang") != null && isValidLanguageCode(httpRequest.getParameter("lang")) ){
            requestedLanguage = httpRequest.getParameter("lang");
        }else if(languageFromCookie != null){
            requestedLanguage = languageFromCookie;
        }else {
            requestedLanguage = "en";
        }

        setCookie(requestedLanguage, httpResponse);

        //set the locale in session
        Locale locale = Locale.getDefault();
        locale = new Locale(requestedLanguage);
        session.setAttribute("locale", locale);

        LOGGER.info("Language Set To :: " +requestedLanguage);

        return true;
    }

    public String getCookie(Cookie cookies[], String name) {
        String cookieValue = null;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(name)) {
                    cookieValue = cookies[i].getValue();
                    return cookieValue;
                }
            }
        }
        return null;
    }

    /**
     * This method sets the cookie to the response object
     *
     * @param  cookieName  the name of the cookie to be added
     * @param  response    the HttpServletResponse object
     */
    private void setCookie(String cookieName, HttpServletResponse response) {
        Cookie cookie = new Cookie("lang", cookieName);
        cookie.setMaxAge(LANGUAGE_COOKIE_AGE);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * This method checks if the lang parameter is a valid ISO Language code
     *
     * @param  languageCode  the language code
     */
    private boolean isValidLanguageCode(String languageCode) {

        return languageCodes.contains(languageCode);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws IOException {
    }
}
