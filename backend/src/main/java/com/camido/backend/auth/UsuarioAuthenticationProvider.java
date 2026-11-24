package com.camido.backend.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class UsuarioAuthenticationProvider implements AuthenticationProvider {

	private final AuthService authService;

	public UsuarioAuthenticationProvider(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String email = authentication.getName();
		String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
		UsuarioPrincipal principal = authService.autenticar(email, password);
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
