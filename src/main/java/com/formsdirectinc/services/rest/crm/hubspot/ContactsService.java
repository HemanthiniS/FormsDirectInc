package com.formsdirectinc.services.rest.crm.hubspot;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.environment.Environment;
import com.formsdirectinc.security.CryptoDelegate;
import com.formsdirectinc.services.account.AccountDelegate;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ContactsService {

  private static Logger LOGGER = Logger.getLogger(ContactsService.class);

  @Autowired
  private Environment environment;

  @Autowired
  private AccountDelegate accountDelegate;

  @Autowired
  private CryptoDelegate cryptoDelegate;

  @RequestMapping(value = "/hubspot", method = RequestMethod.POST)
  public ResponseEntity<Map> postContact(@CookieValue(value = "id.i", required = false) String encryptedUserId,
                                         @CookieValue(value = "hubspotutk", required = false) String hubspotToken,
                                         HttpServletRequest request) {

    LOGGER.info("Attempting HubSpot post...");

    if (encryptedUserId == null) {
      LOGGER.error("Encrypted User ID Cookie (id.i) not available");
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    CustomerSignup customerSignup = accountDelegate.retrieveCustomerSignup(
            Long.parseLong(cryptoDelegate.decrypt(encryptedUserId)));

    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

    if (hubspotToken == null) {
      LOGGER.error(String.format("Hubspot token not available for user %s", customerSignup.getId()));
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    HashMap<String, String> hsContextMap = new HashMap<>();

    hsContextMap.put("hutk", hubspotToken);
    hsContextMap.put("ipAddress", request.getRemoteAddr());

    try {
      HttpResponse<String> response = Unirest
              .post("https://forms.hubspot.com/uploads/form/v2/{portal_id}/{guid}")
              .routeParam("portal_id", environment.getProperty("hubspot.portal.id"))
              .routeParam("guid", environment.getProperty("hubspot.guid"))
              .queryString("hs_context", new Gson().toJson(hsContextMap))
              .queryString("email", customerSignup.getEmailId())
              .queryString("firstname", customerSignup.getFirstName())
              .queryString("lastname", customerSignup.getLastName())
              .queryString("phone", customerSignup.getTelephone())
              .queryString("customerid", customerSignup.getId())
              .queryString("interestedproduct", customerSignup.getInterestedProduct())
              .queryString("signupdate", formatter.format(customerSignup.getSignupDate()))
              .queryString("language", customerSignup.getLanguage())
              .asString();

      LOGGER.info(String.format("Contact post status for user id %s is %s", customerSignup.getId(), response
              .getStatus()));
    } catch (UnirestException e) {
      LOGGER.error(String.format("Exception posting contact to HubSpot for user %s", customerSignup.getId()) + e);
    }
    return new ResponseEntity<>(HttpStatus.CREATED);
  }
}
