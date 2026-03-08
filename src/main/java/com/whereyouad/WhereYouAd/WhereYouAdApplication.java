package com.whereyouad.WhereYouAd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableAsync
public class WhereYouAdApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhereYouAdApplication.class, args);
	}

}
