# DECISIONS.md — Cami-Do

Reglas de producto ya cerradas. `AGENTS.md` dice cómo trabajar. `PROGRESS.md` dice en qué estado está cada módulo.

## Cuenta e intención de incorporación

La cuenta personal no tiene un rol global. Una persona puede ser alumna en una institución, profesora en otra y organizadora en otra. Los roles ORGANIZADOR, PROFESOR y ALUMNO pertenecen a la membresía usuario-institución.

Al crear la cuenta se elige un recorrido inicial. Es una intención de incorporación, no un permiso.

1. **ALUMNO:** debe seleccionar obligatoriamente su categoría o cinturón actual.
2. **PROFESOR:** debe seleccionar obligatoriamente su categoría o cinturón actual.
3. **ORGANIZADOR:** puede seleccionar una categoría, pero no es obligatorio.

Esa intención se envía en `POST /api/usuarios/registro` para validar el alta. No se guarda en `Usuario` y no crea una membresía. Guardarla ahora como único valor de la cuenta la convertiría en un rol global. La incorporación real se modelará después, sobre membresías.

## Categoría declarada

La categoría informada en el registro es DECLARADA. No es un cinturón reconocido por una institución.

Se guarda en `declaraciones_categoria`, con referencia al usuario y al catálogo ITF. No se escribe en `Usuario`. No otorga roles ni habilita notas oficiales.

El catálogo sale de `GET /api/categorias`. No se duplica en Angular.

## Verificación de email pausada solo en local (temporal)

La verificación de correo sigue obligatoria por defecto y en cualquier arranque que no sea el perfil Spring `local`. El login exige `email_verificado=true`, y el alta intenta enviar el enlace con el proveedor ya configurado (Resend o ninguno).

Para seguir el MVP sin dominio ni correo real se puede pausar esa obligación, de forma explícita y solo en desarrollo local:

- Perfil `local`, sin `prod` ni `production`.
- `CAMIDO_VERIFICACION_EMAIL_OBLIGATORIA=false`.

Con la pausa, una cuenta `ACTIVA` entra con la contraseña correcta aunque `email_verificado` siga en `false`. No se envía el correo, no se crean tokens nuevos y no se cambia `email_verificado`. BCrypt, sesión, CSRF y las validaciones del registro no cambian. El endpoint de verificación sigue disponible.

Si esa variable vale `false` sin el perfil `local`, o junto con `prod` / `production`, el backend no arranca.

Esta excepción se retira antes de publicar. Hasta entonces no se considera cerrado el registro con correo real.
