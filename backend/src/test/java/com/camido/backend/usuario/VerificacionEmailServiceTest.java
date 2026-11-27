package com.camido.backend.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.camido.backend.email.EmailEnvioException;
import com.camido.backend.email.EmailSender;

class VerificacionEmailServiceTest {

	private static final Instant AHORA = Instant.parse("2026-11-23T15:00:00Z");
	private static final UUID USUARIO_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

	private TokenVerificacionEmailRepository tokenRepository;
	private UsuarioRepository usuarioRepository;
	private CapturingEmailSender emailSender;
	private VerificacionEmailService service;
	private Usuario usuario;

	@BeforeEach
	void setUp() throws Exception {
		tokenRepository = Mockito.mock(TokenVerificacionEmailRepository.class);
		usuarioRepository = Mockito.mock(UsuarioRepository.class);
		emailSender = new CapturingEmailSender();
		VerificacionEmailProperties properties = new VerificacionEmailProperties();
		properties.setUrlBase("http://localhost:4200/verificar-email");
		Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
		service = new VerificacionEmailService(
				tokenRepository, usuarioRepository, emailSender, properties, clock);

		usuario = new Usuario();
		Field id = Usuario.class.getDeclaredField("id");
		id.setAccessible(true);
		id.set(usuario, USUARIO_ID);
		usuario.setEmail("ana@ejemplo.com");
		usuario.setEstado(UsuarioEstado.ACTIVO);
		usuario.setEmailVerificado(false);
		when(usuarioRepository.findByIdForUpdate(USUARIO_ID)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.findByEmailIgnoreCaseForUpdate("ana@ejemplo.com"))
				.thenReturn(Optional.of(usuario));
	}

	@Test
	void emiteTokenConSha256YEnviaEnlaceSinPersistirElTokenEnClaro() {
		when(tokenRepository.invalidarPendientes(eq(USUARIO_ID), eq(AHORA))).thenReturn(0);
		when(tokenRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

		service.emitirParaUsuario(USUARIO_ID);

		ArgumentCaptor<TokenVerificacionEmail> captor = ArgumentCaptor.forClass(TokenVerificacionEmail.class);
		verify(tokenRepository).saveAndFlush(captor.capture());
		TokenVerificacionEmail guardado = captor.getValue();

		assertEquals(64, guardado.getTokenHash().length());
		assertEquals(AHORA.plusSeconds(24 * 3600), guardado.getVenceEn());
		assertEquals(1, emailSender.enlaces.size());
		assertTrue(emailSender.enlaces.get(0).startsWith("http://localhost:4200/verificar-email?token="));
		String tokenEnClaro = emailSender.enlaces.get(0).substring(emailSender.enlaces.get(0).indexOf("token=") + 6);
		assertEquals(TokenVerificacionEmailSupport.sha256Hex(tokenEnClaro), guardado.getTokenHash());
		assertFalse(guardado.getTokenHash().equals(tokenEnClaro));
	}

	@Test
	void verificarMarcaUsadoYEmailVerificadoEnLaMismaOperacion() {
		String token = TokenVerificacionEmailSupport.generarTokenEnClaro();
		String hash = TokenVerificacionEmailSupport.sha256Hex(token);
		TokenVerificacionEmail entidad = tokenPendiente(hash);

		when(tokenRepository.marcarUsadoSiValido(hash, AHORA)).thenReturn(1);
		when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(entidad));
		when(usuarioRepository.marcarEmailVerificado(USUARIO_ID)).thenReturn(1);

		service.verificar(token);

		verify(tokenRepository).marcarUsadoSiValido(hash, AHORA);
		verify(usuarioRepository).marcarEmailVerificado(USUARIO_ID);
	}

	@Test
	void tokenVencidoOInvalidoLanzaErrorGenerico() {
		when(tokenRepository.marcarUsadoSiValido(any(), eq(AHORA))).thenReturn(0);

		assertThrows(TokenVerificacionInvalidoException.class, () -> service.verificar("token-falso"));
		verify(usuarioRepository, never()).marcarEmailVerificado(any());
	}

	@Test
	void reutilizarTokenYaUsadoFalla() {
		when(tokenRepository.marcarUsadoSiValido(any(), eq(AHORA))).thenReturn(0);

		assertThrows(TokenVerificacionInvalidoException.class,
				() -> service.verificar(TokenVerificacionEmailSupport.generarTokenEnClaro()));
	}

