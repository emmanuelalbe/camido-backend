# AGENTS.md — Cami-Do (backend)

## 1. Contexto del proyecto

Cami-Do es una aplicación web destinada a la gestión de instituciones de artes marciales.

El objetivo es desarrollar progresivamente una aplicación útil, segura, mantenible y escalable.

El proyecto también se utiliza como entorno de aprendizaje de programación y desarrollo asistido por inteligencia artificial.

## 2. Tecnologías

- Frontend: Angular y TypeScript (proyecto separado).
- Backend: Java 21 y Spring Boot 4.x.
- Gestión de dependencias: Maven.
- Persistencia: Spring Data JPA con Hibernate (`ddl-auto=none`).
- Migraciones: Flyway (`classpath:db/migration`).
- Base de datos: PostgreSQL alojada en Supabase (Session Pooler en desarrollo).
- Control de versiones: Git.

No asumir que las tecnologías previstas ya están configuradas.

Antes de incorporar nuevas herramientas o dependencias, explicar su utilidad y justificar su necesidad.

Credenciales de base de datos: solo variables de entorno `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. Nunca versionar secretos.

## 3. Arquitectura

- Monolito modular (MVP); evitar microservicios innecesarios.
- Multi-Tenant con PostgreSQL compartido: las instituciones son tenants; la pertenencia y los roles viven en membresías institucionales.
- Un usuario puede pertenecer a varias instituciones.
- Roles aprobados (`membresia_roles`) separados de solicitudes pendientes (`membresia_rol_solicitudes`); las solicitudes NUNCA otorgan permisos.
- En cada operación el backend DEBE validar membresía `ACTIVA`, institución del tenant y roles únicamente desde `membresia_roles`.
- Las claves foráneas refuerzan integridad; no sustituyen las verificaciones de autorización del backend.
- Catálogo global de categorías ITF; no pertenecen a `usuarios`.
- Una cuenta nueva se crea con estado `ACTIVO` y `emailVerificado=false`. El estado administrativo y la verificación del correo son independientes. La autenticación futura debe exigir el correo verificado solo en las operaciones que lo requieran.
- Verificación de email: token de un solo uso (hash SHA-256 en BD, 24 h). El reenvío no enumera cuentas. `camido.email.provider=none` solo fuera de producción; en prod exigir Resend (u otro) vía variables de entorno. Sin proveedor real el token puede emitirse pero el correo no se considera enviado.
- Separar responsabilidades; respetar la estructura existente.
- No implementar funcionalidades futuras sin autorización.

## 4. Reglas para agentes de IA

- Al comenzar una sesión: leer `AGENTS.md`, `SPEC.md` (si existe) y `PROGRESS.md` antes de trabajar.
- Antes de una funcionalidad importante, presentar un plan y esperar autorización.
- Dividir el trabajo en etapas pequeñas y verificables.
- No marcar tareas como completadas sin verificarlas.
- Actualizar `PROGRESS.md` al terminar cada etapa con resultados reales.
- No modificar archivos fuera del alcance acordado.
- No agregar dependencias innecesarias ni refactorizar sin pedido.
- No eliminar archivos ni código sin autorización.
- No asumir requisitos no documentados; si falta `SPEC.md`, informar y usar solo lo confirmado.
- No leer secretos desde procesos, archivos ajenos al alcance, ni el entorno de otras sesiones sin autorización explícita.
- No controlar el escritorio del usuario sin autorización explícita.
- Migraciones: no usar `baseline`/`clean` ni SQL destructivo salvo autorización explícita; detenerse si la base de destino no se puede confirmar o hay tablas inesperadas.

## 5. Organización del workspace

Cami-Do tiene dos proyectos independientes abiertos en un workspace de Cursor:

- Backend: Java / Spring Boot (este `AGENTS.md`).
- Frontend: Angular / TypeScript.

Reglas:

- No modificar el frontend desde tareas del backend sin autorización.
- No asumir una única carpeta raíz compartida para ambos.
- Mantener separadas configuraciones y dependencias.
- Comunicación frontend ↔ backend mediante API REST.
