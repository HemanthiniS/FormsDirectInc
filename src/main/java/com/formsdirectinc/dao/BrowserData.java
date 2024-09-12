package com.formsdirectinc.dao;

public class BrowserData implements java.io.Serializable {

	private long id;
	private Long customerId;
	private String customerIpAddress;
	private String customerCityName;
	private String customerStateName;
	private String customerCountryName;
	private String userAgent;
	private String httpReferer;
	private String browserLanguage;

	public BrowserData() {
	}

	public BrowserData(long id, Long customerId, String customerIpAddress, String customerCityName,
					String customerStateName, String customerCountryName, String userAgent, String httpReferer,
					String browserLanguage) {
		this.id = id;
		this.customerId = customerId;
		this.customerIpAddress = customerIpAddress;
		this.customerCityName = customerCityName;
		this.customerStateName = customerStateName;
		this.customerCountryName = customerCountryName;
		this.userAgent = userAgent;
		this.httpReferer = httpReferer;
		this.browserLanguage = browserLanguage;
	}

	// Getters & Setters for BrowserData fields

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Long getCustomerId() {
		return this.customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public String getCustomerIpAddress() {
		return this.customerIpAddress;
	}

	public void setCustomerIpAddress(String customerIpAddress) {
		this.customerIpAddress = customerIpAddress;
	}

	public String getCustomerCityName() {
		return this.customerCityName;
	}

	public void setCustomerCityName(String customerCityName) {
		this.customerCityName = customerCityName;
	}

	public String getCustomerStateName() {
		return this.customerStateName;
	}

	public void setCustomerStateName(String customerStateName) {
		this.customerStateName = customerStateName;
	}

	public String getCustomerCountryName() {
		return this.customerCountryName;
	}

	public void setCustomerCountryName(String customerCountryName) {
		this.customerCountryName = customerCountryName;
	}

	public String getUserAgent() {
		return this.userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getHttpReferer() {
		return this.httpReferer;
	}

	public void setHttpReferer(String httpReferer) {
		this.httpReferer = httpReferer;
	}

	public String getBrowserLanguage() {
		return this.browserLanguage;
	}

	public void setBrowserLanguage(String browserLanguage) {
		this.browserLanguage = browserLanguage;
	}

}
