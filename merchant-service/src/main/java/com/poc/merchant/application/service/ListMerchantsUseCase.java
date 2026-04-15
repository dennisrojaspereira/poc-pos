package com.poc.merchant.application.service;

import com.poc.merchant.domain.model.Merchant;
import com.poc.merchant.domain.port.MerchantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMerchantsUseCase {

    private final MerchantRepository merchantRepository;

    public ListMerchantsUseCase(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public List<Merchant> execute() {
        return merchantRepository.findAll();
    }
}
