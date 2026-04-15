package com.poc.merchant.domain.model;

public class Merchant {

    private final String merchantId;
    private final String legalName;
    private final String documentNumber;
    private final MerchantStatus status;

    public Merchant(String merchantId, String legalName, String documentNumber, MerchantStatus status) {
        this.merchantId = merchantId;
        this.legalName = legalName;
        this.documentNumber = documentNumber;
        this.status = status;
    }

    public static Merchant create(String merchantId, String legalName, String documentNumber) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId is required");
        }
        if (legalName == null || legalName.isBlank()) {
            throw new IllegalArgumentException("legalName is required");
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("documentNumber is required");
        }
        return new Merchant(merchantId, legalName, documentNumber, MerchantStatus.ACTIVE);
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
