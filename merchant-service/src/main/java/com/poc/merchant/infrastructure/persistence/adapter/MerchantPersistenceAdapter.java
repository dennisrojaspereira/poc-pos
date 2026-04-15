package com.poc.merchant.infrastructure.persistence.adapter;

import com.poc.merchant.domain.model.Merchant;
import com.poc.merchant.domain.port.MerchantRepository;
import com.poc.merchant.infrastructure.persistence.entity.MerchantJpaEntity;
import com.poc.merchant.infrastructure.persistence.repository.SpringDataMerchantJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MerchantPersistenceAdapter implements MerchantRepository {

    private final SpringDataMerchantJpaRepository repository;

    public MerchantPersistenceAdapter(SpringDataMerchantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Merchant save(Merchant merchant) {
        MerchantJpaEntity saved = repository.save(toEntity(merchant));
        return toDomain(saved);
    }

    @Override
    public Optional<Merchant> findByMerchantId(String merchantId) {
        return repository.findById(merchantId).map(this::toDomain);
    }

    @Override
    public Optional<Merchant> findByDocumentNumber(String documentNumber) {
        return repository.findByDocumentNumber(documentNumber).map(this::toDomain);
    }

    @Override
    public List<Merchant> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private MerchantJpaEntity toEntity(Merchant merchant) {
        return new MerchantJpaEntity(
                merchant.getMerchantId(),
                merchant.getLegalName(),
                merchant.getDocumentNumber(),
                merchant.getStatus()
        );
    }

    private Merchant toDomain(MerchantJpaEntity entity) {
        return new Merchant(
                entity.getMerchantId(),
                entity.getLegalName(),
                entity.getDocumentNumber(),
                entity.getStatus()
        );
    }
}