	@Test
	void reenvioCreaNuevoTokenInvalidandoElAnterior() {
		when(tokenRepository.countByUsuarioIdAndCreadoEnGreaterThanEqual(eq(USUARIO_ID), any())).thenReturn(0L);
		when(tokenRepository.invalidarPendientes(eq(USUARIO_ID), eq(AHORA))).thenReturn(1);
		when(tokenRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

		service.reenviar("ana@ejemplo.com");

		verify(tokenRepository).invalidarPendientes(USUARIO_ID, AHORA);
		verify(tokenRepository).saveAndFlush(any());
		assertEquals(1, emailSender.enlaces.size());
	}

	@Test
	void reenvioConEmailDesconocidoNoEnviaNiFalla() {
		when(usuarioRepository.findByEmailIgnoreCaseForUpdate("ghost@ejemplo.com")).thenReturn(Optional.empty());

		service.reenviar("ghost@ejemplo.com");

		verify(tokenRepository, never()).saveAndFlush(any());
		assertTrue(emailSender.enlaces.isEmpty());
	}

	@Test
	void limiteDeDosMinutosImpideNuevoToken() {
		when(tokenRepository.countByUsuarioIdAndCreadoEnGreaterThanEqual(
				eq(USUARIO_ID), eq(AHORA.minus(VerificacionEmailService.INTERVALO_MINIMO_REENVIO))))
				.thenReturn(1L);

		service.reenviar("ana@ejemplo.com");

		verify(tokenRepository, never()).saveAndFlush(any());
		assertTrue(emailSender.enlaces.isEmpty());
	}

	@Test
	void limiteDeCincoPorHoraImpideNuevoToken() {
		when(tokenRepository.countByUsuarioIdAndCreadoEnGreaterThanEqual(
				eq(USUARIO_ID), eq(AHORA.minus(VerificacionEmailService.INTERVALO_MINIMO_REENVIO))))
				.thenReturn(0L);
		when(tokenRepository.countByUsuarioIdAndCreadoEnGreaterThanEqual(
				eq(USUARIO_ID), eq(AHORA.minus(VerificacionEmailService.VENTANA_REENVIO_HORA))))
				.thenReturn(5L);

		service.reenviar("ana@ejemplo.com");

		verify(tokenRepository, never()).saveAndFlush(any());
		assertTrue(emailSender.enlaces.isEmpty());
	}

	@Test
	void pausaNoEmiteTokenNiEnviaCorreo() {
		service = servicioConVerificacion(false);

		service.emitirParaUsuario(USUARIO_ID);

		verify(tokenRepository, never()).saveAndFlush(any());
		verify(usuarioRepository, never()).findByIdForUpdate(any());
		assertTrue(emailSender.enlaces.isEmpty());
	}

	@Test
	void pausaNoReenviaNiModificaTokens() {
		service = servicioConVerificacion(false);

		service.reenviar("ana@ejemplo.com");

		verify(tokenRepository, never()).saveAndFlush(any());
		verify(usuarioRepository, never()).findByEmailIgnoreCaseForUpdate(any());
		assertTrue(emailSender.enlaces.isEmpty());
	}

	@Test
	void pausaConservaLaVerificacionDeUnTokenExistente() {
		service = servicioConVerificacion(false);
		String token = TokenVerificacionEmailSupport.generarTokenEnClaro();
		String hash = TokenVerificacionEmailSupport.sha256Hex(token);
		when(tokenRepository.marcarUsadoSiValido(hash, AHORA)).thenReturn(1);
		when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(tokenPendiente(hash)));
		when(usuarioRepository.marcarEmailVerificado(USUARIO_ID)).thenReturn(1);

		service.verificar(token);

		verify(usuarioRepository).marcarEmailVerificado(USUARIO_ID);
	}

	@Test
	void falloDeEnvioNoExponeTokenNiRevierteLaEmision() {
		when(tokenRepository.invalidarPendientes(eq(USUARIO_ID), eq(AHORA))).thenReturn(0);
		when(tokenRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
		EmailSender fallido = Mockito.mock(EmailSender.class);
		doThrow(new EmailEnvioException("fallo")).when(fallido)
				.enviarVerificacionEmail(any(), any());
		VerificacionEmailProperties properties = new VerificacionEmailProperties();
		properties.setUrlBase("http://localhost:4200/verificar-email");
		service = new VerificacionEmailService(
				tokenRepository, usuarioRepository, fallido, properties, Clock.fixed(AHORA, ZoneOffset.UTC));

		service.emitirParaUsuario(USUARIO_ID);

		verify(tokenRepository).saveAndFlush(any());
	}

	@Test
	void concurrenciaSoloUnHiloVerificaElMismoToken() throws Exception {
		String token = TokenVerificacionEmailSupport.generarTokenEnClaro();
		String hash = TokenVerificacionEmailSupport.sha256Hex(token);
		TokenVerificacionEmail entidad = tokenPendiente(hash);
		AtomicInteger marcados = new AtomicInteger();

		when(tokenRepository.marcarUsadoSiValido(hash, AHORA)).thenAnswer(inv -> {
			if (marcados.compareAndSet(0, 1)) {
				return 1;
			}
			return 0;
		});
		when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(entidad));
		when(usuarioRepository.marcarEmailVerificado(USUARIO_ID)).thenReturn(1);

		CountDownLatch listos = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger exitos = new AtomicInteger();
		AtomicInteger fallos = new AtomicInteger();

		Runnable tarea = () -> {
			listos.countDown();
			try {
				start.await();
				service.verificar(token);
				exitos.incrementAndGet();
			}
			catch (TokenVerificacionInvalidoException ex) {
				fallos.incrementAndGet();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		};

		Thread t1 = new Thread(tarea);
		Thread t2 = new Thread(tarea);
		t1.start();
		t2.start();
		listos.await();
		start.countDown();
		t1.join();
		t2.join();

		assertEquals(1, exitos.get());
		assertEquals(1, fallos.get());
	}

	private VerificacionEmailService servicioConVerificacion(boolean obligatoria) {
		VerificacionEmailProperties properties = new VerificacionEmailProperties();
		properties.setUrlBase("http://localhost:4200/verificar-email");
		properties.setObligatoria(obligatoria);
		return new VerificacionEmailService(
				tokenRepository, usuarioRepository, emailSender, properties, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	private TokenVerificacionEmail tokenPendiente(String hash) {
		TokenVerificacionEmail token = new TokenVerificacionEmail();
		token.setUsuario(usuario);
		token.setTokenHash(hash);
		token.setVenceEn(AHORA.plusSeconds(3600));
		return token;
	}

	static final class CapturingEmailSender implements EmailSender {
		final List<String> enlaces = new ArrayList<>();

		@Override
		public void enviarVerificacionEmail(String email, String enlaceVerificacion) {
			enlaces.add(enlaceVerificacion);
		}
	}
}
