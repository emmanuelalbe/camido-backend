package com.camido.backend.usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.camido.backend.email.EmailEnvioException;
import com.camido.backend.email.EmailSender;

@Service
public class VerificacionEmailService {

	private static final Logger log = LoggerFactory.getLogger(VerificacionEmailService.class);

	static final Duration INTERVALO_MINIMO_REENVIO = Duration.ofMinutes(2);
	static final Duration VENTANA_REENVIO_HORA = Duration.ofHours(1);
	static final int MAX_REENVIOS_POR_HORA = 5;

	private final TokenVerificacionEmailRepository tokenRepository;
	private final UsuarioRepository usuarioRepository;
	private final EmailSender emailSender;
	private final VerificacionEmailProperties properties;
	private final Clock clock;

	public VerificacionEmailService(
			TokenVerificacionEmailRepository tokenRepository,
			UsuarioRepository usuarioRepository,
			EmailSender emailSender,
			VerificacionEmailProperties properties,
			Clock clock) {
		this.tokenRepository = tokenRepository;
		this.usuarioRepository = usuarioRepository;
		this.emailSender = emailSender;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Emite un token pendiente (transacción) y envía el correo solo tras el commit.
	 * Un fallo de envío no revierte el token ni expone secretos.
	 */
	@Transactional
	public void emitirParaUsuario(UUID usuarioId) {
		Usuario usuario = usuarioRepository.findByIdForUpdate(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("Usuario inexistente"));
		if (usuario.isEmailVerificado() || usuario.getEstado() != UsuarioEstado.ACTIVO) {
			return;
		}
		crearTokenYProgramarEnvio(usuario);
	}

	/**
	 * Consume el token y marca {@code emailVerificado=true} en la misma transacción.
	 * La concurrencia se resuelve con el UPDATE atómico del token (solo un ganador).
	 */
	@Transactional
	public void verificar(String tokenEnClaro) {
		if (tokenEnClaro == null || tokenEnClaro.isBlank()) {
			throw new TokenVerificacionInvalidoException();
		}

		Instant ahora = clock.instant();
		String tokenHash = TokenVerificacionEmailSupport.sha256Hex(tokenEnClaro);
		int actualizados = tokenRepository.marcarUsadoSiValido(tokenHash, ahora);
		if (actualizados != 1) {
			throw new TokenVerificacionInvalidoException();
		}

		TokenVerificacionEmail token = tokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(TokenVerificacionInvalidoException::new);
		int verificados = usuarioRepository.marcarEmailVerificado(token.getUsuario().getId());
		if (verificados != 1 && !token.getUsuario().isEmailVerificado()) {
			throw new TokenVerificacionInvalidoException();
		}
	}

	/**
	 * Reenvío con bloqueo pesimista del usuario para serializar límites y el pendiente.
	 */
	@Transactional
	public void reenviar(String email) {
		Optional<Usuario> posible = usuarioRepository.findByEmailIgnoreCaseForUpdate(email);
		if (posible.isEmpty()) {
			return;
		}

		Usuario usuario = posible.get();
		if (usuario.getEstado() != UsuarioEstado.ACTIVO || usuario.isEmailVerificado()) {
			return;
		}

		Instant ahora = clock.instant();
		if (superaLimiteReenvio(usuario.getId(), ahora)) {
			return;
		}

		try {
			crearTokenYProgramarEnvio(usuario);
		}
		catch (DataIntegrityViolationException ex) {
			// Carrera residual con el índice de pendiente.
		}
	}

	private boolean superaLimiteReenvio(UUID usuarioId, Instant ahora) {
		long enDosMinutos = tokenRepository.countByUsuarioIdAndCreadoEnGreaterThanEqual(
				usuarioId, ahora.minus(INTERVALO_MINIMO_REENVIO));
		if (enDosMinutos >= 1) {
			return true;
		}
		long enUnaHora = tokenRepository.countByUsuarioIdAndCreadoEnGreaterThanEqual(
				usuarioId, ahora.minus(VENTANA_REENVIO_HORA));
		return enUnaHora >= MAX_REENVIOS_POR_HORA;
	}

	private void crearTokenYProgramarEnvio(Usuario usuario) {
		Instant ahora = clock.instant();
		tokenRepository.invalidarPendientes(usuario.getId(), ahora);

		String tokenEnClaro = TokenVerificacionEmailSupport.generarTokenEnClaro();
		String tokenHash = TokenVerificacionEmailSupport.sha256Hex(tokenEnClaro);

		TokenVerificacionEmail token = new TokenVerificacionEmail();
		token.setUsuario(usuario);
		token.setTokenHash(tokenHash);
		token.setVenceEn(ahora.plus(Duration.ofHours(TokenVerificacionEmailSupport.HORAS_VIGENCIA)));
		tokenRepository.saveAndFlush(token);

		String enlace = properties.construirEnlace(tokenEnClaro);
		String emailDestino = usuario.getEmail();
		programarEnvioTrasCommit(emailDestino, enlace);
	}

	private void programarEnvioTrasCommit(String email, String enlace) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			enviarSinExponerSecretos(email, enlace);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				enviarSinExponerSecretos(email, enlace);
			}
		});
	}

	private void enviarSinExponerSecretos(String email, String enlace) {
		try {
			emailSender.enviarVerificacionEmail(email, enlace);
		}
		catch (EmailEnvioException ex) {
			log.warn("El correo de verificación no se envió; el token permanece válido hasta su vencimiento");
		}
		catch (RuntimeException ex) {
			log.warn("El correo de verificación no se envió; el token permanece válido hasta su vencimiento");
		}
	}
}
