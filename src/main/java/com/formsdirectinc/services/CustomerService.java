package com.formsdirectinc.services;

import com.formsdirectinc.dao.UserVisitSource;
import com.formsdirectinc.dao.UserVisitSourceDAO;
import com.formsdirectinc.environment.Environment;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService {

    private static Logger LOGGER = Logger.getLogger(CustomerService.class);

    @Autowired
    private UserVisitSourceDAO userVisitSourceDAO;

    @Autowired
    private Environment environment;

    public boolean isEmailIdExcluded(String emailId, String signupIPAddress) {

        // split email into local-part and domain part for separate checks
        if (emailId != null) {
            String[] emailIdSegments = emailId.toLowerCase().split("@");

        /*
         * check the domain of email ID to exclude emails based on the domains
         * specified by the emailExclusionList.domain property
         */
            String[] emailExclusionDomainList = environment.getProperty("emailExclusionList.domain", "na").split(",");
            if (!emailExclusionDomainList[0].equals("na")) {
                for (String emailExclusionDomain : emailExclusionDomainList) {
                    if (emailIdSegments[1].contains(emailExclusionDomain)) {
                        LOGGER.info(String.format("Email Id %s will be marked for exclusion", emailId));
                        return false;
                    }
                }
            }

        /*
         * check the local-part of email ID to exclude emails based on the local-parts
         * specified by the emailExclusionList.localpart property
         */
            String[] emailExclusionLocalPartList = environment.getProperty("emailExclusionList.localpart", "na").split(",");
            if (!emailExclusionLocalPartList[0].equals("na")) {
                for (String emailExclusionLocalPart : emailExclusionLocalPartList) {
                    if (emailIdSegments[0].contains(emailExclusionLocalPart)) {
                        LOGGER.info(String.format("Email Id %s will be marked for exclusion", emailId));
                        return false;
                    }
                }
            }

            return true;

        } else if (signupIPAddress.startsWith("10.0")) {
            /*
             * If Email-ID doesn't exists, then validate for test account based on the sign-up IP Address.
             *
             */
            return false;
        } else {
            return true;
        }

    }

    public Map<String, Cookie> retrieveSourceTrackingCookies(Cookie[] cookies) {
        Map<String, Cookie> trackingCookieMap = new HashMap<>();
        if(cookies != null) {
            for(Cookie cookie : cookies) {
                List<String> googleClickIdCookieNamesList = environment.getPropertiesAsListOrDefault("googleClickIdCookieNamesList", new ArrayList<>());
                List<String> customSourceTrackingCookieList = environment.getPropertiesAsListOrDefault("customSourceTrackingCookieList", new ArrayList<>());
                List<String> bingClickIdCookieNamesList = environment.getPropertiesAsListOrDefault("bingClickIdCookieNamesList", new ArrayList<>());
                if(googleClickIdCookieNamesList.contains(cookie.getName())) {
                    trackingCookieMap.put("gclid", cookie);
                }
                if(customSourceTrackingCookieList.contains(cookie.getName().toLowerCase())) {
                    trackingCookieMap.put("r", cookie);
                }
                if(bingClickIdCookieNamesList.contains(cookie.getName().toLowerCase())) {
                    trackingCookieMap.put("msclkid", cookie);
                }
            }
        }

        return trackingCookieMap;
    }

    public void persistUserVisitSource(Long userId, String sourceTrackingCookieName, String sourceTrackingCookieValue) {
        UserVisitSource userVisitSource = new UserVisitSource();
        userVisitSource.setUserId(userId);
        userVisitSource.setPropertyKey(sourceTrackingCookieName);
        userVisitSource.setPropertyValue(sourceTrackingCookieValue);
        if(userVisitSourceDAO.find(userVisitSource) == null) {
            userVisitSourceDAO.save(userVisitSource);
        }
    }
}
