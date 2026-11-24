package com.camido.backend.categoria;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

	private final CategoriaRepository categoriaRepository;

	public CategoriaController(CategoriaRepository categoriaRepository) {
		this.categoriaRepository = categoriaRepository;
	}

	@GetMapping
	public List<CategoriaResponse> listar() {
		return categoriaRepository.findByActivoTrueOrderByOrdenAsc().stream()
				.map(categoria -> new CategoriaResponse(
						categoria.getId(),
						categoria.getCodigo(),
						categoria.getNombre(),
						categoria.getOrden()))
				.toList();
	}

	public record CategoriaResponse(UUID id, String codigo, String nombre, int orden) {
	}
}
