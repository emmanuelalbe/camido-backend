package com.camido.backend.usuario;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.camido.backend.categoria.CategoriaInvalidaException;
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
public class RegistroUsuarioController {

	private final RegistroUsuarioService registroUsuarioService;

	public RegistroUsuarioController(RegistroUsuarioService registroUsuarioService) {
		this.registroUsuarioService = registroUsuarioService;
	}

	@PostMapping("/registro")
	public ResponseEntity<UUID> registrar(@Valid @RequestBody RegistroUsuarioRequest request) {
		UUID id = registroUsuarioService.registrar(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(id);
	}

	@ExceptionHandler(EmailYaRegistradoException.class)
	public ResponseEntity<ErrorRegistro> emailDuplicado() {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorRegistro("El email ya está registrado"));
	}

	@ExceptionHandler({ MethodArgumentNotValidException.class, CategoriaInvalidaException.class })
	public ResponseEntity<ErrorRegistro> datosInvalidos() {
		return ResponseEntity.badRequest()
				.body(new ErrorRegistro("Los datos de registro no son válidos"));
	}

	public record ErrorRegistro(String mensaje) {
	}
}
