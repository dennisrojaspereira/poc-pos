package com.poc.pos.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TransactionMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public TransactionMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String operation, String outcome) {
        Counter.builder("pos_transactions_business_total")
                .description("Business transaction operations grouped by operation, outcome and feature variant")
                .tag("operation", operation)
                .tag("outcome", outcome)
                .tag("feature_variant", RequestMetadataContext.getFeatureVariant())
                .register(meterRegistry)
                .increment();
    }
}
