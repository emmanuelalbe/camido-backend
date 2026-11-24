package com.camido.backend.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.camido.backend.usuario.VerificacionEmailProperties;

@Configuration
@EnableConfigurationProperties(VerificacionEmailProperties.class)
public class VerificacionEmailConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
