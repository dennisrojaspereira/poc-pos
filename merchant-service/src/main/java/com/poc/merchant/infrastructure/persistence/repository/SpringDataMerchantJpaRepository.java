package com.poc.merchant.infrastructure.persistence.repository;

import com.poc.merchant.infrastructure.persistence.entity.MerchantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataMerchantJpaRepository extends JpaRepository<MerchantJpaEntity, String> {

    Optional<MerchantJpaEntity> findByDocumentNumber(String documentNumber);
}
