package com.camido.backend.usuario;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camido.verificacion-email")
public class VerificacionEmailProperties {

	/**
	 * Base del enlace (sin query). Ejemplo: http://localhost:4200/verificar-email
	 */
	private String urlBase = "http://localhost:4200/verificar-email";

	/**
	 * Por defecto el login exige el correo verificado y el alta intenta enviar el enlace.
	 * {@code false} es una pausa temporal: solo arranca con el perfil {@code local}.
	 */
	private boolean obligatoria = true;

	public String getUrlBase() {
		return urlBase;
	}

	public void setUrlBase(String urlBase) {
		this.urlBase = urlBase;
	}

	public boolean isObligatoria() {
		return obligatoria;
	}

	public void setObligatoria(boolean obligatoria) {
		this.obligatoria = obligatoria;
	}

	String construirEnlace(String tokenEnClaro) {
		String base = urlBase == null ? "" : urlBase.trim();
		String separador = base.contains("?") ? "&" : "?";
		return base + separador + "token=" + tokenEnClaro;
	}
}
