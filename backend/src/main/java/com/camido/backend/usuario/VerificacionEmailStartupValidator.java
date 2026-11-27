package com.camido.backend.usuario;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * La verificación de correo es obligatoria salvo una pausa explícita del perfil local.
 * Producción y cualquier arranque sin ese perfil no pueden desactivarla.
 */
public class VerificacionEmailStartupValidator implements ApplicationRunner {

	static final String MENSAJE_PAUSA_NO_AUTORIZADA =
			"CAMIDO_VERIFICACION_EMAIL_OBLIGATORIA=false solo se permite con el perfil local, nunca en producción.";

	private final Environment environment;
	private final VerificacionEmailProperties properties;

	public VerificacionEmailStartupValidator(Environment environment, VerificacionEmailProperties properties) {
		this.environment = environment;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (properties.isObligatoria()) {
			return;
		}
		boolean produccion = environment.acceptsProfiles(Profiles.of("prod", "production"));
		boolean local = environment.acceptsProfiles(Profiles.of("local"));
		if (produccion || !local) {
			throw new IllegalStateException(MENSAJE_PAUSA_NO_AUTORIZADA);
		}
	}
}
