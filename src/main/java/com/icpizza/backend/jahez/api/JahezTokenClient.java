package com.icpizza.backend.jahez.api;

import com.icpizza.backend.jahez.config.JahezProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class JahezTokenClient {
    private final WebClient webClient;
    private final JahezProperties properties;

    public Mono<String> fetchToken(){
        Map<String, Object> body = Map.of("secret", properties.secret());
        return webClient.post()
                .uri("/token")
                .headers(h -> h.set("x-api-key", properties.apiKey()))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(TokenResponse::token);
    }

    public record TokenResponse(boolean success, String token){}
}
