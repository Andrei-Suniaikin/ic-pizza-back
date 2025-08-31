package com.icpizza.backend.tiktok.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TikTokService {
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

    @Value("${tiktok.access-token}")
    private String accessToken;

    @Value("${tiktok.pixel-code}")
    private String pixelCode;

    @Value("${tiktok.tiktok-endpoint}")
    private String endpoint;

    private final RestClient client = RestClient.create();

    public boolean sendPlaceAnOrder(String phoneNumber, BigDecimal valueBhd) {
        try {
            String phone = phoneNumber != null && phoneNumber.startsWith("+")
                    ? phoneNumber
                    : (phoneNumber == null ? "" : "+" + phoneNumber);

            Map<String, Object> user = new HashMap<>();
            user.put("phone", phone);

            Map<String, Object> props = new HashMap<>();
            props.put("currency", "BHD");
            props.put("content_type", "product");
            props.put("value", valueBhd != null ? valueBhd : BigDecimal.ZERO);

            Map<String, Object> dataItem = new HashMap<>();
            dataItem.put("event", "PlaceAnOrder");
            dataItem.put("event_time", Instant.now(Clock.system(BAHRAIN)).getEpochSecond());
            dataItem.put("user", user);
            dataItem.put("properties", props);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event_source", "web");
            payload.put("event_source_id", pixelCode);
            payload.put("data", List.of(dataItem));

            var response = client.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Access-Token", accessToken)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);

            log.info("[TikTok] status={}, body={}", response.getStatusCode(), response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("[TikTok] error sending event", e);
            return false;
        }
    }

}
