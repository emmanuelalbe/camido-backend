package com.camido.backend.usuario;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMaximo72BytesValidator implements ConstraintValidator<PasswordMaximo72Bytes, String> {

	static final int LIMITE_BYTES = 72;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}
		return value.getBytes(StandardCharsets.UTF_8).length <= LIMITE_BYTES;
	}
}
