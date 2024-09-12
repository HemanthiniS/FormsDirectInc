package com.formsdirectinc.dao;

import java.util.Date;

public class UserVisitSource implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String propertyKey;
    private String propertyValue;
    private Date createdOn;

    public UserVisitSource() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPropertyKey() {
        return this.propertyKey;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public String getPropertyValue() {
        return this.propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Date getCreatedOn() {
        return this.createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

}
