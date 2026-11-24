package com.camido.backend.email;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Envío real vía Resend. Solo se registra si {@code camido.email.provider=resend}.
 * Nunca registra el enlace (contiene el token).
 */
public class ResendEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

	private final RestClient restClient;
	private final EmailProperties.Resend resend;

	public ResendEmailSender(RestClient.Builder restClientBuilder, EmailProperties properties) {
		this.resend = properties.getResend();
		this.restClient = restClientBuilder.build();
	}

	@Override
	public void enviarVerificacionEmail(String email, String enlaceVerificacion) {
		try {
			restClient.post()
					.uri(resend.getApiUrl())
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + resend.getApiKey())
					.header("User-Agent", "camido-backend/0.0.1")
					.body(Map.of(
							"from", resend.getFrom(),
							"to", List.of(email),
							"subject", "Verificá tu correo en Cami-Do",
							"html", htmlSinLoguear(enlaceVerificacion)))
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientResponseException ex) {
			log.error("Resend rechazó el envío de verificación (HTTP {})", ex.getStatusCode().value());
			throw new EmailEnvioException("No se pudo enviar el correo de verificación");
		}
		catch (RuntimeException ex) {
			log.error("Error al contactar Resend para verificación de email");
			throw new EmailEnvioException("No se pudo enviar el correo de verificación");
		}
	}

	private static String htmlSinLoguear(String enlace) {
		return "<p>Hola,</p>"
				+ "<p>Para verificar tu correo en Cami-Do, abrí este enlace:</p>"
				+ "<p><a href=\"" + enlace + "\">Verificar correo</a></p>"
				+ "<p>El enlace vence en 24 horas.</p>";
	}
}
