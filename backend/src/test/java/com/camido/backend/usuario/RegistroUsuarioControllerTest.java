package com.camido.backend.usuario;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@WebMvcTest(RegistroUsuarioController.class)
@Import(RegistroUsuarioControllerTest.RegistroTestConfig.class)
class RegistroUsuarioControllerTest {

	private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final String PASSWORD = "clave-que-no-debe-verse";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistroTestState state;

	@BeforeEach
	void reiniciar() {
		state.reiniciar();
	}

	@Test
	void registroExitosoDevuelve201YSoloElUuid() throws Exception {
		mockMvc.perform(post("/api/usuarios/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(content().string("\"" + ID + "\""));
	}

	@Test
	void emailDuplicadoDevuelve409SinDetallesInternos() throws Exception {
		state.emailExiste.set(true);

		mockMvc.perform(post("/api/usuarios/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("El email ya está registrado"))
				.andExpect(content().string(not(containsString(PASSWORD))))
				.andExpect(content().string(not(containsString("uq_usuarios"))));
	}

	@Test
	void duplicadoAlConfirmarLaTransaccionDevuelve409() throws Exception {
		state.errorAlConfirmar.set(new DataIntegrityViolationException(
				"commit failed uq_usuarios_email_lower",
				new SQLException("duplicate key value", "23505")));

		mockMvc.perform(post("/api/usuarios/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("El email ya está registrado"))
				.andExpect(content().string(not(containsString("23505"))))
				.andExpect(content().string(not(containsString("uq_usuarios"))))
				.andExpect(content().string(not(containsString(PASSWORD))));
	}

	@Test
	void datosInvalidosDevuelven400SinLaContrasena() throws Exception {
		mockMvc.perform(post("/api/usuarios/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json("abc")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Los datos de registro no son válidos"))
				.andExpect(content().string(not(containsString("abc"))));
	}

	private String json(String password) {
		return """
				{"nombre":"Ana","apellido":"Paz","email":"ana@ejemplo.com","password":"%s","fechaNacimiento":"2000-01-02"}
				""".formatted(password);
	}

	@TestConfiguration
	static class RegistroTestConfig {

		@Bean
		RegistroTestState registroTestState() {
			return new RegistroTestState();
		}

		@Bean
		RegistroUsuarioService registroUsuarioService(RegistroTestState state) {
			return new RegistroUsuarioService(
					state.repositorio(),
					state.encoder(),
					state.transacciones(),
					Mockito.mock(VerificacionEmailService.class));
		}
	}

	static final class RegistroTestState {

		final AtomicBoolean emailExiste = new AtomicBoolean();
		final AtomicReference<RuntimeException> errorAlConfirmar = new AtomicReference<>();

		void reiniciar() {
			emailExiste.set(false);
			errorAlConfirmar.set(null);
		}

		UsuarioRepository repositorio() {
			return (UsuarioRepository) Proxy.newProxyInstance(
					UsuarioRepository.class.getClassLoader(),
					new Class<?>[] { UsuarioRepository.class },
					(proxy, method, args) -> {
						if ("existsByEmailIgnoreCase".equals(method.getName())) {
							return emailExiste.get();
						}
						if ("saveAndFlush".equals(method.getName())) {
							Usuario usuario = (Usuario) args[0];
							Field id = Usuario.class.getDeclaredField("id");
							id.setAccessible(true);
							id.set(usuario, ID);
							return usuario;
						}
						if (method.getReturnType().equals(boolean.class)) {
							return false;
						}
						if (method.getReturnType().equals(int.class) || method.getReturnType().equals(long.class)) {
							return 0;
						}
						return null;
					});
		}

		PasswordEncoder encoder() {
			return new PasswordEncoder() {
				@Override
				public String encode(CharSequence rawPassword) {
					return "hash";
				}

				@Override
				public boolean matches(CharSequence rawPassword, String encodedPassword) {
					return false;
				}
			};
		}

		PlatformTransactionManager transacciones() {
			return new PlatformTransactionManager() {
				@Override
				public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
					return new SimpleTransactionStatus();
				}

				@Override
				public void commit(TransactionStatus status) throws TransactionException {
					RuntimeException error = errorAlConfirmar.get();
					if (error != null) {
						throw error;
					}
				}

				@Override
				public void rollback(TransactionStatus status) throws TransactionException {
				}
			};
		}
	}
}
