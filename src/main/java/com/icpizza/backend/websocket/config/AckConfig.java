package com.icpizza.backend.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class AckConfig {
    @Bean(name = "orderAckScheduler")
    @Primary
    public ThreadPoolTaskScheduler orderAckScheduler() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(2);
        s.setThreadNamePrefix("ws-ack-");
        s.initialize();
        return s;
    }
}
