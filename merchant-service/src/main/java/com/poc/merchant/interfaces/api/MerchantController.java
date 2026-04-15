package com.poc.merchant.interfaces.api;

import com.poc.merchant.application.command.CreateMerchantCommand;
import com.poc.merchant.application.service.CreateMerchantUseCase;
import com.poc.merchant.application.service.GetMerchantUseCase;
import com.poc.merchant.application.service.ListMerchantsUseCase;
import com.poc.merchant.domain.model.Merchant;
import com.poc.merchant.interfaces.api.dto.CreateMerchantRequest;
import com.poc.merchant.interfaces.api.dto.MerchantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final CreateMerchantUseCase createMerchantUseCase;
    private final ListMerchantsUseCase listMerchantsUseCase;
    private final GetMerchantUseCase getMerchantUseCase;

    public MerchantController(
            CreateMerchantUseCase createMerchantUseCase,
            ListMerchantsUseCase listMerchantsUseCase,
            GetMerchantUseCase getMerchantUseCase
    ) {
        this.createMerchantUseCase = createMerchantUseCase;
        this.listMerchantsUseCase = listMerchantsUseCase;
        this.getMerchantUseCase = getMerchantUseCase;
    }

    @PostMapping
    public ResponseEntity<MerchantResponse> create(@Valid @RequestBody CreateMerchantRequest request) {
        Merchant merchant = createMerchantUseCase.execute(new CreateMerchantCommand(
                request.merchantId(),
                request.legalName(),
                request.documentNumber()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(MerchantResponse.from(merchant));
    }

    @GetMapping
    public ResponseEntity<List<MerchantResponse>> list() {
        return ResponseEntity.ok(listMerchantsUseCase.execute().stream()
                .map(MerchantResponse::from)
                .toList());
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantResponse> get(@PathVariable String merchantId) {
        return ResponseEntity.ok(MerchantResponse.from(getMerchantUseCase.execute(merchantId)));
    }
}
