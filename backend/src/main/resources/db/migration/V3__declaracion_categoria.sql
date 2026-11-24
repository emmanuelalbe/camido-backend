-- V3: declaración inicial de categoría del alumno.
-- No es un cinturón oficial, no crea membresía y no otorga roles.
-- La revisión institucional se modelará después, sobre la membresía.

CREATE TABLE declaraciones_categoria (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id    UUID        NOT NULL,
    categoria_id  UUID        NOT NULL,
    estado        VARCHAR(20) NOT NULL DEFAULT 'DECLARADA',
    fecha         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_declaraciones_categoria_usuario UNIQUE (usuario_id),
    CONSTRAINT chk_declaraciones_categoria_estado
        CHECK (estado = 'DECLARADA'),
    CONSTRAINT fk_declaraciones_categoria_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_declaraciones_categoria_categoria
        FOREIGN KEY (categoria_id) REFERENCES categorias (id) ON DELETE RESTRICT
);

CREATE INDEX ix_declaraciones_categoria_categoria_id
    ON declaraciones_categoria (categoria_id);
