-- V1: esquema base multi-tenant de Cami-Do
-- Solo CREATE / INSERT. Sin DROP, TRUNCATE, DELETE ni ALTER de tablas ajenas.
--
-- Autorización (backend): las FKs no sustituyen controles de acceso.
-- En cada operación el backend DEBE validar membresía ACTIVA, institución
-- correcta y roles SOLO desde membresia_roles (nunca desde solicitudes PENDIENTE).

-- ---------------------------------------------------------------------------
-- usuarios: identidad global de la plataforma
-- ---------------------------------------------------------------------------
CREATE TABLE usuarios (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre            VARCHAR(100)  NOT NULL,
    apellido          VARCHAR(100)  NOT NULL,
    email             VARCHAR(320)  NOT NULL,
    password_hash     VARCHAR(255)  NOT NULL,
    fecha_nacimiento  DATE          NOT NULL,
    email_verificado  BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_registro    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    estado            VARCHAR(20)   NOT NULL,
    CONSTRAINT chk_usuarios_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO', 'SUSPENDIDO'))
);

CREATE UNIQUE INDEX uq_usuarios_email_lower
    ON usuarios (LOWER(email));

-- ---------------------------------------------------------------------------
-- instituciones: tenants (codigo único sin distinguir mayúsculas)
-- ---------------------------------------------------------------------------
CREATE TABLE instituciones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          VARCHAR(150) NOT NULL,
    codigo          VARCHAR(50)  NOT NULL,
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    estado          VARCHAR(20)  NOT NULL,
    CONSTRAINT chk_instituciones_estado
        CHECK (estado IN ('ACTIVA', 'INACTIVA'))
);

CREATE UNIQUE INDEX uq_instituciones_codigo_lower
    ON instituciones (LOWER(codigo));

-- ---------------------------------------------------------------------------
-- membresias: vínculo usuario ↔ institución (una por par)
-- ---------------------------------------------------------------------------
CREATE TABLE membresias (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id         UUID         NOT NULL,
    institucion_id     UUID         NOT NULL,
    estado             VARCHAR(20)  NOT NULL,
    fecha_solicitud    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_aprobacion   TIMESTAMPTZ  NULL,
    CONSTRAINT uq_membresias_usuario_institucion
        UNIQUE (usuario_id, institucion_id),
    CONSTRAINT chk_membresias_estado
        CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'RECHAZADA', 'BAJA')),
    CONSTRAINT chk_membresias_fecha_aprobacion
        CHECK (
            (estado = 'ACTIVA' AND fecha_aprobacion IS NOT NULL)
            OR (estado <> 'ACTIVA')
        ),
    CONSTRAINT fk_membresias_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_membresias_institucion
        FOREIGN KEY (institucion_id) REFERENCES instituciones (id) ON DELETE RESTRICT
);

CREATE INDEX ix_membresias_institucion_id ON membresias (institucion_id);
CREATE INDEX ix_membresias_usuario_id ON membresias (usuario_id);

-- ---------------------------------------------------------------------------
-- membresia_roles: ÚNICA fuente de permisos (roles aprobados)
-- ---------------------------------------------------------------------------
CREATE TABLE membresia_roles (
    membresia_id  UUID        NOT NULL,
    rol           VARCHAR(20) NOT NULL,
    CONSTRAINT pk_membresia_roles PRIMARY KEY (membresia_id, rol),
    CONSTRAINT chk_membresia_roles_rol
        CHECK (rol IN ('ORGANIZADOR', 'PROFESOR', 'ALUMNO')),
    CONSTRAINT fk_membresia_roles_membresia
        FOREIGN KEY (membresia_id) REFERENCES membresias (id) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------------
-- membresia_rol_solicitudes: NO otorgan permisos.
-- Al aprobar, el backend debe insertar explícitamente en membresia_roles.
-- ---------------------------------------------------------------------------
CREATE TABLE membresia_rol_solicitudes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    membresia_id            UUID         NOT NULL,
    rol                     VARCHAR(20)  NOT NULL,
    estado                  VARCHAR(20)  NOT NULL,
    fecha_solicitud         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_resolucion        TIMESTAMPTZ  NULL,
    resuelto_por_usuario_id UUID         NULL,
    motivo_decision         VARCHAR(500) NULL,
    CONSTRAINT chk_membresia_rol_solicitudes_rol
        CHECK (rol IN ('ORGANIZADOR', 'PROFESOR', 'ALUMNO')),
    CONSTRAINT chk_membresia_rol_solicitudes_estado
        CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT chk_membresia_rol_solicitudes_resolucion
        CHECK (
            (
                estado = 'PENDIENTE'
                AND fecha_resolucion IS NULL
                AND resuelto_por_usuario_id IS NULL
                AND motivo_decision IS NULL
            )
            OR (
                estado IN ('APROBADA', 'RECHAZADA')
                AND fecha_resolucion IS NOT NULL
                AND resuelto_por_usuario_id IS NOT NULL
                AND motivo_decision IS NOT NULL
                AND length(trim(motivo_decision)) > 0
            )
        ),
    CONSTRAINT fk_membresia_rol_solicitudes_membresia
        FOREIGN KEY (membresia_id) REFERENCES membresias (id) ON DELETE RESTRICT,
    CONSTRAINT fk_membresia_rol_solicitudes_resuelto_por
        FOREIGN KEY (resuelto_por_usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_membresia_rol_solicitud_pendiente
    ON membresia_rol_solicitudes (membresia_id, rol)
    WHERE estado = 'PENDIENTE';

CREATE INDEX ix_membresia_rol_solicitudes_membresia_id
    ON membresia_rol_solicitudes (membresia_id);

-- ---------------------------------------------------------------------------
-- categorias: catálogo global ITF (19 grados)
-- ---------------------------------------------------------------------------
CREATE TABLE categorias (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo   VARCHAR(40)  NOT NULL,
    nombre   VARCHAR(80)  NOT NULL,
    orden    INTEGER      NOT NULL,
    activo   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_categorias_codigo UNIQUE (codigo),
    CONSTRAINT uq_categorias_orden UNIQUE (orden)
);

INSERT INTO categorias (codigo, nombre, orden) VALUES
    ('BLANCO',                 'Blanco',                   1),
    ('BLANCO_PUNTA_AMARILLA',  'Blanco punta amarilla',    2),
    ('AMARILLO',               'Amarillo',                 3),
    ('AMARILLO_PUNTA_VERDE',   'Amarillo punta verde',     4),
    ('VERDE',                  'Verde',                    5),
    ('VERDE_PUNTA_AZUL',       'Verde punta azul',         6),
    ('AZUL',                   'Azul',                     7),
    ('AZUL_PUNTA_ROJO',        'Azul punta rojo',          8),
    ('ROJO',                   'Rojo',                     9),
    ('ROJO_PUNTA_NEGRO',       'Rojo punta negro',        10),
    ('NEGRO_1_DAN',            'Negro 1.º Dan',           11),
    ('NEGRO_2_DAN',            'Negro 2.º Dan',           12),
    ('NEGRO_3_DAN',            'Negro 3.º Dan',           13),
    ('NEGRO_4_DAN',            'Negro 4.º Dan',           14),
    ('NEGRO_5_DAN',            'Negro 5.º Dan',           15),
    ('NEGRO_6_DAN',            'Negro 6.º Dan',           16),
    ('NEGRO_7_DAN',            'Negro 7.º Dan',           17),
    ('NEGRO_8_DAN',            'Negro 8.º Dan',           18),
    ('NEGRO_9_DAN',            'Negro 9.º Dan',           19);
