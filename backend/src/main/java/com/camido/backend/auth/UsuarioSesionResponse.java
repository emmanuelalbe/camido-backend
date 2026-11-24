package com.camido.backend.auth;

import java.util.UUID;

public record UsuarioSesionResponse(
		UUID id,
		String nombre,
		String apellido,
		String email,
		boolean emailVerificado) {

	public static UsuarioSesionResponse desde(UsuarioPrincipal principal) {
		return new UsuarioSesionResponse(
				principal.getId(),
				principal.getNombre(),
				principal.getApellido(),
				principal.getEmail(),
				principal.isEmailVerificado());
	}
}
