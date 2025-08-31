package com.icpizza.backend;

import com.icpizza.backend.jahez.config.JahezProperties;
import com.icpizza.backend.whatsapp.config.WhatsAppApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties({JahezProperties.class, WhatsAppApiProperties.class})
@EnableCaching
@EnableAsync
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
