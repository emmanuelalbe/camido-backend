package com.camido.backend.usuario;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenVerificacionEmailRepository extends JpaRepository<TokenVerificacionEmail, UUID> {

	Optional<TokenVerificacionEmail> findByTokenHash(String tokenHash);

	@Query("""
			SELECT t FROM TokenVerificacionEmail t
			WHERE t.usuario.id = :usuarioId
			  AND t.usadoEn IS NULL
			  AND t.invalidadoEn IS NULL
			""")
	Optional<TokenVerificacionEmail> findPendienteByUsuarioId(@Param("usuarioId") UUID usuarioId);

	@Query("""
			SELECT COUNT(t) FROM TokenVerificacionEmail t
			WHERE t.usuario.id = :usuarioId
			  AND t.creadoEn >= :desde
			""")
	long countByUsuarioIdAndCreadoEnGreaterThanEqual(
			@Param("usuarioId") UUID usuarioId,
			@Param("desde") Instant desde);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE TokenVerificacionEmail t
			SET t.invalidadoEn = :ahora
			WHERE t.usuario.id = :usuarioId
			  AND t.usadoEn IS NULL
			  AND t.invalidadoEn IS NULL
			""")
	int invalidarPendientes(
			@Param("usuarioId") UUID usuarioId,
			@Param("ahora") Instant ahora);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE TokenVerificacionEmail t
			SET t.usadoEn = :ahora
			WHERE t.tokenHash = :tokenHash
			  AND t.usadoEn IS NULL
			  AND t.invalidadoEn IS NULL
			  AND t.venceEn > :ahora
			""")
	int marcarUsadoSiValido(
			@Param("tokenHash") String tokenHash,
			@Param("ahora") Instant ahora);
}
