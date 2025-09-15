package com.icpizza.backend.jahez.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.icpizza.backend.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class JahezDTOs {
    public record JahezOrderCreatePayload(
            List<Item> products,
            BigDecimal final_price,
            BigDecimal price,
            String branch_id,
            String notes,
            Long jahez_id,
            PaymentMethod payment_method,
            Offer offer,
            String phone_number,
            String customer_name,
            DeliveryAddress delivery_address
    ) {
        public record Item(
                String product_id, Integer quantity,
                BigDecimal original_price, BigDecimal final_price,
                List<Modifier> modifiers, String notes
        ) {
        }

        public record Modifier(String modifier_id, List<Option> options) {
        }

        public record Option(BigDecimal original_price, String id, BigDecimal final_price, Integer quantity) {
        }

        public record Offer(BigDecimal amount, Integer type) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static final class DeliveryAddress {
            public Geolocation geolocation;
        }
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static final class Geolocation {
            public Double longitude;
            public Double latitude;
        }

        public enum PaymentMethod {
            CASH, CREDITCARD, POS, POS_MADA, POS_CREDIT_CARD,
            APPLE_PAY_MADA, APPLE_PAY_CREDIT_CARD, PAYFORT_CREDIT_CARD;

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public static PaymentMethod fromJson(String raw) {
                if (raw == null) return CREDITCARD;
                String norm = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
                try {
                    return PaymentMethod.valueOf(norm);
                } catch (IllegalArgumentException ex) {
                    return CREDITCARD;
                }
            }

            public static String toLabel(JahezDTOs.JahezOrderCreatePayload.PaymentMethod pm) {
                if (pm == null) return "Card";
                return switch (pm) {
                    case CASH -> "Cash";

                    case CREDITCARD, POS, POS_MADA, POS_CREDIT_CARD, PAYFORT_CREDIT_CARD -> "Card";

                    case APPLE_PAY_MADA, APPLE_PAY_CREDIT_CARD -> "Apple Pay";
                };
            }
        }
    }

    public record JahezOrderUpdatePayload(
            String event,
            Long jahezOrderId,
            JahezOrderCreatePayload.PaymentMethod payment_method,
            OrderStatus status
    ) {}

    public record JahezStatusUpdateRequest(
            Long jahezOrderId,
            String status,
            String reason
    ) {}

    public record DataForJahezOrder(
            String name,
            String category,
            String size
    ){}

    public record AckSuccess(boolean success) { public static AckSuccess OK(){ return new AckSuccess(true); } }
}
