package com.icpizza.backend.whatsapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppApiProperties(
        String accessToken,
        String phoneNumberId,
        String version,
        String kitchenPhones
) {
}
