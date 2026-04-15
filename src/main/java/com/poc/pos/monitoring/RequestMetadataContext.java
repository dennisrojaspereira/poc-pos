package com.poc.pos.monitoring;

public final class RequestMetadataContext {

    private static final ThreadLocal<String> FEATURE_VARIANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private RequestMetadataContext() {
    }

    public static void setFeatureVariant(String featureVariant) {
        FEATURE_VARIANT.set(featureVariant);
    }

    public static String getFeatureVariant() {
        String variant = FEATURE_VARIANT.get();
        return (variant == null || variant.isBlank()) ? "control" : variant;
    }

    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    public static String getCorrelationId() {
        String correlationId = CORRELATION_ID.get();
        return (correlationId == null || correlationId.isBlank()) ? "unknown" : correlationId;
    }

    public static void clear() {
        FEATURE_VARIANT.remove();
        CORRELATION_ID.remove();
    }
}
