-- V2: tokens de verificación de email
-- Solo CREATE. Sin DROP, TRUNCATE, DELETE ni ALTER de tablas ajenas.
-- El vencimiento de 24 horas lo aplica el servicio; aquí solo se persisten las fechas.
-- token_hash: SHA-256 del token en claro (64 caracteres hex). Nunca el token original.

CREATE TABLE tokens_verificacion_email (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      UUID         NOT NULL,
    token_hash      VARCHAR(64)  NOT NULL,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    vence_en        TIMESTAMPTZ  NOT NULL,
    usado_en        TIMESTAMPTZ  NULL,
    invalidado_en   TIMESTAMPTZ  NULL,
    CONSTRAINT uq_tokens_verificacion_email_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_tokens_verificacion_email_vence
        CHECK (vence_en > creado_en),
    CONSTRAINT chk_tokens_verificacion_email_usado
        CHECK (usado_en IS NULL OR usado_en >= creado_en),
    CONSTRAINT chk_tokens_verificacion_email_invalidado
        CHECK (invalidado_en IS NULL OR invalidado_en >= creado_en),
    CONSTRAINT chk_tokens_verificacion_email_estado
        CHECK (usado_en IS NULL OR invalidado_en IS NULL),
    CONSTRAINT fk_tokens_verificacion_email_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT
);

-- Un token pendiente por usuario (ni usado ni invalidado).
CREATE UNIQUE INDEX uq_tokens_verificacion_email_pendiente
    ON tokens_verificacion_email (usuario_id)
    WHERE usado_en IS NULL AND invalidado_en IS NULL;

-- Consulta de reenvíos: historial por usuario ordenado por creación.
CREATE INDEX ix_tokens_verificacion_email_usuario_creado
    ON tokens_verificacion_email (usuario_id, creado_en DESC);
