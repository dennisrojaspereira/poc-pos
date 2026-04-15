package com.poc.pos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.poc.pos.monitoring.RequestMetadataContext;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HmacAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of("/authorize", "/confirm", "/void");

    private final String secret;
    private final Duration allowedClockSkew;
    private final Clock clock;
    private final HmacReplayProtectionService replayProtectionService;

    @Autowired
    public HmacAuthenticationFilter(
            @Value("${app.security.hmac.secret:change-me}") String secret,
            @Value("${app.security.hmac.allowed-clock-skew:PT5M}") Duration allowedClockSkew,
            HmacReplayProtectionService replayProtectionService
    ) {
        this(secret, allowedClockSkew, Clock.systemUTC(), replayProtectionService);
    }

    HmacAuthenticationFilter(
            String secret,
            Duration allowedClockSkew,
            Clock clock,
            HmacReplayProtectionService replayProtectionService
    ) {
        this.secret = secret;
        this.allowedClockSkew = allowedClockSkew;
        this.clock = clock;
        this.replayProtectionService = replayProtectionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);

            String timestampHeader = requiredHeader(wrappedRequest, "X-Timestamp");
            String correlationId = requiredHeader(wrappedRequest, "X-Correlation-Id");
            String signature = requiredHeader(wrappedRequest, "X-Signature");
            RequestMetadataContext.setFeatureVariant(wrappedRequest.getHeader("X-Feature-Variant"));
            RequestMetadataContext.setCorrelationId(correlationId);
            Instant requestTimestamp = parseTimestamp(timestampHeader);
            validateTimestamp(requestTimestamp);

            String body = new String(wrappedRequest.getCachedBody(), StandardCharsets.UTF_8);
            String canonicalPayload = request.getMethod()
                    + "\n" + request.getRequestURI()
                    + "\n" + timestampHeader
                    + "\n" + correlationId
                    + "\n" + body;
            String computedSignature = computeSignature(canonicalPayload);

            if (!MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            )) {
                throw new HmacAuthenticationException("Invalid HMAC signature");
            }
            replayProtectionService.ensureNotReplayed(computedSignature);

            response.setHeader("X-Correlation-Id", correlationId);
            MDC.put("correlationId", correlationId);
            filterChain.doFilter(wrappedRequest, response);
        } catch (HmacAuthenticationException exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
        } finally {
            MDC.remove("correlationId");
            RequestMetadataContext.clear();
        }
    }

    private String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw new HmacAuthenticationException(name + " header is required");
        }
        return value;
    }

    private Instant parseTimestamp(String timestampHeader) {
        try {
            return Instant.parse(timestampHeader);
        } catch (Exception exception) {
            throw new HmacAuthenticationException("Invalid timestamp format");
        }
    }

    private void validateTimestamp(Instant requestTimestamp) {
        Duration drift = Duration.between(requestTimestamp, Instant.now(clock)).abs();
        if (drift.compareTo(allowedClockSkew) > 0) {
            throw new HmacAuthenticationException("Expired request timestamp");
        }
    }

    private String computeSignature(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new HmacAuthenticationException("Unable to validate HMAC signature");
        }
    }
}
