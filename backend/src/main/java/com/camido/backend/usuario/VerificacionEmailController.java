package com.camido.backend.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class VerificacionEmailController {

	private final VerificacionEmailService verificacionEmailService;

	public VerificacionEmailController(VerificacionEmailService verificacionEmailService) {
		this.verificacionEmailService = verificacionEmailService;
	}

	@PostMapping("/verificacion-email")
	public ResponseEntity<MensajeVerificacion> verificar(@Valid @RequestBody VerificarEmailRequest request) {
		verificacionEmailService.verificar(request.getToken());
		return ResponseEntity.ok(new MensajeVerificacion("Correo verificado"));
	}

	@PostMapping("/verificacion-email/reenvio")
	public ResponseEntity<MensajeVerificacion> reenviar(
			@Valid @RequestBody ReenviarVerificacionEmailRequest request) {
		verificacionEmailService.reenviar(request.getEmail());
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(new MensajeVerificacion("Si el correo existe, recibirás instrucciones"));
	}

	@ExceptionHandler(TokenVerificacionInvalidoException.class)
	public ResponseEntity<MensajeVerificacion> tokenInvalido() {
		return ResponseEntity.badRequest()
				.body(new MensajeVerificacion("El enlace es inválido o ha vencido"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<MensajeVerificacion> datosInvalidos() {
		return ResponseEntity.badRequest()
				.body(new MensajeVerificacion("Los datos no son válidos"));
	}

	public record MensajeVerificacion(String mensaje) {
	}
}
