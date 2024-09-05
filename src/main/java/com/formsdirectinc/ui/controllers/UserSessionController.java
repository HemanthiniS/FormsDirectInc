package com.formsdirectinc.ui.controllers;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.event.LoginEvent;
import com.formsdirectinc.security.CryptoDelegate;
import com.formsdirectinc.services.AccountService;
import com.formsdirectinc.services.RegistrationService;
import com.formsdirectinc.services.account.AccountDelegate;
import com.formsdirectinc.tenant.TenantContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;

@Controller
@RequestMapping("/")
public class UserSessionController {

    private static final String VIEW_LOGIN = "login";
    private static final String VIEW_DISCOUNT_LOGIN = "discount-signin";
    private static final String VIEW_ADMIN_ERROR = "admin_error";
    private static final String VIEW_ERROR_PAGE = "error";
    private static final String NEXT_ACTION_APPLICATIONCENTER = "/application-center/applicationcenter.do";

    private static final String CONTENT_LOGIN_FAILED = "error.email.incorrect";

    private static Logger LOGGER = Logger.getLogger(UserSessionController.class);

    @Autowired
    private CryptoDelegate cryptoDelegate;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountDelegate accountDelegate;

    @Autowired
    private RegistrationService registrationService;

    @RequestMapping(value = {"/${login.controller.action:login.do}","/${logincheck.controller.action:logincheck.do}"}, method = RequestMethod.GET)
    public String showLoginView(@RequestParam(value = "next", required = false) String next,
                                @RequestParam(value = "agentName", required = false) String agentName,
                                @RequestParam(value = "agentId", required = false) String agentId,
                                @RequestParam(value = "promoCode", required = false) String promoCode,
                                @RequestParam(value = "emailId", required = false) String emailId,
                                @RequestParam(value = "application", required = false) String application,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Model model) throws Exception {


        if (StringUtils.isEmpty(next)) {
            next = NEXT_ACTION_APPLICATIONCENTER;
        }

        if (accountService.signedIn(request, response, request.getSession())) {
            if(promoCode != null){
                next = next.contains("?") ? next + "&promoCode="+promoCode : next + "?promoCode="+promoCode;
            }
            LOGGER.debug("Redirecting to " + next);
            return "redirect:" + accountService.absolutizeURL(request, next);
        }


        String eidValue = accountService.getCookieValue("eid", request);
        if (eidValue != null) {
            emailId = cryptoDelegate.decryptExternal(eidValue);
            CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(emailId);
            if (StringUtils.isBlank(customerSignup.getAuthId())) {
                // User not created via social login
                model.addAttribute("emailID", emailId);
            }
            response.addCookie(accountService.setCookieForDeletion("eid"));
        } else if(emailId != null){
            model.addAttribute("emailID", emailId);
        }
        if(agentName != null) {
            response.addCookie(accountService.setCookieForCreation("agentName", agentName));
        }
        if(agentId != null) {
            response.addCookie(accountService.setCookieForCreation("agentId", agentId));
        }

        model.addAttribute("next", next);
        model.addAttribute("promoCode", promoCode);
        return (StringUtils.isNotEmpty(next) && next.contains("discountPayment.do"))?  VIEW_DISCOUNT_LOGIN :  VIEW_LOGIN;
    }

    @RequestMapping(value = "/${login.controller.action:login.do}", method = RequestMethod.POST)
    public String loginuser(@RequestParam("emailId") String emailId,
                            @RequestParam("password") String password,
                            @RequestParam("next") String next,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            HttpSession session,
                            Model model) throws Exception {

        String site = TenantContextHolder.getTenantId();

        if (accountService.signedIn(request, response, session)) {
            if (StringUtils.isNotEmpty(next)) {
                return "redirect:" + accountService.absolutizeURL(request, next);
            } else {
                LOGGER.error("No next parameter specified for login, cannot redirect anywhere. Referer: "
                        + request.getHeader("Referer"));
                LoginEvent.failed(site,"No next parameter specified for login, cannot redirect anywhere. Referer: "
                        + request.getHeader("Referer"),"website");
                return VIEW_ERROR_PAGE;
            }
        }
        if (accountDelegate.authenticate(emailId, password).get("status").equals("OK")) {
            LOGGER.debug("authenticateUser(): user " + emailId
                    + " logged-in successfully");
            CustomerSignup customerSignup = accountDelegate
                    .retrieveProfile(emailId);

            if (customerSignup == null || customerSignup.getId() == 0) {
                LOGGER.debug("Null result when loading customer with email ID: "
                        + emailId);
                return VIEW_ERROR_PAGE;
            }

            session.setAttribute("customerSignup", customerSignup);
            accountService.setUserIDCookie(customerSignup.getId(), response);


            if (StringUtils.isNotEmpty(next)) {
                return "redirect:" + accountService.absolutizeURL(request, next);
            } else {
                LOGGER.error("No next parameter specified for login, cannot redirect anywhere. Referer: "
                        + request.getHeader("Referer"));
                // Log Login Failure Event
                LoginEvent.failed(site,"No next parameter specified for login, cannot redirect anywhere.","website");
                return VIEW_ERROR_PAGE;
            }
        }

        LOGGER.info("Authentication failed for user " + emailId + " from "
                + request.getRemoteAddr());

        model.addAttribute("errors", CONTENT_LOGIN_FAILED);
        // Log Login Failure Event
        LoginEvent.failed(site,"Authentication failed for user "+ emailId + " from "
                + request.getRemoteAddr(),"website");

        return (StringUtils.isNotEmpty(next) && next.contains("discountPayment.do"))?  VIEW_DISCOUNT_LOGIN :  VIEW_LOGIN;
    }


