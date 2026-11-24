package com.camido.backend.categoria;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catálogo global ITF. No pertenece a un usuario ni a una institución.
 */
@Entity
@Table(name = "categorias")
public class Categoria {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "codigo", nullable = false, length = 40)
	private String codigo;

	@Column(name = "nombre", nullable = false, length = 80)
	private String nombre;

	@Column(name = "orden", nullable = false)
	private int orden;

	@Column(name = "activo", nullable = false)
	private boolean activo;

	protected Categoria() {
	}

	public UUID getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNombre() {
		return nombre;
	}

	public int getOrden() {
		return orden;
	}

	public boolean isActivo() {
		return activo;
	}
}
