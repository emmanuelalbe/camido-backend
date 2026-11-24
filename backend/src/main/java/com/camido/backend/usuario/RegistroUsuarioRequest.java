package com.camido.backend.usuario;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta de la cuenta global. No incluye rol (pertenece a la membresía)
 * ni passwordHash (el backend lo genera con BCrypt).
 */
public class RegistroUsuarioRequest {

	@NotBlank
	@Size(max = 100)
	private String nombre;

	@NotBlank
	@Size(max = 100)
	private String apellido;

	@NotBlank
	@Email
	@Size(max = 320)
	private String email;

	/**
	 * Contraseña en claro solo en memoria de este request.
	 * BCrypt usa como máximo 72 bytes UTF-8, no 72 caracteres.
	 * El setter acepta el JSON de entrada; el getter no se serializa.
	 */
	@NotBlank
	@Size(min = 8)
	@PasswordMaximo72Bytes
	private String password;

	@NotNull
	@Past
	private LocalDate fechaNacimiento;

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellido() {
		return apellido;
	}

	public void setApellido(String apellido) {
		this.apellido = apellido;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@JsonIgnore
	public String getPassword() {
		return password;
	}

	@JsonProperty("password")
	public void setPassword(String password) {
		this.password = password;
	}

	public LocalDate getFechaNacimiento() {
		return fechaNacimiento;
	}

	public void setFechaNacimiento(LocalDate fechaNacimiento) {
		this.fechaNacimiento = fechaNacimiento;
	}

	@Override
	public String toString() {
		return "RegistroUsuarioRequest{nombre='" + nombre + "', apellido='" + apellido
				+ "', email='" + email + "', fechaNacimiento=" + fechaNacimiento + "}";
	}
}
