package com.camido.backend.categoria;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeclaracionCategoriaRepository extends JpaRepository<DeclaracionCategoria, UUID> {
}
