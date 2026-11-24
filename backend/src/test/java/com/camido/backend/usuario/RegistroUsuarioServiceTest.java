package com.camido.backend.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class RegistroUsuarioServiceTest {

	private final AtomicBoolean emailExiste = new AtomicBoolean();
	private final AtomicReference<Usuario> guardado = new AtomicReference<>();
	private final AtomicReference<RuntimeException> errorAlGuardar = new AtomicReference<>();
	private final AtomicReference<RuntimeException> errorAlConfirmar = new AtomicReference<>();
	private final AtomicBoolean guardo = new AtomicBoolean();

	private RegistroUsuarioService service;

	@BeforeEach
	void setUp() {
		emailExiste.set(false);
		guardado.set(null);
		errorAlGuardar.set(null);
		errorAlConfirmar.set(null);
		guardo.set(false);
		VerificacionEmailService verificacion = Mockito.mock(VerificacionEmailService.class);
		service = new RegistroUsuarioService(repositorio(), encoder(), transacciones(), verificacion);
	}

	@Test
	void registraUsuarioActivoSinCorreoVerificadoYSinGuardarLaContrasena() {
		UUID id = service.registrar(solicitud());

		Usuario usuario = guardado.get();
		assertEquals(id, usuario.getId());
		assertEquals(UsuarioEstado.ACTIVO, usuario.getEstado());
		assertFalse(usuario.isEmailVerificado());
		assertEquals("hash:secreto12", usuario.getPasswordHash());
		assertNotEquals("secreto12", usuario.getPasswordHash());
		assertTrue(guardo.get());
	}

	@Test
	void rechazaEmailExistenteSinGuardar() {
		emailExiste.set(true);

		assertThrows(EmailYaRegistradoException.class, () -> service.registrar(solicitud()));
		assertFalse(guardo.get());
	}

	@Test
	void traduceDuplicadoDetectadoAlHacerFlush() {
		errorAlGuardar.set(new DataIntegrityViolationException("duplicate key uq_usuarios_email_lower"));

		assertThrows(EmailYaRegistradoException.class, () -> service.registrar(solicitud()));
	}

	@Test
	void traduceDuplicadoDetectadoAlConfirmarLaTransaccion() {
		errorAlConfirmar.set(new DataIntegrityViolationException(
				"could not commit", new SQLException("duplicate key", "23505")));

		assertThrows(EmailYaRegistradoException.class, () -> service.registrar(solicitud()));
	}

	private RegistroUsuarioRequest solicitud() {
		RegistroUsuarioRequest request = new RegistroUsuarioRequest();
		request.setNombre("Ana");
		request.setApellido("Paz");
		request.setEmail("Ana@Ejemplo.com");
		request.setPassword("secreto12");
		request.setFechaNacimiento(LocalDate.of(2000, 1, 2));
		return request;
	}

	private PasswordEncoder encoder() {
		return new PasswordEncoder() {
			@Override
			public String encode(CharSequence rawPassword) {
				return "hash:" + rawPassword;
			}

			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword) {
				return false;
			}
		};
	}

	private PlatformTransactionManager transacciones() {
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

	private UsuarioRepository repositorio() {
		return (UsuarioRepository) Proxy.newProxyInstance(
				UsuarioRepository.class.getClassLoader(),
				new Class<?>[] { UsuarioRepository.class },
				(proxy, method, args) -> {
					if ("existsByEmailIgnoreCase".equals(method.getName())) {
						return emailExiste.get();
					}
					if ("saveAndFlush".equals(method.getName())) {
						guardo.set(true);
						RuntimeException error = errorAlGuardar.get();
						if (error != null) {
							throw error;
						}
						Usuario usuario = (Usuario) args[0];
						Field id = Usuario.class.getDeclaredField("id");
						id.setAccessible(true);
						id.set(usuario, UUID.randomUUID());
						guardado.set(usuario);
						return usuario;
					}
					if (method.isDefault()) {
						return InvocationHandlerInvoke.invokeDefault(proxy, method, args);
					}
					Class<?> tipo = method.getReturnType();
					if (tipo.equals(boolean.class)) {
						return false;
					}
					if (tipo.equals(int.class) || tipo.equals(long.class)) {
						return 0;
					}
					return null;
				});
	}

	private static final class InvocationHandlerInvoke {
		private static Object invokeDefault(Object proxy, java.lang.reflect.Method method, Object[] args)
				throws Throwable {
			return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
		}
	}
}