    @RequestMapping(value = "authorizeUser.do", method = RequestMethod.POST)
    public ResponseEntity<?> authorizeUser(@RequestParam("emailId") String emailId,
                                @RequestParam("password") String password,
                                @RequestParam(value = "source", required = false) String source,
                                HttpServletRequest request,
                                HttpServletResponse response) {

        String site = TenantContextHolder.getTenantId();

        HashMap<String, String> authenticationResponse = accountDelegate.authenticate(emailId, password);

        if (authenticationResponse.get("status").equals("OK")) {
            CustomerSignup customerSignup = accountDelegate.retrieveProfile(emailId);

            if (source != null) {
                accountService.setUserIDCookieCrossSite(customerSignup.getId(), response);
            } else {
                accountService.setUserIDCookie(customerSignup.getId(), response);
            }

            request.getSession().setAttribute("customerSignup", customerSignup);
            String eqFlowsCookie = accountService.getCookieValue("eq.i", request);
            if (StringUtils.isNotBlank(eqFlowsCookie)) {
                registrationService.updateEQDataCustomerId(customerSignup, eqFlowsCookie);
                response.addCookie(accountService.setCookieForDeletion("eq.i"));
            }
            LOGGER.info("User " + emailId + " signed in from " + request.getRemoteAddr());

            if (source != null) {
                LOGGER.info("Request from " + source + " | Email-Address: " + emailId + " | Status: Authentication Success");
            }

            // Log Login Success Event
            LoginEvent.success(site,request.getSession().getId(),customerSignup.getId(),customerSignup.getEmailId(),"website");

        } else {
            // Log Login Failure Event
            LoginEvent.failed(site,"Authentication failed for user "+ emailId+ " from " + request.getRemoteAddr(),"website");
            LOGGER.info("Authentication failed for user " + emailId + " from " + request.getRemoteAddr());

            if (source != null) {
                LOGGER.info("Request from " + source + " | Email-Address: " + emailId + " | Status: Authentication Failed");
            }
        }

        return new ResponseEntity<>(authenticationResponse, HttpStatus.OK);
    }

    @RequestMapping(value = "csimpersonate.do", method = {RequestMethod.GET, RequestMethod.POST})
    public String impersonate(@RequestParam("id") Long id,
                              @RequestParam("signature") String signature,
                              @RequestParam(value = "next", required = false) String next,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              HttpSession session) throws Exception {

        String site = TenantContextHolder.getTenantId();

        if (accountService.userIDCookieSet(request)) {
            response.addCookie(accountService.setCookieForDeletion("id.i"));
        }

        CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(id);
        if (signature.equals(cryptoDelegate.urlSafeHMAC(String.valueOf(customerSignup.getId())))) {
            session.setAttribute("customerSignup", customerSignup);
            accountService.setUserIDCookie(customerSignup.getId(), response);
            Cookie csImpersonateCookie = accountService.setCookieForCreation("isCsImpersonateLoggedIn", "true");
            response.addCookie(csImpersonateCookie);
            Cookie lawyerReviewCookie = accountService.setCookieForCreation("lr.i", cryptoDelegate.encrypt(String.valueOf(customerSignup.getId())));
            response.addCookie(lawyerReviewCookie);
            if(next != null) {
                response.sendRedirect(next);
            } else {
                response.sendRedirect(NEXT_ACTION_APPLICATIONCENTER);
            }

            // Log Login Success Event
            LoginEvent.success(site,session.getId(),customerSignup.getId(),customerSignup.getEmailId(),"admin");
            return null;
        }

        LOGGER.error("CS Impersonate authentication failed for user with id: " + id);
        // Log Login Failure Event
        LoginEvent.error(site,"Authentication failed for user with id"+ id,null,"admin");

        return VIEW_ERROR_PAGE;
    }
}
