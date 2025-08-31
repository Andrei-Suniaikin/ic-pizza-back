package com.icpizza.backend.jahez.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icpizza.backend.jahez.config.JahezProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class JahezAuthManager {
    private final JahezTokenClient tokenClient;
    private final JahezProperties properties;
    private volatile String cachedToken;
    private final AtomicReference<Instant> expiresAt = new AtomicReference<>(Instant.EPOCH);

    public Mono<String> getBearerToken(){
        Instant exp = expiresAt.get();
        if(cachedToken!=null && Instant.now().isBefore(exp.minusSeconds(properties.clockSkewSeconds()))){
            return Mono.just(cachedToken);
        }
        return refreshToken();
    }

    public Mono<String> refreshToken(){
        return tokenClient.fetchToken()
                .map(token -> {
                    cachedToken = token;
                    expiresAt.set(parseExp(token).orElse(Instant.EPOCH));
                    return cachedToken;
                });
    }

    public Optional<Instant> parseExp(String jwt){
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return Optional.empty();
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = new ObjectMapper().readTree(payloadJson);
            if (node.has("exp")) {
                long exp = node.get("exp").asLong();
                return Optional.of(Instant.ofEpochSecond(exp));
            }
        } catch (Exception ignore) {}
        return Optional.empty();
    }
}
