package com.formsdirectinc.services;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.security.CryptoDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by subash on 8/1/19.
 */

@Service
public class AutoLoginService {
    @Autowired
    private CryptoDelegate cryptoDelegate;

    public void deleteSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("id.i")) {
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
                break;
            }
        }
    }

    public boolean isValidSignature(String signature, CustomerSignup customerSignup) {
        String authorizeValues = customerSignup.getId() + ":" + customerSignup.getSignupIPAddress() + ":" + customerSignup.getSignupDate();
        return signature.equals(cryptoDelegate.urlSafeHMAC(authorizeValues));
    }
}
