package com.formsdirectinc.dao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formsdirectinc.services.account.ProductTypeEnum;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {

    private Long productId;
    private String name;
    private String description;
    private ProductTypeEnum productType;
    private BigDecimal price;
    private String descriptionFromRB;
    private Boolean active;
    private Boolean trialEnabled;

    public Product() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductTypeEnum getProductType() {
        return productType;
    }

    public void setProductType(ProductTypeEnum productType) {
        this.productType = productType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescriptionFromRB() {
        return descriptionFromRB;
    }

    public void setDescriptionFromRB(String descriptionFromRB) {
        this.descriptionFromRB = descriptionFromRB;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getTrialEnabled() {
        return trialEnabled;
    }

    public void setTrialEnabled(Boolean trialEnabled) {
        this.trialEnabled = trialEnabled;
    }
}
