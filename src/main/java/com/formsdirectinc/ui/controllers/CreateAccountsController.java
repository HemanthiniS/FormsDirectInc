package com.formsdirectinc.ui.controllers;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.environment.Environment;
import com.formsdirectinc.event.RegistrationEvent;
import com.formsdirectinc.security.CryptoDelegate;
import com.formsdirectinc.services.BrowserDataService;
import com.formsdirectinc.services.RegistrationService;
import com.formsdirectinc.services.account.AccountCreationException;
import com.formsdirectinc.services.account.AccountDelegate;
import com.formsdirectinc.tenant.TenantContextHolder;
import com.formsdirectinc.ui.auth.Authenticator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = {"/${registration.controller.action:createaccounts.do}"})
@SessionAttributes("customerSignup")
public class CreateAccountsController extends Authenticator{

    private static Logger LOGGER = Logger.getLogger(CreateAccountsController.class);

    @Autowired
    protected BrowserDataService browserDataService;

    @Autowired
    protected AccountDelegate accountDelegate;

    @Autowired
    protected CryptoDelegate cryptoDelegate;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private Environment environment;

    public final static String REGISTRATION_PAGE = "registration";
    public final static String REGISTRATION_PAGE_VARIATION = "registration-b";

    public final static String SUCESS_PAGE_QUERY_PARAM = "%s&promoCode=%s";

    public final static String INACTIVE_PRODUCT_LANDING_PAGE = "/inactive-product";

    public final static String ERROR_PAGE = "error";
    public final static String ACTION_TO_APPLICATIONCENTER = "/application-center/applicationcenter.do";

    //
    // TODO: Do we really need to set the resource bundle error key in the
    // controller?
    //

    public final static String REGISTRATION_FAILURE_MESSAGE = "error.registration.failure";
    public final static String ACCOUNT_ALREADY_EXISTS_MESSAGE = "error.email.exists";

    @RequestMapping(method = RequestMethod.GET)
    public String showForm(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                           @RequestParam(value = "next", required = false) String next,
                           @RequestParam(value = "promoCode", required = false) String promoCode,
                           @CookieValue(value = "lang", required = false) String languageCookie,
                           @RequestParam(value = "updateProfile", required = false) String updateProfile,
                           @RequestParam(value = "application", required = false) String appName,
                           @RequestParam(value = "applicationId", required = false) String applicationId,
                           @CookieValue(value = "id.i", required = false) String customerIDCookie,
                           Model model,
                           UriComponentsBuilder uriComponentsBuilder) throws IOException {

//        if (languageCookie == null || languageCookie.isEmpty()) {
//            setLanguageCookie("en", response);
//        } else {
//            setLanguageCookie(languageCookie, response);
//        }


        if (StringUtils.isEmpty(next)) {
            return ERROR_PAGE;
        }

        if (promoCode != null) {
            next = String.format(SUCESS_PAGE_QUERY_PARAM, next, promoCode);
        }
        String successURL = uriComponentsBuilder.replacePath(next).build().toUriString();
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(successURL).build().getQueryParams();

        List<String> applicationParam = parameters.get("application");
        String application = null;
        String gcParentApplicationId = null;

        if (applicationParam != null) {
            application = applicationParam.get(0);
        }

        if (application == null && appName != null && applicationId != null) {
            next+="&application="+appName+"&applicationId="+applicationId;
        }
        successURL = uriComponentsBuilder.replacePath(next).build().toUriString();

        List<String> registrationVariationProducts = environment.getPropertiesAsListOrDefault("registration.variation.products", new ArrayList<>());
        boolean variationRequired = registrationVariationProducts.contains(application);

        if (signedIn(request, response, session)) {
            Long userId = Long.parseLong(cryptoDelegate.decrypt(customerIDCookie));
            CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(userId);
            if(!customerSignup.getProfileComplete()) {
                return variationRequired ?  REGISTRATION_PAGE_VARIATION :  REGISTRATION_PAGE;
            }
            if (customerSignup.getAuthId() != null && customerSignup.getProfileComplete() && customerSignup.getEmailId() == null) {
                LOGGER.debug("Customer already signed-in, user emailId is incomplete redirecting to update Email");
                return variationRequired ?  REGISTRATION_PAGE_VARIATION :  REGISTRATION_PAGE;
            } else {
                LOGGER.debug("Customer already signed-in, redirecting to " + next);
                return "redirect:" + successURL;
            }
        }

        // Redirect user to product unavailable page when the product is inactive
        if (StringUtils.isNotBlank(application) && !registrationService.isActiveProduct(application)) {
            LOGGER.debug("Inactive Application, redirecting Customer to home page" + application);
            response.sendRedirect(INACTIVE_PRODUCT_LANDING_PAGE);
        }

        model.addAttribute("next", next);
        model.addAttribute("customerSignup", new CustomerSignup());
        model.addAttribute("paywallAfter", environment.getProperty(application + ".paywall", "EQ"));
        model.addAttribute("lawyerConsultation", environment.getProperty("lnt.lawyerConsultation", "false"));

        return variationRequired ? REGISTRATION_PAGE_VARIATION : REGISTRATION_PAGE;
    }

