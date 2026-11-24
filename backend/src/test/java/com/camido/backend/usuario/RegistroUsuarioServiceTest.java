package com.camido.backend.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.camido.backend.categoria.Categoria;
import com.camido.backend.categoria.CategoriaInvalidaException;
import com.camido.backend.categoria.CategoriaRepository;
import com.camido.backend.categoria.DeclaracionCategoria;
import com.camido.backend.categoria.DeclaracionCategoriaRepository;
import com.camido.backend.categoria.DeclaracionEstado;
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
	private final AtomicBoolean confirmo = new AtomicBoolean();

	private CategoriaRepository categorias;
	private DeclaracionCategoriaRepository declaraciones;
	private RegistroUsuarioService service;

	@BeforeEach
	void setUp() {
		emailExiste.set(false);
		guardado.set(null);
		errorAlGuardar.set(null);
		errorAlConfirmar.set(null);
		guardo.set(false);
		confirmo.set(false);
		categorias = Mockito.mock(CategoriaRepository.class);
		declaraciones = Mockito.mock(DeclaracionCategoriaRepository.class);
		VerificacionEmailService verificacion = Mockito.mock(VerificacionEmailService.class);
		service = new RegistroUsuarioService(
				repositorio(), encoder(), transacciones(), verificacion, categorias, declaraciones);
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
		verify(declaraciones, never()).save(any());
	}

	@Test
	void guardaLaCategoriaComoDeclaracionSinReconocerlaComoOficial() {
		UUID categoriaId = UUID.randomUUID();
		Categoria categoria = categoria(categoriaId);
		when(categorias.findByIdAndActivoTrue(categoriaId)).thenReturn(Optional.of(categoria));
		RegistroUsuarioRequest request = solicitud();
		request.setIntencion(IntencionIncorporacion.ALUMNO);
		request.setCategoriaId(categoriaId);

		service.registrar(request);

		ArgumentCaptor<DeclaracionCategoria> captor = ArgumentCaptor.forClass(DeclaracionCategoria.class);
		verify(declaraciones).save(captor.capture());
		DeclaracionCategoria declaracion = captor.getValue();
		assertEquals(DeclaracionEstado.DECLARADA, declaracion.getEstado());
		assertEquals(categoria, declaracion.getCategoria());
		assertEquals(guardado.get(), declaracion.getUsuario());
		assertFalse(guardado.get().isEmailVerificado());
	}

	@Test
	void alumnoSinCategoriaNoSeRegistra() {
		RegistroUsuarioRequest request = solicitud();
		request.setIntencion(IntencionIncorporacion.ALUMNO);

		assertThrows(CategoriaInvalidaException.class, () -> service.registrar(request));
		assertFalse(guardo.get());
		verify(declaraciones, never()).save(any());
	}

	@Test
	void profesorSinCategoriaNoSeRegistra() {
		RegistroUsuarioRequest request = solicitud();
		request.setIntencion(IntencionIncorporacion.PROFESOR);

		assertThrows(CategoriaInvalidaException.class, () -> service.registrar(request));
		assertFalse(guardo.get());
		verify(declaraciones, never()).save(any());
	}

	@Test
	void organizadorPuedeRegistrarseSinCategoriaYSinRol() {
		service.registrar(solicitud());

		assertEquals(UsuarioEstado.ACTIVO, guardado.get().getEstado());
		verify(declaraciones, never()).save(any());
	}

	@Test
	void organizadorConCategoriaQuedaSoloComoDeclaracion() {
		UUID categoriaId = UUID.randomUUID();
		when(categorias.findByIdAndActivoTrue(categoriaId)).thenReturn(Optional.of(categoria(categoriaId)));
		RegistroUsuarioRequest request = solicitud();
		request.setCategoriaId(categoriaId);

		service.registrar(request);

		ArgumentCaptor<DeclaracionCategoria> captor = ArgumentCaptor.forClass(DeclaracionCategoria.class);
		verify(declaraciones).save(captor.capture());
		assertEquals(DeclaracionEstado.DECLARADA, captor.getValue().getEstado());
	}

	@Test
	void profesorConCategoriaValidaQuedaComoDeclaracion() {
		UUID categoriaId = UUID.randomUUID();
		when(categorias.findByIdAndActivoTrue(categoriaId)).thenReturn(Optional.of(categoria(categoriaId)));
		RegistroUsuarioRequest request = solicitud();
		request.setIntencion(IntencionIncorporacion.PROFESOR);
		request.setCategoriaId(categoriaId);

		service.registrar(request);

		ArgumentCaptor<DeclaracionCategoria> captor = ArgumentCaptor.forClass(DeclaracionCategoria.class);
		verify(declaraciones).save(captor.capture());
		assertEquals(DeclaracionEstado.DECLARADA, captor.getValue().getEstado());
		assertFalse(guardado.get().isEmailVerificado());
	}

	@Test
	void rechazaCategoriaInexistenteSinGuardarUsuario() {
		UUID categoriaId = UUID.randomUUID();
		when(categorias.findByIdAndActivoTrue(categoriaId)).thenReturn(Optional.empty());
		RegistroUsuarioRequest request = solicitud();
		request.setCategoriaId(categoriaId);

		assertThrows(CategoriaInvalidaException.class, () -> service.registrar(request));
		assertFalse(guardo.get());
		assertFalse(confirmo.get());
		verify(declaraciones, never()).save(any());
	}

	@Test
	void siFallaLaDeclaracionNoConfirmaElRegistro() {
		UUID categoriaId = UUID.randomUUID();
		when(categorias.findByIdAndActivoTrue(categoriaId)).thenReturn(Optional.of(categoria(categoriaId)));
		when(declaraciones.save(any())).thenThrow(new RuntimeException("fallo de declaracion"));
		RegistroUsuarioRequest request = solicitud();
		request.setCategoriaId(categoriaId);

		assertThrows(RuntimeException.class, () -> service.registrar(request));
		assertFalse(confirmo.get());
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
		request.setIntencion(IntencionIncorporacion.ORGANIZADOR);
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
				confirmo.set(true);
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

	private Categoria categoria(UUID id) {
		try {
			Constructor<Categoria> constructor = Categoria.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			Categoria categoria = constructor.newInstance();
			Field field = Categoria.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(categoria, id);
			return categoria;
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static final class InvocationHandlerInvoke {
		private static Object invokeDefault(Object proxy, java.lang.reflect.Method method, Object[] args)
				throws Throwable {
			return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
		}
	}
}
