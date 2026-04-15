package com.poc.merchant.application.service;

import com.poc.merchant.domain.exception.MerchantNotFoundException;
import com.poc.merchant.domain.model.Merchant;
import com.poc.merchant.domain.port.MerchantRepository;
import org.springframework.stereotype.Service;

@Service
public class GetMerchantUseCase {

    private final MerchantRepository merchantRepository;

    public GetMerchantUseCase(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public Merchant execute(String merchantId) {
        return merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant not found"));
    }
}
