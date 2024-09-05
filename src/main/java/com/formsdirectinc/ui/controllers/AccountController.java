package com.formsdirectinc.ui.controllers;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.helpers.mail.Mailer;
import com.formsdirectinc.messageSource.KrailMessageSource;
import com.formsdirectinc.services.AccountService;
import com.formsdirectinc.services.account.AccountDelegate;
import com.formsdirectinc.services.account.ConsentStatus;
import com.formsdirectinc.services.account.ConsentType;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/")
public class AccountController {

    private static final String VIEW_UPDATE_ACCOUNT = "update-account";
    private static final String VIEW_ACCOUNT_PROFILE = "account-profile";
    private static final String VIEW_FORGOT_PASSWORD = "password-reminder";
    private static final String VIEW_RESET_PASSWORD = "reset-password";
    private static final String VIEW_FORGOT_PASSWORD_CONTROLLER = "redirect:/forgotpassword.do";
    private static final String VIEW_PASSWORD_RESET_INSTRUCTIONS_CONTROLLER = "redirect:/passwordresetinstructions.do";
    private static final String VIEW_PASSWORD_RESET_INSTRUCTIONS = "password-reset-instructions";
    private static final String VIEW_RESET_PASSWORD_SUCCESS_CONTROLLER = "redirect:/resetpasswordsuccess.do";
    private static final String VIEW_RESET_PASSWORD_SUCCESS = "reset-password-success";
    private static final String VIEW_UNSUBSCRIBE = "unsubscribe";
    private static final String VIEW_UNSUBSCRIBE_SUCCESS = "unsubscribed";
    private static final String VIEW_LINK_EXPIRED = "link-expired";
    private static final String VIEW_UNSUBSCRIBE_CONTROLLER = "redirect:unsubscribe.do";
    private static final String REDIRECT_ACTION_LOGIN = "redirect:/login.do";
    private static final String NEXT_ACTION_EDIT_ACCOUNT = "/registration/editaccount.do";
    private static final String NEXT_ACTION_MANAGE_ACCOUNT = "/registration/manageaccount.do";
    private static final String NEXT_ACTION_APP_CENTER = "/application-center/applicationcenter.do";
    private static final String CONTENT_KEY_EMAIL_NOT_EXIST = "error.email.notvalid";
    private static final String CONTENT_KEY_MAIL_SUBJECT = "mail.content.ForgotPasswordConfirmation.subject";
    private static final String CONTENT_KEY_MAIL_BODY = "mail.content.ForgotPasswordConfirmation.message";
    private static final String VIEW_UPDATE_PROFILE = "update-profile";

    private static Logger LOGGER = Logger.getLogger(AccountController.class);

    @Value("${passwordResetLinkExpiryTimeInSecs:3600}")
    private int passwordResetLinkExpiryTimeInSecs;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountDelegate accountDelegate;
    @Autowired
    private KrailMessageSource messageSource;

    @Autowired
    private Mailer mailer;

    @RequestMapping(value = "/${editaccount.controller.action:editaccount.do}", method = RequestMethod.GET)
    public String showEditAccountView(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Model model) {
        if (!accountService.signedIn(request, response, request.getSession())) {
            model.addAttribute("next", NEXT_ACTION_EDIT_ACCOUNT);
            return REDIRECT_ACTION_LOGIN;
        }
        return VIEW_UPDATE_ACCOUNT;
    }

    @RequestMapping(value = "/${manageaccount.controller.action:manageaccount.do}", method = RequestMethod.GET)
    public String showManageAccountView(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Model model) {
        if (!accountService.signedIn(request, response, request.getSession())) {
            model.addAttribute("next", NEXT_ACTION_MANAGE_ACCOUNT);
            return REDIRECT_ACTION_LOGIN;
        }
        return VIEW_ACCOUNT_PROFILE;
    }

