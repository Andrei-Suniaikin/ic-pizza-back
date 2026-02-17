package com.icpizza.backend.service;

import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.repository.CustomerRepository;
import com.icpizza.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static java.security.MessageDigest.getInstance;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaService {
    private final CustomerRepository customerRepo;
    private final OrderRepository orderRepo;
    @Value("${meta.pixel-id}")
    private String pixelId;

    @Value("${meta.secret}")
    private String accessToken;

    private final RestClient restClient = RestClient.create();

    @Async
    @Transactional(readOnly = true)
    public void sendPurchaseEvent(Long orderId, String customerNo) {
        try {
            log.info("Sending purchase event to Meta");
            Customer customer = customerRepo.findByTelephoneNoWithoutLock(customerNo)
                    .orElseThrow(() -> new RuntimeException("Customer with telephone number " + customerNo + " not found"));

            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order with id " + orderId + " not found"));

            Map<String, Object> payload = new HashMap<>();
            payload.put("data", List.of(createEventData(order, customer)));

            payload.put("test_event_code", "TEST58503");

            String apiVersion = "v24.0";
            String url = String.format("https://graph.facebook.com/%s/%s/events?access_token=%s", apiVersion, pixelId, accessToken);

            restClient.post()
                    .uri(url)
                    .body(payload)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Purchase event to Meta was sent.");
        }
        catch (Exception e) {
            log.error("Error while sending purchase event to Meta: " + e.getMessage());
        }
    }

    private Map<String, Object> createEventData(Order order, Customer customer) {
        Map<String, Object> event = new HashMap<>();
        event.put("event_name", "Purchase");
        event.put("event_time", System.currentTimeMillis()/1000);
        event.put("action_source", "website");

        Map<String, List<String>> userData = new HashMap<>();
        userData.put("ph", List.of(hash(customer.getTelephoneNo())));
        userData.put("ct", List.of(hash("Bahrain")));
        userData.put("fn", List.of(hash(customer.getName())));

        event.put("user_data", userData);

        Map<String, Object> customData = new HashMap<>();
        customData.put("currency", "BHD");
        customData.put("value", String.valueOf(order.getAmountPaid().setScale(2, RoundingMode.HALF_UP)));
        customData.put("order_id", order.getId().toString());
        event.put("custom_data", customData);

        Map<String, Object> originalEventData = new HashMap<>();
        originalEventData.put("event_name", "Purchase");
        originalEventData.put("event_time", System.currentTimeMillis()/1000);
        originalEventData.put("order_id", order.getId().toString());
        event.put("original_event_data", originalEventData);

        return event;
    }

    private String hash(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            String cleanInput = input.trim().toLowerCase();
            MessageDigest md = getInstance("SHA-256");
            byte[] digest = md.digest(cleanInput.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return "";
        }
    }
}
