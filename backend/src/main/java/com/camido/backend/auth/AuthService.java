package com.camido.backend.auth;

import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.camido.backend.usuario.Usuario;
import com.camido.backend.usuario.UsuarioEstado;
import com.camido.backend.usuario.UsuarioRepository;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
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
		if (usuario == null || usuario.getEstado() != UsuarioEstado.ACTIVO || !usuario.isEmailVerificado()) {
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
