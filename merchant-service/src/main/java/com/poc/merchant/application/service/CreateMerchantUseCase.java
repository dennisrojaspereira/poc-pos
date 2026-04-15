package com.poc.merchant.application.service;

import com.poc.merchant.application.command.CreateMerchantCommand;
import com.poc.merchant.domain.model.Merchant;
import com.poc.merchant.domain.port.MerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateMerchantUseCase {

    private final MerchantRepository merchantRepository;

    public CreateMerchantUseCase(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Transactional
    public Merchant execute(CreateMerchantCommand command) {
        merchantRepository.findByMerchantId(command.merchantId()).ifPresent(existing -> {
            throw new IllegalArgumentException("merchantId already exists");
        });
        merchantRepository.findByDocumentNumber(command.documentNumber()).ifPresent(existing -> {
            throw new IllegalArgumentException("documentNumber already exists");
        });

        Merchant merchant = Merchant.create(
                command.merchantId(),
                command.legalName(),
                command.documentNumber()
        );
        return merchantRepository.save(merchant);
    }
}
