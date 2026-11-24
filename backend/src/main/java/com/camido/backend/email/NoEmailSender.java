package com.camido.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback cuando no hay proveedor real. No simula un envío exitoso:
 * deja constancia de que el correo no se envió. No registra el enlace
 * (contiene el token).
 */
public class NoEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(NoEmailSender.class);

	@Override
	public void enviarVerificacionEmail(String email, String enlaceVerificacion) {
		log.warn(
				"Verificación de email: no hay proveedor de correo configurado; "
						+ "el token puede haberse emitido pero el mensaje no se envió");
	}
}
