package com.formsdirectinc.dao;

import java.io.Serializable;
import java.util.Date;

public class ConsentHistory implements Serializable {
    private long id;
    private CustomerConsent consent;
    private String status;
    private String source;
    private Date date;
    private Date rowCreatedAt;
    private Date rowUpdatedAt;
    private Long version;

    public ConsentHistory() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public CustomerConsent getConsent() {
        return consent;
    }

    public void setConsent(CustomerConsent consent) {
        this.consent = consent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getRowCreatedAt() {
        return rowCreatedAt;
    }

    public void setRowCreatedAt(Date rowCreatedAt) {
        this.rowCreatedAt = rowCreatedAt;
    }

    public Date getRowUpdatedAt() {
        return rowUpdatedAt;
    }

    public void setRowUpdatedAt(Date rowUpdatedAt) {
        this.rowUpdatedAt = rowUpdatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
