package com.poc.merchant.infrastructure.persistence.entity;

import com.poc.merchant.domain.model.MerchantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "merchants")
public class MerchantJpaEntity {

    @Id
    @Column(name = "merchant_id", nullable = false, updatable = false, length = 64)
    private String merchantId;

    @Column(name = "legal_name", nullable = false, length = 255)
    private String legalName;

    @Column(name = "document_number", nullable = false, length = 32, unique = true)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MerchantStatus status;

    protected MerchantJpaEntity() {
    }

    public MerchantJpaEntity(String merchantId, String legalName, String documentNumber, MerchantStatus status) {
        this.merchantId = merchantId;
        this.legalName = legalName;
        this.documentNumber = documentNumber;
        this.status = status;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public MerchantStatus getStatus() {
        return status;
    }
}
