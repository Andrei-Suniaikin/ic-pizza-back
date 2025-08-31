package com.icpizza.backend.jahez.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import com.icpizza.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ic-pizza-bh/jahez-integration")
public class JahezWebhookController {
    private static final Logger log = LoggerFactory.getLogger(JahezWebhookController.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @PostMapping("/webhooks/order_create_endpoint")
    public ResponseEntity<JahezDTOs.AckSuccess> create(@RequestBody JsonNode body) {
        System.out.println(body);
        try {
            var payload = objectMapper.treeToValue(body, JahezDTOs.JahezOrderCreatePayload.class);
            orderService.createJahezOrder(payload); // асинхронно: сохранение, уведомление, дальнейшие действия
        } catch (Exception e) {
            log.error("Jahez create parse error", e); // но ACK всё равно даём
        }
        return ResponseEntity.ok(JahezDTOs.AckSuccess.OK());
    }

    @PostMapping("/webhooks/order_update_endpoint")
    public ResponseEntity<Void> update(@RequestBody JsonNode body) {

        try {
            var payload = objectMapper.treeToValue(body, JahezDTOs.JahezOrderUpdatePayload.class);
            orderService.updateJahezOrderStatus(payload); // асинхронно: обновить статус/оплату, оповестить фронт
        } catch (Exception e) {
            log.error("Jahez update parse error", e);
        }
        return ResponseEntity.ok().build();
    }
}
