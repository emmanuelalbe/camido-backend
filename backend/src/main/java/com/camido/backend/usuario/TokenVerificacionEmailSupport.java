package com.camido.backend.usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generación y hash de tokens de verificación.
 * El valor en claro nunca debe persistirse ni registrarse en logs.
 */
final class TokenVerificacionEmailSupport {

	static final int BYTES_TOKEN = 32;
	static final int HORAS_VIGENCIA = 24;

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private TokenVerificacionEmailSupport() {
	}

	static String generarTokenEnClaro() {
		byte[] bytes = new byte[BYTES_TOKEN];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String sha256Hex(String tokenEnClaro) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(tokenEnClaro.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 no disponible", ex);
		}
	}
}
