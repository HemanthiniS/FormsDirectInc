package com.formsdirectinc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.List;

public class CustomHttpServletRequest extends HttpServletRequestWrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomHttpServletRequest.class);

  public CustomHttpServletRequest(HttpServletRequest request) {
    super(request);
  }

  @Override
  public Cookie[] getCookies() {
    Cookie[] cookies = super.getCookies();

    List<Cookie> cookieList = new ArrayList<>();

    for(Cookie cookie : cookies) {
      if(!cookie.getName().equals("id.i")) {
        cookieList.add(cookie);
      } else {
        LOGGER.info("Skipping id.i cookie with value {} from request", cookie.getValue());
      }
    }

    Cookie[] modifiedCookies = new Cookie[cookieList.size()];
    cookieList.toArray(modifiedCookies);

    return modifiedCookies;
  }
}
