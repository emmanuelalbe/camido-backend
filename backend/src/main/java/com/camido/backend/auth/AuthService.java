package com.camido.backend.auth;

import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.camido.backend.usuario.Usuario;
import com.camido.backend.usuario.UsuarioEstado;
import com.camido.backend.usuario.UsuarioRepository;
import com.camido.backend.usuario.VerificacionEmailProperties;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final VerificacionEmailProperties verificacionEmailProperties;

	public AuthService(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder,
			VerificacionEmailProperties verificacionEmailProperties) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.verificacionEmailProperties = verificacionEmailProperties;
	}

	public UsuarioPrincipal autenticar(String email, String password) {
		String normalizado = normalizarEmail(email);
		if (normalizado.isEmpty() || password == null || password.isBlank()) {
			throw new BadCredentialsException("Credenciales inválidas");
		}
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizado).orElse(null);
		if (!acepta(usuario, password)) {
			throw new BadCredentialsException("Credenciales inválidas");
		}
		return new UsuarioPrincipal(usuario);
	}

	private boolean acepta(Usuario usuario, String password) {
		if (usuario == null || usuario.getEstado() != UsuarioEstado.ACTIVO) {
			return false;
		}
		if (verificacionEmailProperties.isObligatoria() && !usuario.isEmailVerificado()) {
			return false;
		}
		return passwordEncoder.matches(password, usuario.getPasswordHash());
	}

	public static String normalizarEmail(String email) {
		if (email == null) {
			return "";
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
