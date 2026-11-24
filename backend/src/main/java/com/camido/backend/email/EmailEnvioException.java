package com.camido.backend.email;

/**
 * Fallo de envío. El mensaje nunca debe incluir tokens ni contraseñas.
 */
public class EmailEnvioException extends RuntimeException {

	public EmailEnvioException(String message) {
		super(message);
	}
}
