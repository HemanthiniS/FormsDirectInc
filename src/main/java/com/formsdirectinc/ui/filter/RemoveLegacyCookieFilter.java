package com.formsdirectinc.ui.filter;

import com.formsdirectinc.security.CryptoDelegate;
import com.formsdirectinc.utils.CustomHttpServletRequest;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RemoveLegacyCookieFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoveLegacyCookieFilter.class);

  private CryptoDelegate cryptoDelegate;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    WebApplicationContext webApplicationContext = WebApplicationContextUtils.
      getWebApplicationContext(filterConfig.getServletContext());
    cryptoDelegate = (CryptoDelegate) webApplicationContext.getBean("cryptoDelegate");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;

    Cookie[] cookies = httpServletRequest.getCookies();
    Cookie encryptedUserIdCookie = null;

    if(cookies != null) {
      for(Cookie cookie : cookies) {
        if(cookie.getName().equals("id.i")) {
          encryptedUserIdCookie = cookie;
          break;
        }
      }
    }

    if(System.getProperty("fd.security.posture").equals("strong") &&
      encryptedUserIdCookie != null &&
      encryptedUserIdCookie.getValue().length() < 44) {
      try {
        cryptoDelegate.decrypt(encryptedUserIdCookie.getValue());
      } catch (EncryptionOperationNotPossibleException e) {
        LOGGER.info("Error decrypting id.i cookie", e);
        /*
         * re initialize the original httpServletRequest with CustomHttpServletRequest
         * as the original httpServletRequest object is not required which has the id.i
         * cookie, thus avoiding multiple doFilter calls within the try and catch blocks.
         */
        httpServletRequest = new CustomHttpServletRequest(httpServletRequest);

        encryptedUserIdCookie.setPath("/");
        encryptedUserIdCookie.setMaxAge(0);

        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.addCookie(encryptedUserIdCookie);
      }
    }

    /*
     * we can safely use the httpServletRequest object because the object will not have
     * id.i cookie if it was encrypted using legacy (when mechanism is strong) since it
     * is removed in the catch block and for other requests it would just use the original
     * httpServletRequest object which has the id.i cookie.
     */
    chain.doFilter(httpServletRequest, response);
  }

  @Override
  public void destroy() {

  }
}
