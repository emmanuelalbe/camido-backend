package com.camido.backend.usuario;

public class TokenVerificacionInvalidoException extends RuntimeException {

	public TokenVerificacionInvalidoException() {
		super("El enlace es inválido o ha vencido");
	}
}
