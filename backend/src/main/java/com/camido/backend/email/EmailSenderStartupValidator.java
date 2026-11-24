package com.camido.backend.email;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

/**
 * Impide que prod arranque con el fallback NoEmailSender como si hubiera correo real.
 */
public class EmailSenderStartupValidator implements ApplicationRunner {

	private final Environment environment;
	private final EmailProperties properties;

	public EmailSenderStartupValidator(Environment environment, EmailProperties properties) {
		this.environment = environment;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		boolean produccion = environment.acceptsProfiles(Profiles.of("prod", "production"));
		String provider = properties.getProvider() == null ? "none" : properties.getProvider().trim();

		if (produccion && "none".equalsIgnoreCase(provider)) {
			throw new IllegalStateException(
					"En producción camido.email.provider no puede ser 'none'. Configurá Resend u otro proveedor.");
		}

		if ("resend".equalsIgnoreCase(provider)) {
			EmailProperties.Resend resend = properties.getResend();
			if (!StringUtils.hasText(resend.getApiKey()) || !StringUtils.hasText(resend.getFrom())) {
				throw new IllegalStateException(
						"Proveedor Resend incompleto: faltan RESEND_API_KEY y/o RESEND_FROM");
			}
		}
	}
}
