package com.camido.backend.categoria;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.camido.backend.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Cinturón que el alumno dice tener al crear la cuenta.
 * No crea membresía, no asigna rol y no es la categoría oficial.
 */
@Entity
@Table(name = "declaraciones_categoria")
public class DeclaracionCategoria {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, updatable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false, updatable = false)
	private Categoria categoria;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private DeclaracionEstado estado;

	@CreationTimestamp
	@Column(name = "fecha", nullable = false, updatable = false)
	private Instant fecha;

	protected DeclaracionCategoria() {
	}

	public DeclaracionCategoria(Usuario usuario, Categoria categoria) {
		this.usuario = usuario;
		this.categoria = categoria;
		this.estado = DeclaracionEstado.DECLARADA;
	}

	public UUID getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public DeclaracionEstado getEstado() {
		return estado;
	}

	public Instant getFecha() {
		return fecha;
	}
}
