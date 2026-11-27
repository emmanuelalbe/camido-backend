package com.camido.backend.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.camido.backend.config.PasswordEncoderConfig;
import com.camido.backend.config.SecurityConfig;
import com.camido.backend.usuario.Usuario;
import com.camido.backend.usuario.UsuarioEstado;
import com.camido.backend.usuario.UsuarioRepository;
import com.camido.backend.usuario.VerificacionEmailProperties;

import jakarta.servlet.http.HttpSession;

@WebMvcTest(AuthController.class)
@EnableConfigurationProperties(VerificacionEmailProperties.class)
@Import({
		SecurityConfig.class,
		AuthService.class,
		UsuarioAuthenticationProvider.class,
		LoginAttemptLimiter.class,
		PasswordEncoderConfig.class
})
class AuthControllerTest {

	private static final String EMAIL = "ana@example.com";
	private static final String PASSWORD = "clave-segura-1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	@Autowired
	private LoginAttemptLimiter loginAttemptLimiter;

	@BeforeEach
	void limpiarIntentos() {
		loginAttemptLimiter.reiniciar("127.0.0.1|" + EMAIL);
		loginAttemptLimiter.reiniciar("127.0.0.1|nadie@example.com");
		loginAttemptLimiter.reiniciar("127.0.0.1|bloqueo@example.com");
	}

	@Test
	void sinCsrfElLoginResponde403() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(EMAIL, PASSWORD)))
				.andExpect(status().isForbidden());
	}

	@Test
	void loginCorrectoPermiteMeYLogoutInvalidaLaSesion() throws Exception {
		when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(usuarioValido()));

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(EMAIL, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn();

		HttpSession session = login.getRequest().getSession(false);
		mockMvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Ana"));

		mockMvc.perform(post("/api/auth/logout").with(csrf()).session((org.springframework.mock.web.MockHttpSession) session))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void credencialesInvalidasNoDistinguenElMotivo() throws Exception {
		when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
		mockMvc.perform(post("/api/auth/login").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json("nadie@example.com", PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value(AuthController.MENSAJE_RECHAZO));

		when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(usuarioValido()));
		mockMvc.perform(post("/api/auth/login").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(EMAIL, "incorrecta")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value(AuthController.MENSAJE_RECHAZO));
	}

	@Test
	void sinSesionMeEs401() throws Exception {
		mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void demasiadosIntentosResponde429() throws Exception {
		when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
		for (int i = 0; i < LoginAttemptLimiter.MAX_FALLOS; i++) {
			mockMvc.perform(post("/api/auth/login").with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content(json("bloqueo@example.com", "mala")))
					.andExpect(status().isUnauthorized());
		}
		mockMvc.perform(post("/api/auth/login").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json("bloqueo@example.com", "mala")))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.mensaje").value(AuthController.MENSAJE_LIMITE));
	}

	@Test
	void elCatalogoDeCategoriasNoExigeSesion() throws Exception {
		mockMvc.perform(get("/api/categorias")).andExpect(status().isNotFound());
	}

	@Test
	void escribirCategoriasExigeSesion() throws Exception {
		mockMvc.perform(post("/api/categorias").with(csrf())).andExpect(status().isUnauthorized());
	}

	private Usuario usuarioValido() throws Exception {
		Constructor<Usuario> constructor = Usuario.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		Usuario usuario = constructor.newInstance();
		usuario.setNombre("Ana");
		usuario.setApellido("Paz");
		usuario.setEmail(EMAIL);
		usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
		usuario.setEmailVerificado(true);
		usuario.setEstado(UsuarioEstado.ACTIVO);
		Field id = Usuario.class.getDeclaredField("id");
		id.setAccessible(true);
		id.set(usuario, UUID.fromString("22222222-2222-2222-2222-222222222222"));
		return usuario;
	}

	private static String json(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}
}
