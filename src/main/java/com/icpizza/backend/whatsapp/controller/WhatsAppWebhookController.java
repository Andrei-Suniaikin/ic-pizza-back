package com.icpizza.backend.whatsapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.service.CustomerService;
import com.icpizza.backend.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/whatsapp/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {
    private final WhatsAppService wa;
    private final CustomerService customerService;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> handleWebhook(@RequestBody JsonNode body) {
        try {
            if (body == null || body.isNull()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid request, JSON data required"));
            }

            JsonNode value = body.path("entry").path(0).path("changes").path(0).path("value");
            if (!value.isObject()) {
                return ResponseEntity.ok(Map.of("status", "success"));
            }

            if (value.has("messages")) {
                JsonNode msg = value.path("messages").path(0);
                String senderPhone = value.path("contacts").path(0).path("wa_id").asText(null);
                String messageText = msg.path("text").path("body").asText("").trim();

                if (senderPhone == null || senderPhone.isBlank()) {
                    log.warn("Webhook: no wa_id in payload");
                    return ResponseEntity.ok(Map.of("status", "ignored"));
                }

                Optional<Customer> customer = customerService.findCustomer(senderPhone);
                if (customer.isEmpty()) {
                    Customer newCustomer = customerService.createWatsappCustomer(senderPhone);
                    wa.askForName(senderPhone);
                    return ResponseEntity.ok(Map.of("status", "asked for name"));
                }

                if (customerService.isWaitForName(senderPhone) && !customerService.userHasName(senderPhone)) {
                    Customer customerWithName = customerService.saveUserName(senderPhone, messageText);
                    wa.sendMenuUtility(senderPhone, customerWithName.getId());
                    return ResponseEntity.ok(Map.of("status", "name saved, menu sent"));
                }

                if (!customerService.userHasName(senderPhone)) {
                    wa.askForName(senderPhone);
                    customerService.setWaitForName(senderPhone, true);
                    return ResponseEntity.ok(Map.of("status", "asked for name again"));
                }

                wa.sendMenuUtility(senderPhone, customer.get().getId());
                return ResponseEntity.ok(Map.of("status", "menu sent"));
            }

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Webhook error", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.challenge", required = false) String challenge,
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken
    ) {
        return ResponseEntity.ok(challenge != null ? challenge : "");
    }
}
