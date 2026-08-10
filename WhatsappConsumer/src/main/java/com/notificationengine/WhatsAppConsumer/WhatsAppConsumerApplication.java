package com.notificationengine.WhatsAppConsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {
		"com.notificationengine.common.model"
})
@EnableJpaRepositories(basePackages = {
		"com.notificationengine.common.repo"
})
public class WhatsAppConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatsAppConsumerApplication.class, args);
	}

}
