package com.camido.backend.usuario;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA mapeada a {@code public.tokens_verificacion_email} (migración V2).
 * {@code tokenHash} almacena SHA-256 del token; nunca el token en claro.
 */
@Entity
@Table(name = "tokens_verificacion_email")
public class TokenVerificacionEmail {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
	private String tokenHash;

	@CreationTimestamp
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	@Column(name = "vence_en", nullable = false)
	private Instant venceEn;

	@Column(name = "usado_en")
	private Instant usadoEn;

	@Column(name = "invalidado_en")
	private Instant invalidadoEn;

	protected TokenVerificacionEmail() {
		// Requerido por JPA
	}

	public UUID getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

	public Instant getVenceEn() {
		return venceEn;
	}

	public void setVenceEn(Instant venceEn) {
		this.venceEn = venceEn;
	}

	public Instant getUsadoEn() {
		return usadoEn;
	}

	public void setUsadoEn(Instant usadoEn) {
		this.usadoEn = usadoEn;
	}

	public Instant getInvalidadoEn() {
		return invalidadoEn;
	}

	public void setInvalidadoEn(Instant invalidadoEn) {
		this.invalidadoEn = invalidadoEn;
	}
}
