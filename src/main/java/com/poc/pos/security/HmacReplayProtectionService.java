package com.poc.pos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HmacReplayProtectionService {

    private final Duration replayTtl;
    private final ConcurrentHashMap<String, Instant> seenRequests = new ConcurrentHashMap<>();

    public HmacReplayProtectionService(@Value("${app.security.hmac.replay-ttl:PT5M}") Duration replayTtl) {
        this.replayTtl = replayTtl;
    }

    public void ensureNotReplayed(String requestKey) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(replayTtl);

        evictExpiredEntries(now);

        Instant existingExpiration = seenRequests.putIfAbsent(requestKey, expiresAt);
        if (existingExpiration != null && existingExpiration.isAfter(now)) {
            throw new HmacAuthenticationException("Replay attack detected");
        }

        if (existingExpiration != null) {
            seenRequests.put(requestKey, expiresAt);
        }
    }

    private void evictExpiredEntries(Instant now) {
        Iterator<Map.Entry<String, Instant>> iterator = seenRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Instant> entry = iterator.next();
            if (!entry.getValue().isAfter(now)) {
                iterator.remove();
            }
        }
    }
}
