package com.camido.backend.config;

import java.io.IOException;
import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import com.camido.backend.auth.UsuarioAuthenticationProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	AuthenticationManager authenticationManager(UsuarioAuthenticationProvider provider) {
		return new ProviderManager(provider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	ChangeSessionIdAuthenticationStrategy sessionAuthenticationStrategy() {
		return new ChangeSessionIdAuthenticationStrategy();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfRepository)
						.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.sessionFixation(fixation -> fixation.changeSessionId()))
				.securityContext(context -> context.securityContextRepository(securityContextRepository()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/saludo").permitAll()
						.requestMatchers("/api/usuarios/registro").permitAll()
						.requestMatchers("/api/usuarios/verificacion-email").permitAll()
						.requestMatchers("/api/usuarios/verificacion-email/reenvio").permitAll()
						.requestMatchers("/api/auth/login").permitAll()
						.requestMatchers("/api/auth/csrf").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/categorias").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll())
				.logout(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
		return http.build();
	}

	/**
	 * Patrón SPA de Spring Security: el valor de la cookie XSRF-TOKEN coincide con
	 * el header X-XSRF-TOKEN que envía Angular.
	 */
	static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

		private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
		private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
			this.xor.handle(request, response, csrfToken);
			csrfToken.get();
		}

		@Override
		public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
			String header = request.getHeader(csrfToken.getHeaderName());
			if (header != null) {
				return this.plain.resolveCsrfTokenValue(request, csrfToken);
			}
			return this.xor.resolveCsrfTokenValue(request, csrfToken);
		}
	}

	static final class CsrfCookieFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {
			CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
			if (token != null) {
				token.getToken();
			}
			chain.doFilter(request, response);
		}
	}
}
