# PROGRESS.md — Cami-Do

Última actualización: 2026-11-23

## Tecnologías configuradas

- Backend: Java 21, Spring Boot 4.1.2, Maven.
- Persistencia: Spring Data JPA + Hibernate (`ddl-auto=none`).
- Flyway + PostgreSQL (Supabase Session Pooler).
- Datasource vía `DB_*` (archivo local `.env` gitignored).
- Frontend: Angular 22 + TypeScript + CSS tradicional (`camido-frontend`).
- Correo: Resend (`EMAIL_PROVIDER=resend`) + `spring-boot-starter-restclient`.

## Módulo completado: Registro y verificación de correo

**Estado:** implementado, probado y validado de extremo a extremo (2026-11-23).

Flujo manual comprobado:

1. Registro exitoso vía `POST /api/usuarios/registro` (HTTP 201, solo UUID).
2. Persistencia correcta del usuario en PostgreSQL (Supabase): `ACTIVO`, `email_verificado=false` al alta.
3. Generación de token seguro (en claro solo en el correo; en BD únicamente hash SHA-256).
4. Envío y recepción del correo con Resend (desde `onboarding@resend.dev` en desarrollo).
5. Apertura del enlace desde el cliente de correo hacia Angular (`/verificar-email`).
6. Verificación exitosa Angular ↔ Spring Boot → UI «Correo verificado» y `email_verificado=true` en PostgreSQL.
7. Reapertura del mismo enlace → UI «Inválido o vencido» (token de un solo uso; queda con `usado_en`).

Verificaciones de calidad asociadas al módulo:

| Verificación | Resultado |
|---|---|
| Backend Java 21 + Spring Boot 4.1.2 | Arranque y endpoints OK |
| Frontend Angular 22 | `ng serve` + ruta `/verificar-email` OK |
| Flyway V1 y V2 | Aplicadas (`success=true`); schema en versión 2 |
| Build Angular development | OK |
| Build Angular production | OK (`apiBaseUrl=/api`, sin localhost) |
| Pruebas frontend (Vitest) | **5/5** OK (`verificar-email.spec.ts`) |
| Pruebas backend relevantes | Registro + verificación (unitarias/controller) e integración PostgreSQL OK |

## Verificado (infra / esquema)

- Conexión a Supabase Session Pooler / DB `postgres`: OK.
- `flyway_schema_history`: V1 `create core tables` y V2 tokens de verificación de email, ambas con `success=true`.
- Tablas en `public` (entre otras): `usuarios`, `instituciones`, `membresias`, `membresia_roles`, `membresia_rol_solicitudes`, `categorias`, `tokens_verificacion_email` (+ `flyway_schema_history`).
- Categorías ITF: **19** (incluye «Azul punta rojo» y «Rojo punta negro»).
- Solicitudes: columnas `resuelto_por_usuario_id`, `motivo_decision` presentes.
- Instituciones: índice `uq_instituciones_codigo_lower` (`LOWER(codigo)`).

## Nota histórica

- Un arranque de validación falló solo por **puerto 8080 ocupado**; Flyway ya había validado bien antes de ese error.
- El arranque tras Resend falló primero por credenciales DB y luego por falta de `RestClient.Builder`; se resolvió con `spring-boot-starter-restclient` (sin beans manuales).

## SPEC.md

- No encontrado.

## Entidades JPA

| Entidad | Estado |
|---|---|
| `Usuario` (`com.camido.backend.usuario`) | Alineada con V1; `fechaRegistro` con `@CreationTimestamp`; `UsuarioEstado` = ACTIVO/INACTIVO/SUSPENDIDO. Compilación OK. |
| `UsuarioRepository` | `JpaRepository<Usuario, UUID>`; búsquedas por email case-insensitive; marcado de email verificado. |
| `TokenVerificacionEmail` + repositorio | Alineados con V2; hash, vigencia, usado/invalidado. |
| Controllers / services | `RegistroUsuarioController` / `RegistroUsuarioService`; `VerificacionEmailController` / `VerificacionEmailService`. |

## Seguridad (parcial)

| Ítem | Estado |
|---|---|
| `spring-boot-starter-security` | Sesión HTTP + CSRF + BCrypt. Sin JWT. |
| Bean `PasswordEncoder` (`BCryptPasswordEncoder`) | En `PasswordEncoderConfig`. |
| Verificación de email | Token de un solo uso; sin loguear token ni secretos. |
| Login / sesión | Implementado y probado en automático. E2E en navegador **pendiente**. |

## Registro (detalle histórico)

