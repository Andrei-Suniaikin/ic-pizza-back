package com.icpizza.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    public static final ZoneId BAHRAIN_TZ = ZoneId.of("Asia/Bahrain");
}
