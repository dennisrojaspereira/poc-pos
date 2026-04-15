package com.poc.merchant.domain.port;

import com.poc.merchant.domain.model.Merchant;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository {

    Merchant save(Merchant merchant);

    Optional<Merchant> findByMerchantId(String merchantId);

    Optional<Merchant> findByDocumentNumber(String documentNumber);

    List<Merchant> findAll();
}
