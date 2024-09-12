package com.formsdirectinc.dao;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class CustomerConsent implements Serializable {
    private Long id;
    private Long customerId;
    private Set<ConsentHistory> histories = new HashSet<>();
    private String type;
    private String status;
    private Date date;
    private Date rowCreatedAt;
    private Date rowUpdatedAt;
    private Long version;

    public CustomerConsent() {
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

    public Set<ConsentHistory> getHistories() {
        return histories;
    }

    public void setHistories(Set<ConsentHistory> histories) {
        this.histories = histories;
    }

    public void setHistory(ConsentHistory history){
        this.histories.add(history);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
