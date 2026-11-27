package com.camido.backend.usuario;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class VerificacionEmailStartupValidatorTest {

	@Test
	void obligatoriaArrancaEnCualquierPerfil() {
		assertDoesNotThrow(() -> validar(true, new String[0]));
		assertDoesNotThrow(() -> validar(true, "prod"));
		assertDoesNotThrow(() -> validar(true, "local"));
	}

	@Test
	void pausaSoloArrancaConPerfilLocal() {
		assertDoesNotThrow(() -> validar(false, "local"));
	}

	@Test
	void pausaFueraDelPerfilLocalRechazaElArranque() {
		IllegalStateException sinPerfil = assertThrows(IllegalStateException.class, () -> validar(false));
		IllegalStateException produccion = assertThrows(IllegalStateException.class, () -> validar(false, "prod"));
		IllegalStateException production = assertThrows(IllegalStateException.class, () -> validar(false, "production"));
		IllegalStateException localYProd = assertThrows(
				IllegalStateException.class, () -> validar(false, "local", "prod"));

		assertTrue(sinPerfil.getMessage().contains("perfil local"));
		assertTrue(produccion.getMessage().contains("perfil local"));
		assertTrue(production.getMessage().contains("perfil local"));
		assertTrue(localYProd.getMessage().contains("perfil local"));
	}

	private static void validar(boolean obligatoria, String... perfiles) {
		VerificacionEmailProperties properties = new VerificacionEmailProperties();
		properties.setObligatoria(obligatoria);
		MockEnvironment environment = new MockEnvironment();
		if (perfiles.length > 0) {
			environment.setActiveProfiles(perfiles);
		}
		new VerificacionEmailStartupValidator(environment, properties)
				.run(new DefaultApplicationArguments(new String[0]));
	}
}
