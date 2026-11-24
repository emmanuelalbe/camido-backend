package com.camido.backend.usuario;

import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.camido.backend.categoria.Categoria;
import com.camido.backend.categoria.CategoriaInvalidaException;
import com.camido.backend.categoria.CategoriaRepository;
import com.camido.backend.categoria.DeclaracionCategoria;
import com.camido.backend.categoria.DeclaracionCategoriaRepository;

@Service
public class RegistroUsuarioService {

	private static final Logger log = LoggerFactory.getLogger(RegistroUsuarioService.class);

	static final String INDICE_EMAIL = "uq_usuarios_email_lower";

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final TransactionTemplate transactionTemplate;
	private final VerificacionEmailService verificacionEmailService;
	private final CategoriaRepository categoriaRepository;
	private final DeclaracionCategoriaRepository declaracionCategoriaRepository;

	public RegistroUsuarioService(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder,
			PlatformTransactionManager transactionManager,
			VerificacionEmailService verificacionEmailService,
			CategoriaRepository categoriaRepository,
			DeclaracionCategoriaRepository declaracionCategoriaRepository) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.verificacionEmailService = verificacionEmailService;
		this.categoriaRepository = categoriaRepository;
		this.declaracionCategoriaRepository = declaracionCategoriaRepository;
	}

	/**
	 * Registra la cuenta global. Devuelve solo el id: no la contraseña ni el hash.
	 * Tras el alta intenta emitir el token de verificación; un fallo ahí no
	 * revierte el registro (el usuario puede usar el reenvío).
	 */
	public UUID registrar(RegistroUsuarioRequest request) {
		UUID id;
		try {
			id = transactionTemplate.execute(status -> insertar(request));
		}
		catch (EmailYaRegistradoException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			if (esEmailDuplicado(ex)) {
				throw new EmailYaRegistradoException();
			}
			throw ex;
		}

		try {
			verificacionEmailService.emitirParaUsuario(id);
		}
		catch (RuntimeException ex) {
			log.warn("No se pudo emitir el token de verificación tras el registro");
		}
		return id;
	}

	private UUID insertar(RegistroUsuarioRequest request) {
		if (usuarioRepository.existsByEmailIgnoreCase(request.getEmail())) {
			throw new EmailYaRegistradoException();
		}

		if (exigeCategoria(request.getIntencion()) && request.getCategoriaId() == null) {
			throw new CategoriaInvalidaException();
		}

		Categoria declarada = null;
		if (request.getCategoriaId() != null) {
			declarada = categoriaRepository.findByIdAndActivoTrue(request.getCategoriaId())
					.orElseThrow(CategoriaInvalidaException::new);
		}

		Usuario usuario = new Usuario();
		usuario.setNombre(request.getNombre());
		usuario.setApellido(request.getApellido());
		usuario.setEmail(request.getEmail());
		usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		usuario.setFechaNacimiento(request.getFechaNacimiento());
		usuario.setEmailVerificado(false);
		usuario.setEstado(UsuarioEstado.ACTIVO);
		Usuario guardado = usuarioRepository.saveAndFlush(usuario);
		if (declarada != null) {
			declaracionCategoriaRepository.save(new DeclaracionCategoria(guardado, declarada));
		}
		return guardado.getId();
	}

	private static boolean exigeCategoria(IntencionIncorporacion intencion) {
		return intencion == IntencionIncorporacion.ALUMNO || intencion == IntencionIncorporacion.PROFESOR;
	}

	private boolean esEmailDuplicado(Throwable error) {
		Throwable actual = error;
		while (actual != null) {
			if (actual instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
				return true;
			}
			if (actual instanceof ConstraintViolationException violacion) {
				if ("23505".equals(violacion.getSQLState())) {
					return true;
				}
				String nombre = violacion.getConstraintName();
				if (nombre != null && nombre.toLowerCase().contains(INDICE_EMAIL)) {
					return true;
				}
			}
			if (actual instanceof DataIntegrityViolationException) {
				String mensaje = actual.getMessage();
				if (mensaje != null && mensaje.toLowerCase().contains(INDICE_EMAIL)) {
					return true;
				}
			}
			String mensaje = actual.getMessage();
			if (mensaje != null && mensaje.toLowerCase().contains(INDICE_EMAIL)) {
				return true;
			}
			actual = actual.getCause();
		}
		return false;
	}
}
