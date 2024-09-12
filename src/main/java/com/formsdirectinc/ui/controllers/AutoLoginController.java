package com.formsdirectinc.ui.controllers;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.security.CryptoDelegate;
import com.formsdirectinc.services.AccountService;
import com.formsdirectinc.services.AutoLoginService;
import com.formsdirectinc.services.RegistrationService;
import com.formsdirectinc.services.account.AccountDelegate;
import com.formsdirectinc.ui.auth.Authenticator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by subash on 7/29/19.
 */

@Controller
public class AutoLoginController extends Authenticator {

    private static Logger LOGGER = Logger.getLogger(CreateAccountsController.class);
    private static String LOGIN_URI = "redirect:logincheck.do?next=%s";
    private static String ERROR_VIEW = "error";
    private static String ACTION_TO_APPLICATIONCENTER= "/application-center/applicationcenter.do";

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountDelegate accountDelegate;
    @Autowired
    private CryptoDelegate cryptoDelegate;
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private AutoLoginService autoLoginService;

    @RequestMapping(value = "/autologin.do", method = RequestMethod.GET)
    protected String getAutoLoginRequest(final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       @RequestParam(value = "next", required = false) String next,
                                       @RequestParam(value = "uid", required = false) Long userId,
                                       @RequestParam(value = "signature", required = false) String signature,
                                       @CookieValue(value = "id.i", required = false) String userIdCookie) throws Exception {




        if (StringUtils.isBlank(next)) {
            LOGGER.error("next url is null from the request: "+userId);
            return "redirect:"+accountService.absolutizeURL(request, ACTION_TO_APPLICATIONCENTER);
        }

        if (userId == null || StringUtils.isBlank(signature)) {
            LOGGER.error("userId or signature is null from the request url redirect user to login");
            return String.format(LOGIN_URI, next);

        }

        CustomerSignup customerSignup = registrationService.retrieveCustomerSignup(userId);

        if (customerSignup == null) {

            LOGGER.error("Unknown user {} " + userId);
            if(userIdCookie !=null) {
                autoLoginService.deleteSessionCookie(request, response);
            }

            return String.format(LOGIN_URI, next);
        }

        if (autoLoginService.isValidSignature(signature, customerSignup)) {

            setUserIDCookie(userId, response);
            return "redirect:"+accountService.absolutizeURL(request,  next);

        } else {

            LOGGER.error("signature is not matched for user: " + userId);
            if(userIdCookie !=null) {
                autoLoginService.deleteSessionCookie(request, response);
            }

            return String.format(LOGIN_URI, next);
        }

    }



}