package com.formsdirectinc.services;

import com.formsdirectinc.dao.BrowserData;
import com.formsdirectinc.dao.BrowserDataDAO;
import com.formsdirectinc.dao.CustomerSignup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BrowserDataService {

    @Autowired
    protected BrowserDataDAO browserDataDAO;

    public void storeBrowserData(String customerIPAddress, String refererCookie, String acceptLanguageHeader,
                    String userAgentHeader, String geoIpCityHeader, String geoIpRegionHeader, String geoIpCountryHeader,
                    CustomerSignup customerSignup) {

        BrowserData browserData = new BrowserData();

        browserData.setCustomerId(customerSignup.getId());
        browserData.setCustomerIpAddress(customerIPAddress);
        browserData.setBrowserLanguage(acceptLanguageHeader);
        browserData.setCustomerCityName(geoIpCityHeader);
        browserData.setCustomerStateName(geoIpRegionHeader);
        browserData.setCustomerCountryName(geoIpCountryHeader);
        browserData.setUserAgent(userAgentHeader);
        browserData.setHttpReferer(refererCookie);

        //
        // Save the browser data information
        //

        browserDataDAO.save(browserData);
    }
}