    @RequestMapping(method = RequestMethod.POST)
    public String register(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                           @RequestParam("next") String next,
                           @RequestParam(value = "application", required = false) String application,
                           @CookieValue(value = "referer", required = false) String refererCookie,
                           @CookieValue(value = "lang", defaultValue = "en") String languageCookie,
                           @RequestHeader(name = "Accept-Language", required = false) String acceptLanguageHeader,
                           @RequestHeader(name = "User-Agent", required = false) String userAgentHeader,
                           @RequestHeader(name = "geoip_city", required = false) String geoIpCityHeader,
                           @RequestHeader(name = "geoip_region_name", required = false) String geoIpRegionHeader,
                           @RequestHeader(name = "geoip_country_name", required = false) String geoIpCountryHeader,
                           @RequestParam(value = "congrats", required = false) Boolean congrats,
                           @ModelAttribute("customerSignup") CustomerSignup customerSignup, Model model,
                           UriComponentsBuilder uriComponentsBuilder) throws Exception {

        String site = TenantContextHolder.getTenantId();

        String customerIPAddress = request.getRemoteAddr();
        String successURL = uriComponentsBuilder.replacePath(next).build().toUriString();
        List<String> registrationVariationProducts = environment.getPropertiesAsListOrDefault("registration.variation.products", new ArrayList<>());
        boolean variationRequired = registrationVariationProducts.contains(application);

        if (signedIn(request, response, session)) {
            if (StringUtils.isNotEmpty(next)) {
                return "redirect:" + successURL;
            } else {
                LOGGER.error("No next parameter specified for registration, cannot redirect anywhere. Referer: "
                        + request.getHeader("Referer"));
                // Log Registration Failure Event - No next parameter specified
                RegistrationEvent.failed(site,"No next parameter specified for registration, cannot redirect anywhere. Referer: "
                        + request.getHeader("Referer"));
                return ERROR_PAGE;
            }
        }

        LOGGER.debug("emailId" + customerSignup.getEmailId());

        //
        // Does user already exist?
        //

        CustomerSignup existingCustomer = accountDelegate.retrieveProfile(customerSignup.getEmailId());

        if (existingCustomer != null) {
            //
            // Yes, can't register this user.
            //
            LOGGER.info("User already exists for customer " + customerSignup.getEmailId());
            model.addAttribute("errors", ACCOUNT_ALREADY_EXISTS_MESSAGE);

            // Log Registration Failure Event - User Already Exists
            RegistrationEvent.failed(site,"User already exists for customer " + customerSignup.getEmailId());

            return variationRequired ?  REGISTRATION_PAGE_VARIATION :  REGISTRATION_PAGE;
        }

        //
        // Store signup address, language, interested product
        //

        customerSignup.setSignupIPAddress(customerIPAddress);
        customerSignup.setLanguage(languageCookie);

        if (application != null) {
            customerSignup.setInterestedProduct(application);
        } else if (StringUtils.isNotEmpty(request.getContextPath())) {
            customerSignup.setInterestedProduct(request.getContextPath().substring(1));
        }

        LOGGER.info("Customer " + customerSignup.getEmailId() + " signing-up for "
                            + customerSignup.getInterestedProduct() + " from " + customerIPAddress);

        //
        // Check for countryCode & countryName from country value and save in db
        //
        // TODO: This needs to be moved to accountDelegate's createAccount()
        //
        accountDelegate.formatTelephone(customerSignup);

        try {
            accountDelegate.createAccount(customerSignup);
            LOGGER.info("User " + customerSignup.getEmailId() + " registered for "
                                + customerSignup.getInterestedProduct() + " from IP: " + request.getRemoteAddr());
            // Log Registration Success Event
            RegistrationEvent.success(site,session.getId(),customerSignup.getId(),customerSignup.getEmailId());
        } catch (AccountCreationException ae) {
            LOGGER.error("Error creating account for customer: " + customerSignup.getEmailId() + " for product: "
                                 + customerSignup.getInterestedProduct() + " from IP: " + request.getRemoteAddr());
            model.addAttribute("errors", REGISTRATION_FAILURE_MESSAGE);
            // Log Registration Error Event - AccountCreationException
            RegistrationEvent.error(site,"Account Creation Exception",ae);

            return ERROR_PAGE;
        }

        //
        // Set id.i cookie
        //
        setUserIDCookie(customerSignup.getId(), response);

        //
        // save browser information
        //
        browserDataService.storeBrowserData(customerIPAddress, refererCookie, acceptLanguageHeader, userAgentHeader,
                                            geoIpCityHeader, geoIpRegionHeader, geoIpCountryHeader, customerSignup);

        LOGGER.info("User " + customerSignup.getEmailId() + " registered for " + customerSignup.getInterestedProduct()
                            + " from IP: " + request.getRemoteAddr());

        if (StringUtils.isEmpty(next)) {
            LOGGER.debug("Either invalid next parameter or the next parameter is empty.");
            // Log Registration Failure Event - Invalid next parameter
            RegistrationEvent.failed(site,"Either invalid next parameter or the next parameter is empty.");
            return ERROR_PAGE;
        }
        return "redirect:" + successURL;
    }

}
