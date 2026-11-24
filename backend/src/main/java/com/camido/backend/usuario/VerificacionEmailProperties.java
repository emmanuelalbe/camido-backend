package com.camido.backend.usuario;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camido.verificacion-email")
public class VerificacionEmailProperties {

	/**
	 * Base del enlace (sin query). Ejemplo: http://localhost:4200/verificar-email
	 */
	private String urlBase = "http://localhost:4200/verificar-email";

	public String getUrlBase() {
		return urlBase;
	}

	public void setUrlBase(String urlBase) {
		this.urlBase = urlBase;
	}

	String construirEnlace(String tokenEnClaro) {
		String base = urlBase == null ? "" : urlBase.trim();
		String separador = base.contains("?") ? "&" : "?";
		return base + separador + "token=" + tokenEnClaro;
	}
}
