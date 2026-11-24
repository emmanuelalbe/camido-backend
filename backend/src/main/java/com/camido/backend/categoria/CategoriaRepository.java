package com.camido.backend.categoria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

	List<Categoria> findByActivoTrueOrderByOrdenAsc();

	Optional<Categoria> findByIdAndActivoTrue(UUID id);
}