    @RequestMapping(value = "/${editaccount.controller.action:editaccount.do}", method = RequestMethod.POST)
    public String updateAccountDetail(@RequestParam(value = "emailId") String email,
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      Model model) {

        CustomerSignup updatedCustomerSignup = accountDelegate.retrieveProfile(email);

        if (request.getParameter("firstName") != null) {
            updatedCustomerSignup.setFirstName(request.getParameter("firstName"));
        }
        if (request.getParameter("middleName") != null) {
            updatedCustomerSignup.setMiddleName(request.getParameter("middleName"));
        }
        if (request.getParameter("lastName") != null) {
            updatedCustomerSignup.setLastName(request.getParameter("lastName"));
        }
        if (StringUtils.isNotBlank(request.getParameter("newPassword"))) {
            updatedCustomerSignup.setPassword(request.getParameter("newPassword"));
        }
        if (request.getParameter("telephoneCountryCode") != null && !((String) request.getParameter("telephoneCountryCode")).equals("")) {
            updatedCustomerSignup.setTelephoneCountryCode(request
                    .getParameter("telephoneCountryCode"));
        } else {
            updatedCustomerSignup.setTelephoneCountryCode(null);
        }
        if (request.getParameter("telephone") != null && !((String) request.getParameter("telephone")).equals("")) {
            updatedCustomerSignup.setTelephone(request
                    .getParameter("telephone"));
        } else {
            updatedCustomerSignup.setTelephone(null);
        }

        if (request.getParameter("telephoneCountryCode") != null && !((String) request.getParameter("telephoneCountryCode")).equals("") && ((String) request.getParameter("telephoneCountryCode")).equals("1")) {
            updatedCustomerSignup.setTelephoneCountryName(request.getParameter("telephoneCountry"));
        } else if (request.getParameter("telephoneCountryCode") != null && !((String) request.getParameter("telephoneCountryCode")).equals("") && !((String) request.getParameter("telephoneCountryCode")).equals("1")) {
            updatedCustomerSignup.setTelephoneCountryName(request.getParameter("telephoneNonUSCountry"));
        } else {
            updatedCustomerSignup.setTelephoneCountryName(null);
        }

        boolean result = accountDelegate.updateProfile(updatedCustomerSignup);
        model.addAttribute("csUpdate", String.valueOf(result));
        return VIEW_ACCOUNT_PROFILE;
    }

    @RequestMapping(value = "/${forgotpassword.controller.action:forgotpassword.do}", method = RequestMethod.GET)
    public String showForgotPasswordView() {
        return VIEW_FORGOT_PASSWORD;
    }

    @RequestMapping(value = "/${forgotpassword.controller.action:forgotpassword.do}", method = RequestMethod.POST)
    public String sendPasswordResetLink(@RequestParam("emailId") String emailId,
                                        HttpServletRequest request) throws Exception {


        if (StringUtils.isBlank(emailId)) {
            LOGGER.info("Email field is empty");
            return VIEW_FORGOT_PASSWORD_CONTROLLER;
        }

        CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(emailId);

        if (customerSignup == null) {
            LOGGER.debug("Forgot Password Flow :: EmailId " + emailId + " is not registered");
            return VIEW_PASSWORD_RESET_INSTRUCTIONS_CONTROLLER;
        }

        try {
            LOGGER.debug("Forgot Password Flow :: Initiated for email " + emailId);
            String language = accountService.getLanguageFromCookie(request);
            String userId = Long.toString(customerSignup.getId());
            String passwordResetURL = accountService.generatePasswordResetURL(emailId, userId, passwordResetLinkExpiryTimeInSecs, language);
            Locale locale = new Locale(language);

            // Get key from resourceBundle for the sender, subject and message body.
            String senderMail = messageSource.getMessage("sender", null, locale);
            String subject = messageSource.getMessage(CONTENT_KEY_MAIL_SUBJECT, null, locale);
            String messageBody = messageSource.getMessage(CONTENT_KEY_MAIL_BODY, null, locale);

            // Create MailArgument Object Array.
            Object[] arguments = new Object[4];

            // Set both the senderMail and emailId
            String userName = "user";
            if (customerSignup.getProfileComplete()) {
                userName = StringUtils.capitalize(String.format("%s %s", customerSignup.getFirstName(), customerSignup.getLastName()));
            }
            arguments[0] = senderMail;
            arguments[1] = userName;
            arguments[2] = emailId;
            arguments[3] = passwordResetURL;

            subject = java.text.MessageFormat.format(subject, arguments);
            messageBody = java.text.MessageFormat.format(messageBody, arguments);


            mailer.sendMailHtml(senderMail, emailId, subject, messageBody);
        } catch (Exception e) {
            LOGGER.error("Forgot Password Flow :: Error sending email due to " + e);
            throw new RuntimeException(e);
        }
        LOGGER.debug("Forgot Password Flow :: Displaying password reset instructions page for email " + emailId);
        return VIEW_PASSWORD_RESET_INSTRUCTIONS_CONTROLLER;

    }

