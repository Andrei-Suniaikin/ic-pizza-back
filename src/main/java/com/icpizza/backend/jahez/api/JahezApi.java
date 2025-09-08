package com.icpizza.backend.jahez.api;

import com.icpizza.backend.jahez.config.JahezProperties;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JahezApi {
    private final WebClient webClient;
    private final JahezAuthManager auth;
    private final JahezProperties properties;


    private <T> Mono<T> withAuth(Function<HttpHeaders, Mono<T>> call) {
        return auth.getBearerToken()
                .flatMap(tok -> call.apply(buildHeaders(tok)))
                .onErrorResume(WebClientResponseException.Unauthorized.class, ex ->
                        auth.refreshToken().flatMap(newTok -> call.apply(buildHeaders(newTok)))
                );
    }

    private HttpHeaders buildHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.set("x-api-key", properties.apiKey());
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    public Mono<JahezDTOs.AckSuccess> sendAccepted(long jahezOrderId) {
        var body = Map.of("jahezOrderId", String.valueOf(jahezOrderId), "status", "A");
        return withAuth(h -> webClient.post()
                .uri("/webhooks/status_update")
                .headers(dst -> dst.addAll(h))
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(msg -> new IllegalStateException("Jahez failed: " + resp.statusCode() + " " + msg))
                )
                .bodyToMono(JahezDTOs.AckSuccess.class)
        );
    }

    public Mono<JahezDTOs.AckSuccess> sendRejected(long jahezOrderId, String reason) {
        var payload = new HashMap<String,Object>();
        payload.put("jahezOrderId", jahezOrderId);
        payload.put("status", "R");
        payload.put("reason", (reason == null || reason.isBlank()) ? "Rejected by operator" : reason);

        return withAuth(h -> webClient.post()
                .uri("/webhooks/status_update")
                .headers(dst -> dst.addAll(h))
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(msg -> new IllegalStateException("Jahez failed: " + resp.statusCode() + " " + msg))
                )
                .bodyToMono(JahezDTOs.AckSuccess.class)
        );
    }
}