| Ítem | Estado |
|---|---|
| `spring-boot-starter-validation` | Agregado. |
| `RegistroUsuarioRequest` | Password: mínimo 8 caracteres y máximo 72 bytes UTF-8. JSON de entrada deserializa `password`; la salida no lo incluye. |
| `RegistroUsuarioService` | Cuenta nueva: `ACTIVO` y `emailVerificado=false` (decisión en `AGENTS.md`). Hash BCrypt. Duplicados → 409. |
| `RegistroUsuarioController` | `POST /api/usuarios/registro`: 201 (UUID), 409, 400. Sin exponer hash ni detalles de PostgreSQL. |
| Pruebas | `RegistroUsuarioServiceTest` y `RegistroUsuarioControllerTest` OK (2026-11-23). |
| Registro E2E temprano | 2026-11-23: alta con email sintético de ejemplo; 409 al reintentar; 400 con datos inválidos. Usuario sintético no eliminado aún. |

## Verificación de email (backend — detalle)

| Ítem | Estado |
|---|---|
| Diseño | Token SHA-256, un pendiente por usuario, 24 h. |
| Flujo | Emisión tras registro; verificar/reenviar. UPDATE atómico + `emailVerificado` en la misma `@Transactional`. Envío **después del commit**. |
| Reenvío | Lock pesimista; límites 1/2 min y 5/h; respuesta genérica. |
| Correo | `NoEmailSender` solo fuera de prod; `ResendEmailSender` si `EMAIL_PROVIDER=resend`. |
| CORS | `http://localhost:4200` con credenciales; headers `Content-Type`, `Accept`, `X-XSRF-TOKEN`. Sin origen `*`. |
| Pruebas | Unitarias + `VerificacionEmailIntegrationTest` OK (2026-11-23). |

## Frontend Angular (`camido-frontend`)

| Ítem | Estado |
|---|---|
| Estructura | Standalone; `features/auth` (verificación, login, bienvenida). |
| Entornos | Dev y prod usan `/api`. En desarrollo `ng serve` proxea `/api` a `localhost:8080` para que la cookie de sesión sea del mismo sitio. |
| UI verificación | Estados: verificando / ok / inválido / ausente / errorRed. Token fuera de la URL tras procesar. |
| Login UI | `/login` (Reactive Forms) y `/bienvenida` protegida con guard. `AuthService` + `withCredentials`. Sin secretos en storage. |
| Strict | TypeScript `strict` + `strictTemplates`. |
| Pruebas | Vitest **11/11** OK (5 verificación + 6 login/sesión/guard). |
| Builds | Development y production OK. |
| Git | Configurado y publicado (ver sección siguiente). |

## Git y GitHub (2026-11-23)

| Ítem | Estado |
|---|---|
| Estrategia | Repositorios **independientes** (backend y frontend). |
| Backend remoto | `https://github.com/emmanuelalbe/camido-backend.git` |
| Frontend remoto | `https://github.com/emmanuelalbe/camido-frontend.git` |
| Rama | `main` en ambos, con seguimiento a `origin/main`. |
| Commit inicial backend | `chore: initial backend baseline` (publicado). |
| Commit inicial frontend | `chore: initial frontend baseline` (publicado). |
| Seguridad | `.env` / secretos excluidos; `target/`, `node_modules/`, `dist/`, `.angular/`, `.cursor/` ignorados; `.env.example` solo con placeholders. Sin `push --force`. |

## Login y sesión (2026-11-23)

**Implementado y probado automáticamente. No validado E2E en el navegador.**

| Ítem | Estado |
|---|---|
| Estrategia | Sesión de servidor (`JSESSIONID` HttpOnly, `SameSite=Lax`). `Secure` solo en perfil `prod`. CSRF con cookie `XSRF-TOKEN` (no HttpOnly) y header `X-XSRF-TOKEN`. |
| Endpoints | `GET /api/auth/csrf`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`. |
| Reglas | Email normalizado. Un mismo mensaje si no existe, contraseña incorrecta, email no verificado o cuenta no activa. Límite en memoria: 5 fallos / 15 min por IP+email (una sola instancia). |
| Públicos | Registro, verificación, reenvío, saludo y login. El resto de `/api/**` exige sesión. |
| Sesión | `changeSessionId` al autenticar. Logout invalida la sesión. No hay tabla nueva ni cambios a V1/V2. |
| Pruebas backend | `AuthServiceTest` + `AuthControllerTest` OK (login, rechazo genérico, 401 sin sesión, logout, CSRF 403, límite 429). Registro y verificación de controllers/servicios sin regresión. |
| E2E navegador | **Pendiente.** Hace falta autorización para usar o crear una cuenta de prueba. |

## Pendientes

1. Validar login/logout en el navegador (E2E) con una cuenta de prueba autorizada.
2. Revisar y limpiar usuarios de prueba, **previa autorización**.
3. **Próximo módulo sugerido:** recuperación de contraseña (todavía no implementada).
4. Instituciones / roles / evaluaciones: fuera del alcance inmediato.
5. El límite de login es en memoria: no se comparte entre varias instancias.
