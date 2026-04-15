package com.poc.pos.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ProcessTracing {

    private final ObservationRegistry observationRegistry;
    private final MeterRegistry meterRegistry;

    public ProcessTracing(ObservationRegistry observationRegistry, MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.meterRegistry = meterRegistry;
    }

    public <T> T trace(String spanName, String contextualName, String operation, String step, Supplier<T> supplier) {
        String featureVariant = RequestMetadataContext.getFeatureVariant();
        String correlationId = RequestMetadataContext.getCorrelationId();
        Observation observation = Observation.createNotStarted(spanName, observationRegistry)
                .contextualName(contextualName)
                .lowCardinalityKeyValue("operation", operation)
                .lowCardinalityKeyValue("step", step)
                .lowCardinalityKeyValue("feature_variant", featureVariant)
                .highCardinalityKeyValue("correlation_id", correlationId);
        Timer.Sample sample = Timer.start(meterRegistry);

        observation.start();
        try (Observation.Scope ignored = observation.openScope()) {
            T result = supplier.get();
            observation.lowCardinalityKeyValue("outcome", "success");
            sample.stop(timer(operation, step, featureVariant, "success"));
            return result;
        } catch (RuntimeException exception) {
            observation.error(exception);
            observation.lowCardinalityKeyValue("outcome", "error");
            sample.stop(timer(operation, step, featureVariant, "error"));
            throw exception;
        } finally {
            observation.stop();
        }
    }

    public void traceVoid(String spanName, String contextualName, String operation, String step, Runnable runnable) {
        trace(spanName, contextualName, operation, step, () -> {
            runnable.run();
            return null;
        });
    }

    private Timer timer(String operation, String step, String featureVariant, String outcome) {
        return Timer.builder("pos_process_step_duration")
                .description("Duration of traced process steps grouped by operation and step")
                .tag("operation", operation)
                .tag("step", step)
                .tag("feature_variant", featureVariant)
                .tag("outcome", outcome)
                .register(meterRegistry);
    }
}
