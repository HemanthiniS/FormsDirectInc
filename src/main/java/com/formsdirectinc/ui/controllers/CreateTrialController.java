package com.formsdirectinc.ui.controllers;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.services.AccountService;
import com.formsdirectinc.services.RegistrationService;
import com.formsdirectinc.services.account.AccountDelegate;
import com.formsdirectinc.services.account.OfferStageEnum;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/createtrial.do")
public class CreateTrialController {
    private static Logger LOGGER = Logger.getLogger(CreateTrialController.class);

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AccountDelegate accountDelegate;

    @Autowired
    private AccountService accountService;

    public final static String APPLICATION_CREATED_CONFIRMATION_PAGE = "/trial?application=%s&applicationID=%s";
    public final static String START_APPLICATION = "/%s/startapplication.do";
    public final static String PAYMENT_PAGE = "/payment/payment.do?application=%s";
    public final static String LOGIN_PAGE = "/registration/logincheck.do?next=/application-center/applicationcenter.do";
    public final static String APPLICATION_CENTER = "/application-center/applicationcenter.do";
    public final static String REDIRECT_TO_CREATEACCOUNTS_CONTROLLER = "/registration/createaccounts.do?next=/registration/createtrial.do?application=%s";

    @RequestMapping(method = RequestMethod.GET)
    public String showView(@RequestParam(value = "application", required = false) String applicationName,
                           @RequestParam(value = "stageName", required = false) String stageName,
                           @RequestParam(value = "emailId", required = false) String emailId,
                           @CookieValue(value = "id.i", required = false) String customerIDCookie,
                           @CookieValue(value = "lang", required = false, defaultValue = "en") String language,
                           UriComponentsBuilder uriComponentsBuilder, Model model,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {

        String paymentPageURL = uriComponentsBuilder.replacePath(String.format(PAYMENT_PAGE, applicationName)).build().toUriString();
        String loginPageURL = uriComponentsBuilder.replacePath(LOGIN_PAGE).build().toUriString();
        String updateProfileURL = uriComponentsBuilder.replacePath(String.format(REDIRECT_TO_CREATEACCOUNTS_CONTROLLER, applicationName)).build().toUriString();
        String applicationCenterURL = uriComponentsBuilder.replacePath(String.format(APPLICATION_CENTER)).build().toUriString();
        Long userId = null;

        if (customerIDCookie == null && !StringUtils.isNotBlank(emailId)) {
            return "redirect:" + loginPageURL;
        }

        if (customerIDCookie != null) {
            userId = Long.parseLong(registrationService.decryptUserIdCookieValue(customerIDCookie));
        }

        if (StringUtils.isNotBlank(emailId)) {
            response.addCookie(accountService.setCookieForDeletion("id.i"));
            try {
                CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(emailId);
                accountService.setUserIDCookie(customerSignup.getId(), response);
                userId = customerSignup.getId();
            } catch (Exception e) {
                LOGGER.error("User email not registered, hence redirecting to signup page", e);
                return "redirect:" + updateProfileURL;
            }
        }

        if (userId == null) {
            return "redirect:" + loginPageURL;
        }

        // Check if the user profile is completed, w.r.t to 2 step registration
        if (!registrationService.isUserProfileComplete(userId)) {
            model.addAttribute("updateProfile", "true");
            LOGGER.info("User Profile is incomplete, redirecting to update profile view");
            return "redirect:" + updateProfileURL;
        }

        // Check if the user has an application available for trial creation
        if (!StringUtils.isNotBlank(applicationName)) {
            LOGGER.info("User has no valid application to create trial application, hence redirecting to application-center");
            return "redirect:" + applicationCenterURL;
        }

        Cookie navIDCookie = WebUtils.getCookie(request, String.format("%s_nav_id", applicationName));

        Long gcParentApplicationId = null;
        if (request.getParameter("gcParentApplicationId") != null) {
            gcParentApplicationId = Long.parseLong(request.getParameter("gcParentApplicationId"));
        }

        if (request.getParameter("gcrFlow") != null && Boolean.parseBoolean(request.getParameter("gcrFlow"))) {
            HttpResponse<JsonNode> gcParentApplicationIdResponse = registrationService.getGCParentApplicationId(userId);
            if (gcParentApplicationIdResponse.getStatus() == 200) {
                gcParentApplicationId = gcParentApplicationIdResponse.getBody().getArray().getLong(0);
            } else {
                return "redirect:" + loginPageURL;
            }
        }

        // Create a trial application if the trial is enabled for the opted product
        if (registrationService.isTrialEnabledForProduct(applicationName)) {
            JSONObject application = registrationService.trialApplicationCreation(applicationName, userId, language, navIDCookie, gcParentApplicationId);
            if (application.has("applicationId") && application.get("applicationId").toString() != null) {
                if(stageName != null && stageName.equals(OfferStageEnum.REGISTRATION.name())){
                    return "redirect:" + uriComponentsBuilder.replacePath(String.format(APPLICATION_CREATED_CONFIRMATION_PAGE, applicationName,application.get("applicationId").toString())).build().toUriString();
                } else {
                    UriComponentsBuilder builder = uriComponentsBuilder.replacePath(String.format(START_APPLICATION, applicationName));
                    builder.query(request.getQueryString().replace("application=","applicationName="));
                    builder.queryParam("interaction", "SQ");
                    builder.queryParam("applicationId", application.get("applicationId").toString());
                    return "redirect:" + builder.build().toUriString();
                }
            } else {
                // TODO: When trial is enabled but trial-application was not created. Need to decide on the right action
                return "redirect:" + loginPageURL;
            }
        } else {
            return "redirect:" + paymentPageURL;
        }
    }
}
