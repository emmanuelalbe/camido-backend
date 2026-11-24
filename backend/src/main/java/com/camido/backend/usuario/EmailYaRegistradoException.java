package com.camido.backend.usuario;

/**
 * El email ya existe según la comprobación previa o el índice único LOWER(email).
 * El mensaje no incluye la contraseña ni el hash.
 */
public class EmailYaRegistradoException extends RuntimeException {

	public EmailYaRegistradoException() {
		super("El email ya está registrado");
	}
}