    @RequestMapping(value = "/${passwordresetinstructions.controller.action:passwordresetinstructions.do}", method = RequestMethod.GET)
    public String showPasswordResetInfoView() {
        return VIEW_PASSWORD_RESET_INSTRUCTIONS;
    }

    @RequestMapping(value = "/${resetpassword.controller.action:resetpassword.do}", method = RequestMethod.GET)
    public String showResetPasswordView(@RequestParam("token") String token,
                                        Model model) {

        String[] paramValues = accountService.decrypt(token).split("\\|");
        String signature = paramValues[0];
        String userId = paramValues[1];
        String emailAddress = paramValues[2];
        String linkExpiryTimeinMilliSecs = paramValues[3];

        long currentTimeInMilliSecs = accountService.currentTimeInMilliSecs();
        String generatedSignature = accountService.generateSignature(userId, linkExpiryTimeinMilliSecs);
        if (generatedSignature.equals(signature)) {
            if (currentTimeInMilliSecs > Long.parseLong(linkExpiryTimeinMilliSecs)) {
                LOGGER.debug("Forgot Password Flow :: Displaying link expired page because password reset link is expired for email " + emailAddress);
                return  VIEW_LINK_EXPIRED;
            }
        } else {
            LOGGER.debug("Forgot Password Flow :: Displaying link expired page because signature is invalid for email " + emailAddress);
            return  VIEW_LINK_EXPIRED;
        }

        model.addAttribute("emailAddress", accountService.encrypt(emailAddress));

        LOGGER.debug("Forgot Password Flow :: Displaying reset-password page for email " + emailAddress);
        return VIEW_RESET_PASSWORD;
    }

    @RequestMapping(value = "/${resetpassword.controller.action:resetpassword.do}", method = RequestMethod.POST)
    public String updatePassword(HttpServletRequest request) {

        String emailId = accountService.decrypt(request.getParameter("emailAddress"));
        CustomerSignup customerSignup = accountDelegate.retrieveProfile(emailId);
        customerSignup.setPassword(request.getParameter("newPassword"));
        accountDelegate.updateProfile(customerSignup);

        LOGGER.debug("Forgot Password Flow :: New password updated for email " + customerSignup.getEmailId());
        return VIEW_RESET_PASSWORD_SUCCESS_CONTROLLER;
    }

    @RequestMapping(value = "/${resetpasswordsuccess.controller.action:resetpasswordsuccess.do}", method = RequestMethod.GET)
    public String showResetPassWordSuccessView() {
        return VIEW_RESET_PASSWORD_SUCCESS;
    }

    @RequestMapping(value = "/${unsubscribe.controller.action:unsubscribe.do}", method = RequestMethod.GET)
    public String showUnsubcribeView() {
        return VIEW_UNSUBSCRIBE;
    }

