package com.camido.backend.usuario;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * Acceso a {@code usuarios}. Las búsquedas por email usan {@code LOWER(...)}
 * para alinearse con el índice único {@code uq_usuarios_email_lower}.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	@Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
	Optional<Usuario> findByEmailIgnoreCase(@Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
	Optional<Usuario> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM Usuario u WHERE u.id = :id")
	Optional<Usuario> findByIdForUpdate(@Param("id") UUID id);

	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
	boolean existsByEmailIgnoreCase(@Param("email") String email);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Usuario u
			SET u.emailVerificado = TRUE
			WHERE u.id = :usuarioId
			  AND u.emailVerificado = FALSE
			""")
	int marcarEmailVerificado(@Param("usuarioId") UUID usuarioId);
}
