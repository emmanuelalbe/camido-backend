package com.camido.backend.usuario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public class VerificarEmailRequest {

	@NotBlank
	private String token;

	@JsonIgnore
	public String getToken() {
		return token;
	}

	@JsonProperty("token")
	public void setToken(String token) {
		this.token = token;
	}

	@Override
	public String toString() {
		return "VerificarEmailRequest{}";
	}
}
