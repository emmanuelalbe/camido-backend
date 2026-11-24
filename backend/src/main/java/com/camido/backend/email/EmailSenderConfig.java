package com.camido.backend.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailSenderConfig {

	@Bean
	@Profile("!prod & !production")
	@ConditionalOnProperty(name = "camido.email.provider", havingValue = "none", matchIfMissing = true)
	EmailSender noEmailSender() {
		return new NoEmailSender();
	}

	@Bean
	@ConditionalOnProperty(name = "camido.email.provider", havingValue = "resend")
	EmailSender resendEmailSender(RestClient.Builder restClientBuilder, EmailProperties properties) {
		EmailProperties.Resend resend = properties.getResend();
		if (!StringUtils.hasText(resend.getApiKey()) || !StringUtils.hasText(resend.getFrom())) {
			throw new IllegalStateException(
					"camido.email.provider=resend requiere RESEND_API_KEY y RESEND_FROM");
		}
		return new ResendEmailSender(restClientBuilder, properties);
	}

	@Bean
	EmailSenderStartupValidator emailSenderStartupValidator(Environment environment, EmailProperties properties) {
		return new EmailSenderStartupValidator(environment, properties);
	}
}
