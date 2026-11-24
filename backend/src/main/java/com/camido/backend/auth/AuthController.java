package com.camido.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	static final String MENSAJE_RECHAZO = "No se pudo iniciar sesión";
	static final String MENSAJE_LIMITE = "Demasiados intentos. Probá más tarde";

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final ChangeSessionIdAuthenticationStrategy sessionAuthenticationStrategy;
	private final LoginAttemptLimiter loginAttemptLimiter;

	public AuthController(
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository,
			ChangeSessionIdAuthenticationStrategy sessionAuthenticationStrategy,
			LoginAttemptLimiter loginAttemptLimiter) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.loginAttemptLimiter = loginAttemptLimiter;
	}

	@GetMapping("/csrf")
	public ResponseEntity<Void> csrf() {
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		String clave = claveLimite(httpRequest, request.getEmail());
		if (loginAttemptLimiter.bloqueado(clave)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.body(new MensajeAuth(MENSAJE_LIMITE));
		}
		try {
			Authentication authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(request.getEmail(), request.getPassword()));
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
			securityContextRepository.saveContext(context, httpRequest, httpResponse);
			loginAttemptLimiter.reiniciar(clave);
			return ResponseEntity.ok(UsuarioSesionResponse.desde((UsuarioPrincipal) authentication.getPrincipal()));
		}
		catch (BadCredentialsException ex) {
			loginAttemptLimiter.registrarFallo(clave);
			SecurityContextHolder.clearContext();
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeAuth(MENSAJE_RECHAZO));
		}
	}

	@GetMapping("/me")
	public ResponseEntity<UsuarioSesionResponse> me() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(UsuarioSesionResponse.desde(principal));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		SecurityContextHolder.clearContext();
		var session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<MensajeAuth> datosInvalidos() {
		return ResponseEntity.badRequest().body(new MensajeAuth(MENSAJE_RECHAZO));
	}

	public record MensajeAuth(String mensaje) {
	}

	private static String claveLimite(HttpServletRequest request, String email) {
		String ip = request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
		return ip + "|" + AuthService.normalizarEmail(email);
	}
}
