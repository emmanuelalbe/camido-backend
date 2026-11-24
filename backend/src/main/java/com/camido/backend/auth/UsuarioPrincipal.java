package com.camido.backend.auth;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.camido.backend.usuario.Usuario;

public final class UsuarioPrincipal implements UserDetails {

	private final UUID id;
	private final String nombre;
	private final String apellido;
	private final String email;
	private final boolean emailVerificado;

	public UsuarioPrincipal(Usuario usuario) {
		this.id = usuario.getId();
		this.nombre = usuario.getNombre();
		this.apellido = usuario.getApellido();
		this.email = usuario.getEmail();
		this.emailVerificado = usuario.isEmailVerificado();
	}

	public UUID getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getApellido() {
		return apellido;
	}

	public String getEmail() {
		return email;
	}

	public boolean isEmailVerificado() {
		return emailVerificado;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return id.toString();
	}
}
