package com.formsdirectinc.dao;

public class Leads implements java.io.Serializable {

    private Long id;
    private Long customerId;
    private String contactId;
    private Long orderId;
    private String dealId;
    private String dealName;
    private Boolean dealStatus;
    private String dealSource;

    public Leads(){}
    public Leads(Long id, Long customerId, String contactId, Long orderId, String dealId, String dealName, Boolean dealStatus, String dealSource){
        this.id = id;
        this.customerId = customerId;
        this.contactId = contactId;
        this.orderId = orderId;
        this.dealId = dealId;
        this.dealName = dealName;
        this.dealStatus = dealStatus;
        this.dealSource = dealSource;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public String getDealName() {
        return dealName;
    }

    public void setDealName(String dealName) {
        this.dealName = dealName;
    }

    public Boolean getDealStatus() {
        return dealStatus;
    }

    public void setDealStatus(Boolean dealStatus) {
        this.dealStatus = dealStatus;
    }

    public String getDealSource() {
        return dealSource;
    }

    public void setDealSource(String dealSource) {
        this.dealSource = dealSource;
    }
}
