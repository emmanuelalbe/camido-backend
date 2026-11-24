package com.camido.backend.usuario;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = PasswordMaximo72BytesValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMaximo72Bytes {

	String message() default "La contraseña supera el límite de 72 bytes de BCrypt";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
