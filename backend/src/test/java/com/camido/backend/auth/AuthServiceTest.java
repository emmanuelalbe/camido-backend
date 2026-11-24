package com.camido.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.camido.backend.usuario.Usuario;
import com.camido.backend.usuario.UsuarioEstado;
import com.camido.backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String EMAIL = "ana@example.com";
	private static final String PASSWORD = "clave-segura-1";

	@Mock
	private UsuarioRepository usuarioRepository;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(usuarioRepository, passwordEncoder);
	}

	@Test
	void loginCorrecto() {
		when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario(true, UsuarioEstado.ACTIVO)));
		UsuarioPrincipal principal = authService.autenticar("  ANA@example.com ", PASSWORD);
		assertEquals(EMAIL, principal.getEmail());
	}

	@Test
	void passwordIncorrecta() {
		when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario(true, UsuarioEstado.ACTIVO)));
		assertThrows(BadCredentialsException.class, () -> authService.autenticar(EMAIL, "otra-clave"));
	}

	@Test
	void usuarioInexistente() {
		when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
		assertThrows(BadCredentialsException.class, () -> authService.autenticar(EMAIL, PASSWORD));
	}

	@Test
	void emailNoVerificado() {
		when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario(false, UsuarioEstado.ACTIVO)));
		assertThrows(BadCredentialsException.class, () -> authService.autenticar(EMAIL, PASSWORD));
	}

	@Test
	void cuentaInactivaOSuspendida() {
		when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario(true, UsuarioEstado.INACTIVO)));
		assertThrows(BadCredentialsException.class, () -> authService.autenticar(EMAIL, PASSWORD));
		when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario(true, UsuarioEstado.SUSPENDIDO)));
		assertThrows(BadCredentialsException.class, () -> authService.autenticar(EMAIL, PASSWORD));
	}

	private Usuario usuario(boolean verificado, UsuarioEstado estado) {
		Usuario usuario = nuevoUsuario();
		usuario.setNombre("Ana");
		usuario.setApellido("Paz");
		usuario.setEmail(EMAIL);
		usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
		usuario.setEmailVerificado(verificado);
		usuario.setEstado(estado);
		return usuario;
	}

	private static Usuario nuevoUsuario() {
		try {
			Constructor<Usuario> constructor = Usuario.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
