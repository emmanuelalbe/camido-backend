package com.camido.backend.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.camido.backend.email.EmailSender;

/**
 * Integración contra PostgreSQL real (variables DB_*).
 * Crea y elimina únicamente el usuario/token de esta prueba.
 */
@SpringBootTest
@Import(VerificacionEmailIntegrationTest.CapturaEmailConfig.class)
class VerificacionEmailIntegrationTest {

	private static final String EMAIL =
			"e2e.verificacion." + System.currentTimeMillis() + "@example.com";

	@Autowired
	private RegistroUsuarioService registroUsuarioService;

	@Autowired
	private VerificacionEmailService verificacionEmailService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private TokenVerificacionEmailRepository tokenRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AtomicReference<String> ultimoEnlace;

	@MockitoBean
	private EmailSender emailSender;

	private UUID usuarioId;

	@BeforeEach
	void requiereBase() {
		assumeTrue(System.getenv("DB_HOST") != null && !System.getenv("DB_HOST").isBlank(),
				"DB_* no configuradas; se omite la integración");

		doAnswer(inv -> {
			ultimoEnlace.set(inv.getArgument(1));
			return null;
		}).when(emailSender).enviarVerificacionEmail(anyString(), anyString());

		RegistroUsuarioRequest request = new RegistroUsuarioRequest();
		request.setNombre("Integra");
		request.setApellido("Verificacion");
		request.setEmail(EMAIL);
		request.setPassword("Prueba-Integ-2026");
		request.setFechaNacimiento(LocalDate.of(2000, 1, 2));
		usuarioId = registroUsuarioService.registrar(request);
	}

	@AfterEach
	void limpiarSoloDatosDeEstaPrueba() {
		if (usuarioId == null) {
			return;
		}
		jdbcTemplate.update("DELETE FROM tokens_verificacion_email WHERE usuario_id = ?", usuarioId);
		jdbcTemplate.update("DELETE FROM usuarios WHERE id = ?", usuarioId);
	}

	@Test
	void emitirVerificarYReenvioSobrePostgres() {
		Optional<Usuario> creado = usuarioRepository.findById(usuarioId);
		assertTrue(creado.isPresent());
		assertFalse(creado.get().isEmailVerificado());

		assertTrue(tokenRepository.findPendienteByUsuarioId(usuarioId).isPresent());
		String enlace = ultimoEnlace.get();
		assertTrue(enlace != null && enlace.contains("token="));
		String token = enlace.substring(enlace.indexOf("token=") + 6);

		verificacionEmailService.verificar(token);

		Usuario verificado = usuarioRepository.findById(usuarioId).orElseThrow();
		assertTrue(verificado.isEmailVerificado());
		assertTrue(tokenRepository.findPendienteByUsuarioId(usuarioId).isEmpty());

		ultimoEnlace.set(null);
		verificacionEmailService.reenviar(EMAIL);
		assertEquals(null, ultimoEnlace.get(), "usuario ya verificado: no debe reenviar");
	}

	static class CapturaEmailConfig {
		@Bean
		@Primary
		AtomicReference<String> ultimoEnlace() {
			return new AtomicReference<>();
		}
	}
}
