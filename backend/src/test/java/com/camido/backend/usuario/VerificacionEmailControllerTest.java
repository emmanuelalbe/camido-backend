package com.camido.backend.usuario;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VerificacionEmailController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(VerificacionEmailControllerTest.Config.class)
class VerificacionEmailControllerTest {

	private static final String TOKEN = "token-secreto-de-prueba";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private VerificacionEmailService verificacionEmailService;

	@Test
	void verificarExitosoDevuelve200SinElToken() throws Exception {
		doNothing().when(verificacionEmailService).verificar(TOKEN);

		mockMvc.perform(post("/api/usuarios/verificacion-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + TOKEN + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensaje").value("Correo verificado"))
				.andExpect(content().string(not(containsString(TOKEN))));
	}

	@Test
	void tokenInvalidoDevuelve400Generico() throws Exception {
		doThrow(new TokenVerificacionInvalidoException()).when(verificacionEmailService).verificar(anyString());

		mockMvc.perform(post("/api/usuarios/verificacion-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + TOKEN + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El enlace es inválido o ha vencido"))
				.andExpect(content().string(not(containsString(TOKEN))))
				.andExpect(content().string(not(containsString("SHA"))))
				.andExpect(content().string(not(containsString("Exception"))));
	}

	@Test
	void reenvioSiempreDevuelve202() throws Exception {
		doNothing().when(verificacionEmailService).reenviar("ana@ejemplo.com");

		mockMvc.perform(post("/api/usuarios/verificacion-email/reenvio")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"ana@ejemplo.com\"}"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.mensaje").value("Si el correo existe, recibirás instrucciones"));
	}

	@TestConfiguration
	static class Config {
		@Bean
		VerificacionEmailService verificacionEmailService() {
			return Mockito.mock(VerificacionEmailService.class);
		}
	}
}