    @RequestMapping(value = "/${unsubscribe.controller.action:unsubscribe.do}", method = RequestMethod.POST)
    public String updateOptOut(@RequestParam("emailId") String emailId,
                               RedirectAttributes redirectAttributes, Model model) {
        CustomerSignup customerSignup = accountDelegate.retrieveProfile(emailId);
        if (customerSignup != null) {
            customerSignup.setUniversalOptOut(true);
            Map<ConsentType , ConsentStatus> consents = new HashMap<>();
            consents.put(ConsentType.EMAIL, ConsentStatus.OPT_OUT);
            accountDelegate.createOrUpdateCustomerConsent(customerSignup, consents, "unsubscribe.do");
            accountDelegate.updateProfile(customerSignup);
            model.addAttribute("emailID", customerSignup.getEmailId());
            return  VIEW_UNSUBSCRIBE_SUCCESS;

        } else {
            redirectAttributes.addFlashAttribute("error", CONTENT_KEY_EMAIL_NOT_EXIST);
            return VIEW_UNSUBSCRIBE_CONTROLLER;
        }

    }

    @RequestMapping(value = "/${updateprofile.controller.action:updateprofile.do}", method = RequestMethod.GET)
    public String updateProfileView(HttpServletRequest request, HttpServletResponse response, Model model) {

        if (!accountService.signedIn(request, response, request.getSession())) {
            model.addAttribute("next", NEXT_ACTION_APP_CENTER);
            return REDIRECT_ACTION_LOGIN;
        }
        return VIEW_UPDATE_PROFILE;
    }

    @RequestMapping(value = "/${updateprofile.controller.action:updateprofile.do}", method = RequestMethod.POST)
    public String updateProfile(HttpServletRequest request, HttpServletResponse response, Model model) {

        if (!accountService.signedIn(request, response, request.getSession())) {
            model.addAttribute("next", NEXT_ACTION_APP_CENTER);
            return REDIRECT_ACTION_LOGIN;
        }

        CustomerSignup customerSignup = accountService.retrieveCustomerSignupFromCookie(request);
        if (request.getParameter("firstName") != null) {
            customerSignup.setFirstName(request.getParameter("firstName"));
        }
        if (request.getParameter("middleName") != null) {
            customerSignup.setMiddleName(request.getParameter("middleName"));
        }
        if (request.getParameter("lastName") != null) {
            customerSignup.setLastName(request.getParameter("lastName"));
        }
        if (StringUtils.isNotBlank(request.getParameter("password"))) {
            customerSignup.setPassword(request.getParameter("password"));
        }
        if (request.getParameter("telephoneCountryCode") != null && !((String) request.getParameter("telephoneCountryCode")).equals("")) {
            customerSignup.setTelephoneCountryCode(request
                    .getParameter("telephoneCountryCode"));
        } else {
            customerSignup.setTelephoneCountryCode(null);
        }
        if (request.getParameter("telephoneCountryName") != null && !((String) request.getParameter("telephoneCountryName")).equals("")) {
            customerSignup.setTelephoneCountryName(request
                    .getParameter("telephoneCountryName"));
        } else {
            customerSignup.setTelephoneCountryName(null);
        }
        if (request.getParameter("telephone") != null && !((String) request.getParameter("telephone")).equals("")) {
            customerSignup.setTelephone(request
                    .getParameter("telephone"));
        } else {
            customerSignup.setTelephone(null);
        }

        if (request.getParameter("smsAgree") != null && request.getParameter("smsAgree").equals("true")) {
            HashMap<ConsentType, ConsentStatus> consents = new HashMap<>();
            consents.put(ConsentType.SMS, ConsentStatus.OPT_IN);
            accountDelegate.createOrUpdateCustomerConsent(customerSignup, consents, "Signup");
        }

        customerSignup.setProfileComplete(true);
        customerSignup.setIsGuestUser(false);
        accountDelegate.updateProfile(customerSignup);
        model.addAttribute("next", NEXT_ACTION_APP_CENTER);
        return REDIRECT_ACTION_LOGIN;
    }
}
