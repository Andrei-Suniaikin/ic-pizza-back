package com.icpizza.backend.jahez;

import com.icpizza.backend.jahez.api.JahezAuthManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/internal/jahez")
@RequiredArgsConstructor
public class JahezTokenDebugController {
    private final JahezAuthManager auth;

    @GetMapping("/token-status")
    public Mono<TokenStatus> tokenStatus() {
        return auth.getBearerToken().map(tok -> {
            var expOpt = extractExp(tok);
            return new TokenStatus(
                    true,
                    expOpt.map(Instant::toString).orElse(null),
                    mask(tok)
            );
        });
    }

    private static Optional<Instant> extractExp(String jwt) {
        try {
            String[] p = jwt.split("\\.");
            if (p.length < 2) return Optional.empty();
            var payload = new String(Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8);
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
            return node.has("exp") ? Optional.of(Instant.ofEpochSecond(node.get("exp").asLong())) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String mask(String tok) {
        if (tok == null || tok.length() < 10) return "***";
        return tok.substring(0, 6) + "..." + tok.substring(tok.length() - 6);
    }

    public record TokenStatus(boolean ok, String expUtc, String tokenMasked) {}
}
