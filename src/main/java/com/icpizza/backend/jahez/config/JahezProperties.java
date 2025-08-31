package com.icpizza.backend.jahez.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jahez")
public record JahezProperties(
        String baseUrl,
        String secret,
        String apiKey,
        @DefaultValue("60") long clockSkewSeconds,
        @DefaultValue("3000") int connectTimeoutMs,
        @DefaultValue("5000") int readTimeoutMs

) {}
