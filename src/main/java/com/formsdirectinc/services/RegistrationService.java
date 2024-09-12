package com.formsdirectinc.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.dao.EQDataTable;
import com.formsdirectinc.dao.EQDataTableDAO;
import com.formsdirectinc.dao.LeadsDAO;
import com.formsdirectinc.ecommerce.ProductDelegate;
import com.formsdirectinc.security.CryptoDelegate;
import com.formsdirectinc.services.account.AccountCreationException;
import com.formsdirectinc.services.account.AccountDelegate;
import com.formsdirectinc.services.account.ConsentStatus;
import com.formsdirectinc.services.account.ConsentType;
import com.formsdirectinc.tenant.TenantContextHolder;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.formsdirectinc.environment.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.servlet.http.Cookie;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class RegistrationService {

  private static String ENDPOINT_URI_TO_RETRIEVE_PRODUCT_BY_NAME = "/ecommerce/api/v1/products/?productName=%s";
  private static String ENDPOINT_URI_TO_RETRIEVE_INDEPENDENTLYSALABLEPRODUCT = "/ecommerce/api/v1/products/independentlySalable?productName=%s";
  private static String TRIAL_ENDPOINT_URI = "/ecommerce/api/v1/products/trial";
  private static String EMAILLANDING_URL_WITH_EMAILID = "%s/ecommerce/emaillanding.do?emailId=%s";
  private static String EMAILLANDING_URL_WITHOUT_EMAILID = "%s/ecommerce/emaillanding.do";
  private static String ENDPOINT_URI_TO_CANCEL_CUSTOMER_ORDERS = "/ecommerce/api/v1/orders/users/%s";
  private static String ENDPOINT_URI_TO_RETRIEVE_CUSTOMER_PARENT_APPLICATION_IDS = "/ecommerce/api/v1/customers/%s/gcParentApplicationId";
  private static final String GETRESPONSE_CREATE_CONTACT = "%s/contacts";
  private static final String GETRESPONSE_CONTACT_URL = "%s/contacts?query[email]=%s";
  private static final String GETRESPONSE_UNSUBSCRIBE_CONTACT = "%s/contacts/%s";
  private static final String HUBSPOT_CONTACT_URL = "%s/contacts/v1/contact/email/%s/profile";
  private static final String HUBSPOT_UNSUBSCRIBE_URL = "%s/email/public/v1/subscriptions/%s";

  @Autowired
  private AccountDelegate accountDelegate;

  @Autowired
  private EQDataTableDAO eqDataTableDAO;

  @Autowired
  private LeadsDAO leadsDAO;

  @Autowired
  private Environment environment;

  @Autowired
  private CryptoDelegate cryptoDelegate;

  @Autowired
  private ProductDelegate productDelegate;

  private static Logger LOGGER = Logger.getLogger(RegistrationService.class);

  public boolean isUserProfileComplete(String customerIDCookie) {

    if (customerIDCookie == null) {
      throw new IllegalArgumentException();
    }

    Long userId = Long.parseLong(cryptoDelegate.decrypt(customerIDCookie));
    CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(userId);

    return customerSignup.getProfileComplete();
  }

  public boolean isUserProfileComplete(Long userId) {

    if (userId == null) {
      throw new IllegalArgumentException();
    }

    CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(userId);

    return customerSignup.getProfileComplete();
  }

  public boolean updateProfile(Long userId, CustomerSignup customerSignup) {

    if(userId == null) {
      throw new IllegalArgumentException();
    }

    CustomerSignup storedCustomerSignup = accountDelegate.retrieveCustomerSignup(userId);

    storedCustomerSignup.setFirstName(customerSignup.getFirstName());
    storedCustomerSignup.setLastName(customerSignup.getLastName());
    storedCustomerSignup.getSigninPhone().setAreaCode(customerSignup.getSigninPhone().getAreaCode());
    storedCustomerSignup.getSigninPhone().setCountry(customerSignup.getSigninPhone().getCountry());
    storedCustomerSignup.getSigninPhone().setPhone1(customerSignup.getSigninPhone().getPhone1());
    storedCustomerSignup.getSigninPhone().setPhone2(customerSignup.getSigninPhone().getPhone2());
    storedCustomerSignup.getSigninPhone().setPhone3(customerSignup.getSigninPhone().getPhone3());
    if(customerSignup.getInterestedProduct() !=null) {
      storedCustomerSignup.setInterestedProduct(customerSignup.getInterestedProduct());
    }
    if(customerSignup.getIsGuestUser() != null){
      storedCustomerSignup.setIsGuestUser(customerSignup.getIsGuestUser());
    }

    if(customerSignup.getEmailId() != null){
      storedCustomerSignup.setEmailId(customerSignup.getEmailId());
    }
    if(customerSignup.getPassword() != null){
      storedCustomerSignup.setPassword(customerSignup.getPassword());
    }
    storedCustomerSignup.setProfileComplete(true);

    return accountDelegate.updateProfile(storedCustomerSignup);
  }

  public CustomerSignup retrieveProfile(String emailId) {

    if(emailId == null) {
      throw new IllegalArgumentException();
    }

    return accountDelegate.retrieveProfile(emailId);
  }

  public void createAccount(CustomerSignup customerSignup) throws AccountCreationException {

    if(customerSignup == null) {
      throw new IllegalArgumentException();
    }

    accountDelegate.createAccount(customerSignup);
  }

  public void createCustomerConsent(CustomerSignup customerSignup, Map<ConsentType, ConsentStatus> consents, String source) {

    if(customerSignup == null || consents == null) {
      throw new IllegalArgumentException();
    }
    accountDelegate.createOrUpdateCustomerConsent(customerSignup, consents, source);
  }

  public CustomerSignup retrieveCustomerSignup(Long userId) {

    if(userId == null) {
      throw new IllegalArgumentException();
    }

    return accountDelegate.retrieveCustomerSignup(userId);
  }

  public String encryptUserIdCookieValue(String userIdCookieValue) {

    if(userIdCookieValue == null) {
      throw new IllegalArgumentException();
    }

    return cryptoDelegate.encrypt(userIdCookieValue);
  }

  public String decryptUserIdCookieValue(String encryptedUserIdCookieValue) {

    if(encryptedUserIdCookieValue == null) {
      throw new IllegalArgumentException();
    }

    return cryptoDelegate.decrypt(encryptedUserIdCookieValue);
  }

  public Boolean isTrialEnabledForProduct(String productName) {

    JSONArray productResourceList = getProductByName(productName);
    Boolean trialEnabled = false;

    for (int i=0; i<productResourceList.length(); i++ ) {
      JSONObject productResource = productResourceList.getJSONObject(i);
      if (productResource.getBoolean("trialEnabled")) {
        trialEnabled = true;
        break;
      }
    }
    return trialEnabled;
  }

  public JSONObject trialApplicationCreation(String productName, Long userID, String language, Cookie navIDCookie, Long gcParentApplicationId) throws JsonProcessingException {
    HashMap<String, Object> newApplicationMap = new HashMap<>();

    newApplicationMap.put("productName", productName);
    newApplicationMap.put("userID", userID);
    newApplicationMap.put("language", language);

    if (navIDCookie != null) {
      newApplicationMap.put("navIDCookie", navIDCookie.getValue());
    }

    String newApplication = new ObjectMapper().writeValueAsString(newApplicationMap);
    JSONObject application = createTrial(newApplication, gcParentApplicationId);

    return application;
  }

  public JSONArray getIndependentlySalableProductsByName(String productName) {
    HttpResponse<JsonNode> jsonNodeHttpResponse;

    String productEndpointURI = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .replacePath(String.format(ENDPOINT_URI_TO_RETRIEVE_INDEPENDENTLYSALABLEPRODUCT, productName)).build().toUriString();

    try {
      jsonNodeHttpResponse = Unirest.get(productEndpointURI).basicAuth(environment.getProperty("auth.user"), "").asJson();
    } catch (UnirestException e) {
      throw new RuntimeException(String.format("Error while endpoint request - %s", e));
    }

    if(jsonNodeHttpResponse.getStatus() !=200){
      throw new RuntimeException(String.format("REST endpoint %s returned status code: %d",
              productEndpointURI, jsonNodeHttpResponse.getStatus()));
    }

    return jsonNodeHttpResponse.getBody().getArray();
  }

  public JSONArray getProductByName(String productName) {
    HttpResponse<JsonNode> jsonNodeHttpResponse;

    String productEndpointURI = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .replacePath(String.format(ENDPOINT_URI_TO_RETRIEVE_PRODUCT_BY_NAME, productName)).build().toUriString();

    try {
      jsonNodeHttpResponse = Unirest.get(productEndpointURI).basicAuth(environment.getProperty("auth.user"), "").asJson();
    } catch (UnirestException e) {
      throw new RuntimeException(String.format("Error while endpoint request - %s", e));
    }

    if(jsonNodeHttpResponse.getStatus() !=200){
      throw new RuntimeException(String.format("REST endpoint %s returned status code: %d",
              productEndpointURI, jsonNodeHttpResponse.getStatus()));
    }

    return jsonNodeHttpResponse.getBody().getArray();
  }

  public JSONObject createTrial(String newApplication, Long gcParentApplicationId) {
    HttpResponse<JsonNode> jsonNodeHttpResponse;

    String trialEndpointURI = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .replacePath(TRIAL_ENDPOINT_URI).build().toUriString();

    try {
      if (gcParentApplicationId != null) {
        jsonNodeHttpResponse = Unirest.post(trialEndpointURI)
                .basicAuth(environment.getProperty("auth.user"), "")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .queryString("gcParentApplicationId", gcParentApplicationId)
                .body(newApplication).asJson();
      } else {
        jsonNodeHttpResponse = Unirest.post(trialEndpointURI)
                .basicAuth(environment.getProperty("auth.user"), "")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .body(newApplication).asJson();
      }

    } catch (UnirestException e) {
      throw new RuntimeException(String.format("Error while endpoint request - %s", e));
    }

    return jsonNodeHttpResponse.getBody().getObject();
  }

  public HttpResponse<JsonNode> getGCParentApplicationId(Long customerId) {
    HttpResponse<JsonNode> jsonNodeHttpResponse;

    String gcParentApplicationIdEndpointURI = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .replacePath(String.format(ENDPOINT_URI_TO_RETRIEVE_CUSTOMER_PARENT_APPLICATION_IDS, customerId)).build().toUriString();

    try {
      jsonNodeHttpResponse = Unirest.get(gcParentApplicationIdEndpointURI).basicAuth(environment.getProperty("auth.user"), "").asJson();
    } catch (UnirestException e) {
      throw new RuntimeException(String.format("Error while endpoint request - %s", e));
    }

    if (jsonNodeHttpResponse.getStatus() != 200 && jsonNodeHttpResponse.getStatus() != 204) {
      throw new RuntimeException(String.format("REST endpoint %s returned status code: %d",
              gcParentApplicationIdEndpointURI, jsonNodeHttpResponse.getStatus()));
    }

    return jsonNodeHttpResponse;
  }

  public Boolean isActiveProduct(String productName) {

    if(productName == null) {
      throw new IllegalArgumentException();
    }

    if (productDelegate.getProduct(productName) != null) {
      return true;
    }
    return false;
  }

  public BigDecimal getProductCostByName(String productName) {
    JSONArray productResourceList = getProductByName(productName);
    BigDecimal cost = BigDecimal.ZERO;

    for (int i=0; i<productResourceList.length(); i++ ) {
      JSONObject productResource = productResourceList.getJSONObject(i);
        cost = productResource.getBigDecimal("price");
    }
    return cost;
  }

  public void postContactToHubspot(CustomerSignup customerSignup, String hubspotToken, String eqFlowsCookie, String emailConsent, String smsConsent) {

    String site = TenantContextHolder.getTenantId();

    HashMap<String, String> hsContextMap = new HashMap<>();
    String phoneNumber = "NA";
    String formattedPhoneNumber = "NA";
    Map<String,Object> queryParam = new HashMap<>();

    hsContextMap.put("hutk", hubspotToken);
    hsContextMap.put("ipAddress", customerSignup.getSignupIPAddress());

    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
    if(customerSignup.getTelephoneCountryCode() != null && customerSignup.getTelephone() != null){
      phoneNumber = customerSignup.getTelephone();
      formattedPhoneNumber = String.format("+%s%s",customerSignup.getTelephoneCountryCode(),customerSignup.getTelephone());
    }

    if(emailConsent != null){
      queryParam.put("email_consent",emailConsent);
    }

    if(smsConsent != null){
      queryParam.put("sms_consent",smsConsent);
    }

    try {
      HttpResponse<String> response = Unirest
              .post("https://forms.hubspot.com/uploads/form/v2/{portal_id}/{guid}")
              .routeParam("portal_id", environment.getProperty("hubspot.portal.id"))
              .routeParam("guid", environment.getProperty("hubspot.guid"))
              .queryString("hs_context", new Gson().toJson(hsContextMap))
              .queryString(site+"_customerid", customerSignup.getId())
              .queryString("email", customerSignup.getEmailId())
              .queryString("firstname", customerSignup.getFirstName())
              .queryString("lastname", customerSignup.getLastName())
              /* The internal name is wrongly set in HubSpot with a typo.  Don't change it.  It must be `firsname_gvs` only */
              .queryString("firsname_gvs", customerSignup.getFirstName().replace(" ", "%20"))
              .queryString("lastname_gvs", customerSignup.getLastName().replace(" ", "%20"))
              .queryString("3cxphone", formattedPhoneNumber)
              .queryString("phone", (smsConsent.equals(ConsentStatus.OPT_IN.toString())) ? phoneNumber : "")
              .queryString("mobilephone", (smsConsent.equals(ConsentStatus.OPT_IN.toString())) ? "" : phoneNumber)
              .queryString(site+"_signupdate", formatter.format(customerSignup.getSignupDate()))
              .queryString(site+"_websiteurl", customerSignup.getSite())
              .queryString(site+"_greenCardExpDate", getGreenCardExpirationDate(customerSignup.getInterestedProduct(),eqFlowsCookie))
              .queryString(site+"_interestedproduct", StringUtils.isNotBlank(customerSignup.getInterestedProduct()) ? String.format("%s-%s",customerSignup.getInterestedProduct(),customerSignup.getLanguage()) : "NA")
              .queryString(site+"_productprice", StringUtils.isNotBlank(customerSignup.getInterestedProduct()) ? getProductCostByName(customerSignup.getInterestedProduct()) : BigDecimal.ZERO)
              .queryString(site+"_language", Locale.forLanguageTag(customerSignup.getLanguage()).getDisplayLanguage())
              .queryString(site+"_autologinurl", (customerSignup.getEmailId() != null) ? String.format(EMAILLANDING_URL_WITH_EMAILID,customerSignup.getSite(),customerSignup.getEmailId()
              ) : String.format(EMAILLANDING_URL_WITHOUT_EMAILID,customerSignup.getSite()))
              .queryString(queryParam)
              .asString();
      LOGGER.info(String.format("creating contact %s in Hubspot returned with status %s and response %s",customerSignup.getEmailId(), response.getStatus(), response.getBody()));
    } catch (UnirestException e) {
      LOGGER.error(String.format("Exception posting contact to HubSpot for user %s", customerSignup.getId()) + e);
    }

  }

  public void updateEQDataCustomerId(CustomerSignup customerSignup, String eqId){
      List<EQDataTable> eqIDList = eqDataTableDAO.findByIds(decryptEQFlows(eqId));
      for (EQDataTable eqDataTable : eqIDList) {
          eqDataTable.setUserId(customerSignup.getId());
          eqDataTableDAO.saveOrUpdate(eqDataTable);
          LOGGER.info(String.format("UserId for eqid %s is %s", customerSignup.getId(), eqDataTable.getId()));
      }
  }

  private String getGreenCardExpirationDate(String product, String eqFlowsCookie) {
      //
      // If user takes multiple product EQ, we will have multiple ids
      //
      if(StringUtils.isBlank(eqFlowsCookie)){
          return "NA";
      }
      List<EQDataTable> list = eqDataTableDAO.findByProductAndIds(product,decryptEQFlows(eqFlowsCookie));
      for (EQDataTable eqDataTable: list) {
        try {
          String bean = environment.getProperty(String.format("xpath.greenCardExpBean.%s", eqDataTable.getProduct()));
          if (bean == null) {
            continue;
          }
          DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
          Document document = documentBuilder.parse(new InputSource(new StringReader(eqDataTable.getData())));
          String expDate = getDate(document, bean);
          if (expDate != null) {
            return expDate;
          }
        } catch (Exception ex) {
          LOGGER.error(String.format("Error fetching GC expiration date due to exception %s",ex.toString()));
        }
      }
    return "NA";
  }

  private String getDate(Document document, String bean) {
    try {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        XPathExpression xPathExpressionDay = xPath.compile(String.format("%s/day", bean));
        XPathExpression xPathExpressionMonth = xPath.compile(String.format("%s/month", bean));
        XPathExpression xPathExpressionYear = xPath.compile(String.format("%s/year", bean));
        XPathExpression xPathExpressionUnknown = xPath.compile(String.format("%s/unknown", bean));
        String day = xPathExpressionDay.evaluate(document);
        String month = xPathExpressionMonth.evaluate(document);
        String year = xPathExpressionYear.evaluate(document);
        String gcExpDate="Card does not have expiration date";
        boolean unknown = Boolean.parseBoolean(xPathExpressionUnknown.evaluate(document));
        if(StringUtils.isBlank(day) || StringUtils.isBlank(month) || StringUtils.isBlank(year)){
            return "NA";
        }
        if (!unknown) {
            LocalDate localDate = LocalDate.parse(String.format("%s-%s-%s", day, month, year), DateTimeFormatter.ofPattern("d-MMM-yyyy"));
            gcExpDate = localDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        }
        LOGGER.info(String.format("Green Card Expiration Date is %s",gcExpDate));
        return gcExpDate;

    } catch (Exception ex) {
       LOGGER.error(String.format("Error parsing GC Expiration Date %s",ex.toString()));
    }
    return "NA";
  }

  public Boolean isValidUserToRegister(String emailId, String password) {
    String emailRegex = "^.{1,64}@.{1,255}$";
    String passwordRegex = "^[0-9A-Za-z\\!\\@\\#\\$\\%\\^\\&\\*\\+\\_\\-\\}\\{\\[\\]\\'\\<\\>\\,\\.\\/\\?\\|]{6,30}$";

    if (emailId == null) {
      throw new IllegalArgumentException();
    }

    Pattern emailPattern = Pattern.compile(emailRegex);

    if (StringUtils.isNotBlank(password)) {
      Pattern passwordPattern = Pattern.compile(passwordRegex);
      return (emailPattern.matcher(emailId).matches() && passwordPattern.matcher(password).matches());
    }

    return emailPattern.matcher(emailId).matches();

  }

  public Boolean isValidUserToUpdateProfile(String firstName, String lastName, String telephone) {
    String nameRegex = "^[a-zA-Z\\s|\\.|\\-|\\'|\\#]{1,49}$";

    if (firstName == null) {
      throw new IllegalArgumentException();
    }
    if (lastName == null) {
      throw new IllegalArgumentException();
    }

    Pattern namePattern = Pattern.compile(nameRegex);
    return (namePattern.matcher(firstName).matches() && namePattern.matcher(lastName).matches() && isValidTelephone(telephone));
  }

  public Boolean isValidTelephone(String telephone) {

    if (StringUtils.isNotEmpty(telephone)) {
      // Regex for US and non US phone numbers
      String phoneNumberRegex = "^[0-9]{0,50}$";
      Pattern phoneNumberPattern = Pattern.compile(phoneNumberRegex);
      return phoneNumberPattern.matcher(telephone).matches();
    }

    return true;
  }

  public List<Long> decryptEQFlows(String eqId) {
      List<Long> idList = new ArrayList<>();
      if (StringUtils.isNotBlank(eqId)) {
          String ids = cryptoDelegate.decryptExternal(eqId);
          for (String id : ids.split(",")) {
              idList.add(Long.parseLong(id));
          }
      }
      return idList;
  }

  public void postContactToGetResponse(CustomerSignup customerSignup, Map<String, String> customProperties){
    String url = String.format(GETRESPONSE_CREATE_CONTACT, environment.getProperty("getresponse.api.url"));
    String name = customerSignup.getFirstName().concat(" ").concat(customerSignup.getLastName());
    try {
      HttpResponse<JsonNode> response = Unirest.post(url)
              .header("Content-Type", "application/json")
              .header("X-Auth-Token", environment.getProperty("getresponse.auth.token"))
              .body(contactJson(customerSignup.getFirstName(), customerSignup.getEmailId(), customProperties))
              .asJson();
      LOGGER.info(String.format("creating contact %s in getResponse returned with status %s and response %s",customerSignup.getEmailId(), response.getStatus(), response.getBody()));
    }  catch (UnirestException e) {
      LOGGER.error("Unable create contact", e);
      throw new RuntimeException(e);
    }
  }

  private JSONObject contactJson(String name, String email, Map<String, String> customProperties){
    JSONObject contactRequest = new JSONObject();
    if(environment.getProperty("getResponse.campaignId") != null && !environment.getProperty("getResponse.campaignId").isEmpty()){
      contactRequest.put("campaign", new JSONObject().put("campaignId",environment.getProperty("getResponse.campaignId")));
    }
    contactRequest.put("email",email);
    contactRequest.put("name",name);
    JSONArray customFieldValues  = new JSONArray();
    customProperties.forEach((key,value) -> {
      customFieldValues.put(new JSONObject().put("customFieldId",environment.getProperty(key.concat(".customFieldId")))
              .put("value",new JSONArray()
                      .put(value)));
    });
    contactRequest.put("customFieldValues",customFieldValues);
    return contactRequest;
  }

  public boolean isTestUser(String emailId, String signupIPAddress) {
    boolean testUser = false;

    if(emailId != null){
      String[] emailIdSegments = emailId.toLowerCase().split("@");
      String[] emailTestDomainList = environment.getProperty("emailExclusionList.domain", "na").split(",");
        if(!emailTestDomainList[0].equals("na")){
          for (String filter : emailTestDomainList) {
            if (emailIdSegments[1].contains(filter)) {
                LOGGER.info(String.format("Email Id %s will be marked as Test user", emailId));
              testUser = true;
            }
          }
        }

      String[] emailTestPartList = environment.getProperty("emailExclusionList.localpart", "na").split(",");
      if (!emailTestPartList[0].equals("na")) {
        for (String emailExclusionLocalPart : emailTestPartList) {
          if (emailIdSegments[0].contains(emailExclusionLocalPart)) {
            LOGGER.info(String.format("Email Id %s will be marked as Test user", emailId));
            testUser = true;
          }
        }
      }

    } else if (signupIPAddress.startsWith("10.0")) {
      testUser =true;
    }
    return testUser;
  }

    public void unsubscribeUserFromFD(CustomerSignup customerSignup, Boolean markDeleted, Map<ConsentType, ConsentStatus> consents, String source) {
        Long userId = customerSignup.getId();
        accountDelegate.createOrUpdateCustomerConsent(customerSignup, consents, source);
        accountDelegate.deleteCustomer(customerSignup, consents, markDeleted);
        if (markDeleted) {
            String cancelOrderEndpointURI = ServletUriComponentsBuilder.fromCurrentRequestUri()
                    .replacePath(String.format(ENDPOINT_URI_TO_CANCEL_CUSTOMER_ORDERS, customerSignup.getId())).build().toUriString();
            HttpResponse<String> response;
            try {
                response = Unirest.delete(cancelOrderEndpointURI).basicAuth(environment.getProperty("auth.user"), "").asString();
            } catch (UnirestException e) {
                throw new RuntimeException(String.format("Error while cancel order endpoint request - %s", e));
            }

            if (response.getStatus() != 200) {
                throw new RuntimeException(String.format("REST endpoint %s returned status code: %d",
                        cancelOrderEndpointURI, response.getStatus()));
            }
        }
        LOGGER.info(String.format("User %s deleted successfully from FD Database userId ::", userId));
    }
}
