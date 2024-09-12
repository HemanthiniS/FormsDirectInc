package com.formsdirectinc.services.rest;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.event.RegistrationEvent;
import com.formsdirectinc.services.*;
import com.formsdirectinc.services.account.AccountCreationException;
import com.formsdirectinc.services.account.AccountDelegate;
import com.formsdirectinc.services.account.ConsentStatus;
import com.formsdirectinc.services.account.ConsentType;
import com.formsdirectinc.tenant.TenantContextHolder;
import com.formsdirectinc.ui.annotations.AuthenticationRequired;
import com.formsdirectinc.ui.auth.Authenticator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import com.formsdirectinc.environment.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;


@RestController
@RequestMapping("/api/v1")
public class UserAccountService {
    private static Logger LOGGER = Logger.getLogger(UserAccountService.class);
    public final static String ACCOUNT_ALREADY_EXISTS = "Account Already exists";
    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

    @Autowired
    protected BrowserDataService browserDataService;

    @Autowired
    protected Authenticator authenticator;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private CustomerService customerService;

    @Value("${minimumTLSVersionSupported:1.2}")
    private Float minimumTLSVersionSupported;

    @Autowired
    private Environment environment;

    @Autowired
    private AccountDelegate accountDelegate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AccountService accountService;

    @RequestMapping(method = RequestMethod.POST, value = "/users", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map> register(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                        @RequestParam("next") String next,
                                        @RequestParam(value = "application", required = false) String application,
                                        @RequestParam(value = "interestedProduct", required = false) String interestedProduct,
                                        @RequestParam(value = "emailConsent", required = false) String emailConsent,
                                        @RequestParam(value = "smsConsent", required = false) String smsConsent,
                                        @CookieValue(value = "referer", required = false) String refererCookie,
                                        @CookieValue(value = "lang", defaultValue = "en") String languageCookie,
                                        @RequestHeader(name = "Accept-Language", required = false) String acceptLanguageHeader,
                                        @ModelAttribute("customerSignup") CustomerSignup customerSignup,
                                        @RequestHeader(name = "User-Agent", required = false) String userAgentHeader,
                                        @RequestHeader(name = "geoip_city", required = false) String geoIpCityHeader,
                                        @RequestHeader(name = "geoip_region_name", required = false) String geoIpRegionHeader,
                                        @RequestHeader(name = "geoip_country_name", required = false) String geoIpCountryHeader,
                                        @ModelAttribute("headers") HttpHeaders headers, Model model) throws Exception {


        String site = TenantContextHolder.getTenantId();

        // Log Registration Attempt
        RegistrationEvent.attempted(site, application, customerSignup.getId());

        // print emailId for information purpose

        LOGGER.debug("For customer" + customerSignup.getEmailId());
        String customerIPAddress = request.getRemoteAddr();

        // Check if the emailID and the password provided are valid
        if (!registrationService.isValidUserToRegister(customerSignup.getEmailId(), customerSignup.getPassword())) {
            LOGGER.error("Either the emailId : " + customerSignup.getEmailId()
                    + " or password : " + customerSignup.getPassword() + " is invalid");
            return new ResponseEntity<Map>(HttpStatus.BAD_REQUEST);
        }

        /**
         *
         * throwing illegal argument exception if email id is null
         *
         */

        if (customerSignup.getEmailId() == null) {
            throw new IllegalArgumentException();
        }


        //
        // Does user already exist?
        //

        CustomerSignup existingCustomer = registrationService.retrieveProfile(customerSignup.getEmailId());

        if (existingCustomer != null) {

            //
            // Yes, can't register this user.
            //

            LOGGER.info("User already exists for customer " + customerSignup.getEmailId());

            // Log Registration Failure Event - User Already Exists
            RegistrationEvent.failed(site, "User already exists for customer " + customerSignup.getEmailId());


            //
            // Return 409
            //

            return new ResponseEntity<Map>(HttpStatus.CONFLICT);
        }

        //
        // Store signup address, language, interested product
        //

        customerSignup.setSignupIPAddress(customerIPAddress);
        customerSignup.setLanguage(languageCookie);
        customerSignup.setProfileComplete(false);
        customerSignup.setUniversalOptOut(false);
        if (customerSignup.getIsGuestUser() == null) {
            customerSignup.setIsGuestUser(false);
        }


        if (interestedProduct == null) {
            if (application != null) {
                customerSignup.setInterestedProduct(application);
            } else if (StringUtils.isNotEmpty(request.getContextPath())) {
                customerSignup.setInterestedProduct(request.getContextPath().substring(1));
            }
        } else {
            customerSignup.setInterestedProduct(interestedProduct);
        }

        LOGGER.info("Customer " + customerSignup.getEmailId() + " signing-up for "
                + customerSignup.getInterestedProduct() + " from " + customerIPAddress);

        try {

            registrationService.createAccount(customerSignup);
            LOGGER.info("User " + customerSignup.getEmailId() + " registered for "
                    + customerSignup.getInterestedProduct() + " from IP: " + request.getRemoteAddr());

            // Log Registration Success Event
            RegistrationEvent.success(site, session.getId(), customerSignup.getId(), customerSignup.getEmailId());


        } catch (AccountCreationException ae) {
            LOGGER.error("Error creating account for customer: " + customerSignup.getEmailId() + " for product: "
                    + customerSignup.getInterestedProduct() + " from IP: " + request.getRemoteAddr());

            // Log Registration error Event - AccountCreationException
            RegistrationEvent.error(site, "AccountCreationException", ae);

            return new ResponseEntity<Map>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            if (StringUtils.isNotBlank(emailConsent) && StringUtils.isNotBlank(smsConsent)) {
                Map<ConsentType, ConsentStatus> consents = new HashMap<>();
                consents.put(ConsentType.EMAIL, ConsentStatus.valueOf(emailConsent));
                consents.put(ConsentType.SMS, ConsentStatus.valueOf(smsConsent));
                registrationService.createCustomerConsent(customerSignup, consents, "Signup");
            }
        } catch (Exception e) {
            LOGGER.error("Error creating customer consent: " + customerSignup.getEmailId());
            RegistrationEvent.error(site, "ConsentCreationException", e);
        }

        String encryptedUserId = registrationService.encryptUserIdCookieValue(Long.toString(customerSignup.getId()));

        String successUrl = String.format("/api/v1/users/%s", customerSignup.getId());

        //
        // to be stored as location header
        //

        String locationUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path(successUrl).build()
                .toUriString();
        headers.add("Location", locationUrl);

        Map<String, Object> responseJson = new HashMap<String, Object>();
        responseJson.put("login_token", encryptedUserId);
        responseJson.put("uid", customerSignup.getId());

        //
        // Store browser information
        //

        browserDataService.storeBrowserData(customerIPAddress, refererCookie, acceptLanguageHeader, userAgentHeader,
                geoIpCityHeader, geoIpRegionHeader, geoIpCountryHeader, customerSignup);

        //
        // Return 201 with a next action set to loacation header and a json
        // containing userId
        //

        return new ResponseEntity<Map>(responseJson, headers, HttpStatus.CREATED);

    }

    //
    // update Signup details
    //

    @AuthenticationRequired(action = "forbid")
    @RequestMapping(method = RequestMethod.POST, value = "/users/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> update(@PathVariable("userId") String userId,
                                         @RequestBody CustomerSignup customerSignup,
                                         @CookieValue(value = "id.i", required = false) String customerIDCookie,
                                         @CookieValue(value = "hubspotutk", required = false) String hubspotToken,
                                         @CookieValue(value = "eq.i", required = false) String eqFlowsCookie,
                                         @RequestParam(value = "next", required = false) String next,
                                         @RequestParam(value = "interestedProduct", required = false) String interestedProduct,
                                         @RequestParam(value = "emailConsent", required = false) String emailConsent,
                                         @RequestParam(value = "smsConsent", required = false) String smsConsent,
                                         @RequestParam(value = "updateConsentStatus", required = false, defaultValue = "false") Boolean updateConsentStatus,
                                         HttpServletRequest request,
                                         HttpServletResponse response,
                                         UriComponentsBuilder uriComponentsBuilder) throws Exception {

        String site = TenantContextHolder.getTenantId();

        if (!userId.equals(registrationService.decryptUserIdCookieValue(customerIDCookie))) {
            LOGGER.error("UserID dosen't match with the encrypted id.i cookie " + userId);
            return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
        }

        if (customerSignup == null) {
            LOGGER.error("Unable to load CustomerSignup " + customerSignup);
            return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
        }

        String nextURL = uriComponentsBuilder.replacePath(next).build().toUriString();
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(nextURL).build().getQueryParams();
        List<String> applicationParam = parameters.get("application");
        Map<String, String> consumerDetail = new HashMap<String, String>();
        String application = new String();
        if (applicationParam != null) {
            application = applicationParam.get(0);
        }

        String telephone = (customerSignup.getSigninPhone().getAreaCode() != null) ? customerSignup.getSigninPhone().getAreaCode() +
                customerSignup.getSigninPhone().getPhone1() + customerSignup.getSigninPhone().getPhone2() : customerSignup.getSigninPhone().getPhone3();

        // Check if the user name and the phone number provided are valid
        if (!registrationService.isValidUserToUpdateProfile(customerSignup.getFirstName(), customerSignup.getLastName(), telephone)) {
            LOGGER.error("Either the UserName : (FirstName : " + customerSignup.getFirstName()
                    + "), (LastName : " + customerSignup.getLastName() + ") or telephone number : "
                    + telephone + " is invalid");
            return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
        }

        if (customerSignup.getInterestedProduct() == null) {
            if (interestedProduct == null) {
                if (StringUtils.isNotBlank(application)) {
                    customerSignup.setInterestedProduct(application);
                }
            } else {
                customerSignup.setInterestedProduct(interestedProduct);
            }
        }


        registrationService.updateProfile(Long.parseLong(userId), customerSignup);

        CustomerSignup updatedCustomerSignup = registrationService.retrieveCustomerSignup(
                Long.parseLong(registrationService.decryptUserIdCookieValue(customerIDCookie)));

        if (updateConsentStatus) {
            try {
                Map<ConsentType, ConsentStatus> consents = new HashMap<>();
                if (StringUtils.isNotBlank(emailConsent)) {
                    consents.put(ConsentType.EMAIL, ConsentStatus.valueOf(emailConsent));
                }
                if (StringUtils.isNotBlank(smsConsent)) {
                    consents.put(ConsentType.SMS, ConsentStatus.valueOf(smsConsent));
                }
                if (!consents.isEmpty()) {
                    registrationService.createCustomerConsent(updatedCustomerSignup, consents, "Signup");
                }
            } catch (Exception e) {
                LOGGER.error("Error creating customer consent: " + customerSignup.getEmailId());
                RegistrationEvent.error(site, "ConsentCreationException", e);
            }
        }

        LOGGER.info(String.format("Before hubspot contact %s", updatedCustomerSignup.getId()));
        if (environment.getBooleanProperty("hubspot.enabled") && customerService.isEmailIdExcluded(updatedCustomerSignup.getEmailId(), updatedCustomerSignup.getSignupIPAddress())) {
            LOGGER.info(String.format("Attepmting to post contact to  hubspot %s", updatedCustomerSignup.getId()));
            if (hubspotToken != null) {
                registrationService.postContactToHubspot(updatedCustomerSignup, hubspotToken, eqFlowsCookie, emailConsent, smsConsent);
            } else {
                LOGGER.info(String.format("Hubspot token not available for user %s", updatedCustomerSignup.getId()));
            }
        } else {
            LOGGER.info(String.format("Either Hubspot is not enabled or user is a test user %s", updatedCustomerSignup.getId()));
        }

        LOGGER.info(String.format("Before Attempting to post Contcat to GetResponse", updatedCustomerSignup.getEmailId()));
        if (environment.getBooleanProperty("getresponse.enabled") && customerService.isEmailIdExcluded(updatedCustomerSignup.getEmailId(), updatedCustomerSignup.getSignupIPAddress())) {
            LOGGER.info(String.format("Attempting to post Contcat to GetResponse", updatedCustomerSignup.getEmailId()));
            consumerDetail.put("customer_id", String.valueOf(updatedCustomerSignup.getId()));
            consumerDetail.put("interested_product", updatedCustomerSignup.getInterestedProduct());
            consumerDetail.put("language", updatedCustomerSignup.getLanguage());
            consumerDetail.put("ispaid", "false");
            consumerDetail.put("signup_date", formatter.format(updatedCustomerSignup.getSignupDate()));
            registrationService.postContactToGetResponse(updatedCustomerSignup, consumerDetail);
        }

        if (StringUtils.isNotBlank(eqFlowsCookie)) {
            registrationService.updateEQDataCustomerId(updatedCustomerSignup, eqFlowsCookie);
            response.addCookie(accountService.setCookieForDeletion("eq.i"));
        }

        if (customerService.isEmailIdExcluded(updatedCustomerSignup.getEmailId(), updatedCustomerSignup.getSignupIPAddress())) {
            emailService.sendWelcomeEmail(updatedCustomerSignup, application, accountService.getBaseUrl(request));
        }

        // Log for debugging purpose

        LOGGER.debug("Updated Successfully");

        return new ResponseEntity<Object>(new HashMap<String, Object>(), HttpStatus.OK);

    }

    @CrossOrigin(origins = {"https://consult.onenationlaw.com", "https://testconsult.onenationlaw.com"})
    @RequestMapping(method = RequestMethod.POST, value = "/users/create")
    public ResponseEntity<?> registerWithEQData(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                                @RequestParam(value = "application", required = false) String application,
                                                @RequestParam(value = "eqId", required = false) String eqId,
                                                @RequestParam(value = "interestedProduct", required = false) String interestedProduct,
                                                @RequestParam(value = "lang", required = false) String language,
                                                @RequestParam(value = "emailConsent", required = false) String emailConsent,
                                                @RequestParam(value = "smsConsent", required = false) String smsConsent,
                                                @RequestHeader(name = "Accept-Language", required = false) String acceptLanguageHeader,
                                                @ModelAttribute("customerSignup") CustomerSignup customerSignup,
                                                @CookieValue(value = "hubspotutk", required = false) String hubspotToken,
                                                @RequestHeader(name = "User-Agent", required = false) String userAgentHeader,
                                                @RequestHeader(name = "geoip_city", required = false) String geoIpCityHeader,
                                                @RequestHeader(name = "geoip_region_name", required = false) String geoIpRegionHeader,
                                                @RequestHeader(name = "geoip_country_name", required = false) String geoIpCountryHeader) throws Exception {

        String site = TenantContextHolder.getTenantId();

        // Log Registration Attempt
        RegistrationEvent.attempted(site, application, customerSignup.getId());
        LOGGER.debug("For customer" + customerSignup.getEmailId());
        String customerIPAddress = request.getRemoteAddr();
        Map<String, String> consumerDetail = new HashMap<String, String>();

        // Check if the emailID and the password provided are valid
        if (!registrationService.isValidUserToRegister(customerSignup.getEmailId(), customerSignup.getPassword())) {
            LOGGER.error("Either the emailId : " + customerSignup.getEmailId()
                    + " or password : " + customerSignup.getPassword() + " is invalid");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (customerSignup.getEmailId() == null) {
            throw new IllegalArgumentException();
        }

        CustomerSignup existingCustomer = registrationService.retrieveProfile(customerSignup.getEmailId());
        if (existingCustomer != null) {
            LOGGER.info("User already exists for customer " + customerSignup.getEmailId());
            RegistrationEvent.failed(site, "User already exists for customer " + customerSignup.getEmailId());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        customerSignup.getSigninPhone().setAreaCode(customerSignup.getSigninPhone().getAreaCode());
        customerSignup.getSigninPhone().setCountry(customerSignup.getSigninPhone().getCountry());
        customerSignup.getSigninPhone().setPhone1(customerSignup.getSigninPhone().getPhone1());
        customerSignup.getSigninPhone().setPhone2(customerSignup.getSigninPhone().getPhone2());
        customerSignup.getSigninPhone().setPhone3(customerSignup.getSigninPhone().getPhone3());

        accountDelegate.formatTelephone(customerSignup);

        customerSignup.setSignupIPAddress(customerIPAddress);
        customerSignup.setLanguage(language);
        customerSignup.setProfileComplete(true);
        customerSignup.setUniversalOptOut(false);

        if (customerSignup.getIsGuestUser() == null) {
            customerSignup.setIsGuestUser(false);
        }

        if (interestedProduct == null) {
            if (application != null) {
                customerSignup.setInterestedProduct(application);
            } else if (StringUtils.isNotEmpty(request.getContextPath())) {
                customerSignup.setInterestedProduct(request.getContextPath().substring(1));
            }
        } else {
            customerSignup.setInterestedProduct(interestedProduct);
        }

        LOGGER.info("Customer " + customerSignup.getEmailId() + " signing-up for "
                + customerSignup.getInterestedProduct() + " from " + customerIPAddress);

        try {
            registrationService.createAccount(customerSignup);
            LOGGER.info("User " + customerSignup.getEmailId() + " registered for "
                    + customerSignup.getInterestedProduct() + " from IP: " + request.getRemoteAddr());
            // Log Registration Success Event
            RegistrationEvent.success(site, session.getId(), customerSignup.getId(), customerSignup.getEmailId());
        } catch (AccountCreationException ae) {
            LOGGER.error("Error creating account for customer: " + customerSignup.getEmailId() + " for product: "
                    + customerSignup.getInterestedProduct() + " from IP: " + request.getRemoteAddr());
            // Log Registration error Event - AccountCreationException
            RegistrationEvent.error(site, "AccountCreationException", ae);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        browserDataService.storeBrowserData(customerIPAddress, null, acceptLanguageHeader, userAgentHeader,
                geoIpCityHeader, geoIpRegionHeader, geoIpCountryHeader, customerSignup);

        try {
            if (StringUtils.isNotBlank(emailConsent) && StringUtils.isNotBlank(smsConsent)) {
                Map<ConsentType, ConsentStatus> consents = new HashMap<>();
                consents.put(ConsentType.EMAIL, ConsentStatus.valueOf(emailConsent));
                consents.put(ConsentType.SMS, ConsentStatus.valueOf(smsConsent));
                registrationService.createCustomerConsent(customerSignup, consents, "Signup");
            }
        } catch (Exception e) {
            LOGGER.error("Error creating customer consent: " + customerSignup.getEmailId());
            RegistrationEvent.error(site, "ConsentCreationException", e);
        }

        if (environment.getBooleanProperty("hubspot.enabled") && customerService.isEmailIdExcluded(customerSignup.getEmailId(), customerSignup.getSignupIPAddress())) {
            if (hubspotToken != null) {
                //registrationService.postContactToHubspot(customerSignup, hubspotToken, eqId, emailConsent, smsConsent);
            } else {
                LOGGER.debug(String.format("Hubspot token not available for user %s", customerSignup.getId()));
            }
        }

        if (environment.getBooleanProperty("getresponse.enabled") && customerService.isEmailIdExcluded(customerSignup.getEmailId(), customerSignup.getSignupIPAddress())) {
            consumerDetail.put("customer_id", String.valueOf(customerSignup.getId()));
            consumerDetail.put("interested_product", customerSignup.getInterestedProduct());
            consumerDetail.put("language", customerSignup.getLanguage());
            consumerDetail.put("ispaid", "false");
            consumerDetail.put("signup_date", formatter.format(customerSignup.getSignupDate()));
            registrationService.postContactToGetResponse(customerSignup, consumerDetail);
        }

        if (StringUtils.isNotBlank(eqId)) {
            registrationService.updateEQDataCustomerId(customerSignup, eqId);
            response.addCookie(accountService.setCookieForDeletion("eq.i"));
        }

        return new ResponseEntity<>(customerSignup.getId(), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/users/current")
    public ResponseEntity<Object> retrieveLocationHeader(@CookieValue(value = "id.i", required = false) String customerIDCookie,
                                                         @ModelAttribute("headers") HttpHeaders headers) throws Exception {

        String site = TenantContextHolder.getTenantId();

        if (customerIDCookie == null) {
            LOGGER.error(String.format("Encrypted Customer Cookie %s is not valid", customerIDCookie));
            return new ResponseEntity<Object>(new HashMap<String, Object>(), HttpStatus.BAD_REQUEST);
        }

        String customerID = registrationService.decryptUserIdCookieValue(customerIDCookie);
        String userLocationUrl = String.format("/api/v1/users/%s", customerID);
        String locationUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path(userLocationUrl).build()
                .toUriString();
        headers.add("Location", locationUrl);

        return new ResponseEntity<>(new HashMap<String, Object>(), headers, HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.GET, value = "/users/visitSource")
    @CrossOrigin(origins = {"https://consult.onenationlaw.com", "https://testconsult.onenationlaw.com"})
    public ResponseEntity<Object> persistUserVisitSourceData(HttpServletRequest request,
                                                             @CookieValue(value = "id.i", required = false) String encryptedUserIDCookie) throws Exception {

        String site = TenantContextHolder.getTenantId();

        if (encryptedUserIDCookie == null) {
            LOGGER.info("Encrypted User ID cookie does not exist for tracking user source");
            return new ResponseEntity<>(HttpStatus.OK);
        }

        Long userId = Long.parseLong(registrationService.decryptUserIdCookieValue(encryptedUserIDCookie));

        Map<String, Cookie> sourceTrackingCookieMap = customerService.retrieveSourceTrackingCookies(request.getCookies());

        if (sourceTrackingCookieMap.isEmpty()) {
            LOGGER.info(String.format("Could not track user visit source data for user ID %s", userId));
            return new ResponseEntity<>(HttpStatus.OK);
        }

        //persist user_visit_source_t with the source tracking cookie data
        for (String trackingCookieName : sourceTrackingCookieMap.keySet()) {
            customerService.persistUserVisitSource(
                    userId,
                    trackingCookieName,
                    sourceTrackingCookieMap.get(trackingCookieName).getValue());
        }

        LOGGER.info(String.format("Source tracking data persisted to user_visit_source_t for customer ID %s", userId));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/tlsVersionSupported", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = {"https://consult.onenationlaw.com", "https://testconsult.onenationlaw.com"})
    public ResponseEntity<Map> tlsVersion(HttpServletRequest request) {

        HashMap<String, Boolean> response = new HashMap<>();
        Boolean versionSupported = true;
        Float version;
        String[] tls;

        if (StringUtils.isNotBlank(request.getHeader("X-SSL-Protocol"))) {
            LOGGER.info("X-SSL-Protocol from Request Header " + request.getHeader("X-SSL-Protocol"));
            tls = request.getHeader("X-SSL-Protocol").split("v");
            if (tls.length == 2) {
                version = Float.parseFloat(tls[1]);

                //checking minimum requirement for browser
                if (version < minimumTLSVersionSupported) {
                    versionSupported = false;
                }
            }
        }

        response.put("tlsSupported", versionSupported);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map> userInfo(HttpServletRequest request, @CookieValue(value = "id.i", required = false) String encryptedUserIDCookie) throws Exception {

        if (encryptedUserIDCookie == null) {
            LOGGER.info("Encrypted User ID cookie does not exist for tracking user source");
            return new ResponseEntity<>(HttpStatus.OK);
        }

        Long userId = Long.parseLong(registrationService.decryptUserIdCookieValue(encryptedUserIDCookie));
        CustomerSignup customerSignup = null;

        if (userId != null) {
            customerSignup = accountDelegate.retrieveCustomerSignup(userId);
        }

        Map<String, Object> response = new HashMap<String, Object>();

        response.put("firstName", customerSignup.getFirstName());
        response.put("lastName", customerSignup.getLastName());
        response.put("isAuthUser", (customerSignup.getAuthId() != null) ? true : false);
        response.put("isProfileComplete", customerSignup.getProfileComplete());
        response.put("emailID", customerSignup.getEmailId());
        response.put("userId", customerSignup.getId());
        response.put("signupLanguage", customerSignup.getLanguage());

        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.GET, value = "/userId")
    public ResponseEntity<Map> getUserId(@RequestParam(value = "email", required = true) String email) {
        CustomerSignup customerSignup = registrationService.retrieveProfile(email);
        String encryptedUserId = registrationService.encryptUserIdCookieValue(Long.toString(customerSignup.getId()));
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("user_id_token", encryptedUserId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map> getCustomerProfile(@PathVariable(value = "userId") Long userId) {

        CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(userId);
        if (customerSignup != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("firstname", customerSignup.getFirstName());
            response.put("lastname", customerSignup.getLastName());
            response.put("email", customerSignup.getEmailId());
            response.put("phone", customerSignup.getTelephone());
            response.put("countryCode", customerSignup.getTelephoneCountryCode());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = {"https://hub.formsdirect.com"})
    public ResponseEntity<String> unsubscribeUser(@RequestParam String source,
                                                  @RequestParam(name = "email", required = false) Optional<String> optionalEmail,
                                                  @RequestParam(name = "phone", required = false) Optional<String> optionalPhone,
                                                  @RequestParam(defaultValue = "false", required = false) Boolean markDeleted,
                                                  @RequestBody Map<ConsentType, ConsentStatus> consents) {
        try {
            List<CustomerSignup> customerSignups = null;
            if (optionalEmail.isPresent()) {
                customerSignups = new ArrayList<CustomerSignup>();
                CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(optionalEmail.get());
                if(customerSignup != null){
                    customerSignups.add(customerSignup);
                }
            } else if (optionalPhone.isPresent()) {
                customerSignups = accountDelegate.retrieveCustomerSignupByPhone(optionalPhone.get());
            }

            if (customerSignups == null || customerSignups.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            for (CustomerSignup customerSignup : customerSignups) {
                registrationService.unsubscribeUserFromFD(customerSignup, markDeleted, consents, source);
            }
            return new ResponseEntity<>("User consent status updated successfully", HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error("Exception while updating the user consent", e);
            return new ResponseEntity<>("Failed to Update user consent status", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
