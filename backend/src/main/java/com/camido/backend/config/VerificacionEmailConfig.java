package com.camido.backend.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.camido.backend.usuario.VerificacionEmailProperties;
import com.camido.backend.usuario.VerificacionEmailStartupValidator;

@Configuration
@EnableConfigurationProperties(VerificacionEmailProperties.class)
public class VerificacionEmailConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	VerificacionEmailStartupValidator verificacionEmailStartupValidator(
			Environment environment,
			VerificacionEmailProperties properties) {
		return new VerificacionEmailStartupValidator(environment, properties);
	}
}
