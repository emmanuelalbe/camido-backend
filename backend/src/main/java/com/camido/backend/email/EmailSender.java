package com.camido.backend.email;

/**
 * Contrato de envío de correos. Una implementación real debe configurarse
 * explícitamente; sin ella el backend emite tokens pero no envía mensajes.
 */
public interface EmailSender {

	/**
	 * @param email dirección del destinatario
	 * @param enlaceVerificacion URL completa que incluye el token en claro
	 */
	void enviarVerificacionEmail(String email, String enlaceVerificacion);
}
