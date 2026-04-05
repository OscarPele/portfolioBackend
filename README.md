# portfolioBackend

Backend del portfolio de Òscar Pelegrina, construido con **Spring Boot 3** y **Java 23**.

Expone una API REST con autenticación JWT, CRUD de tareas, chat en tiempo real con IA y documentación OpenAPI/Swagger.

---

## Stack

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot 3.4, Spring Security, Spring WebSocket |
| Autenticación | JWT (OAuth2 Resource Server) |
| Base de datos | PostgreSQL (producción) · H2 en memoria (tests) |
| ORM | Spring Data JPA / Hibernate |
| Documentación | springdoc-openapi (Swagger UI en `/swagger-ui.html`) |
| Mail | Spring Mail · Gmail SMTP |
| IA | DeepSeek API |
| Tests | JUnit 5 · Mockito · MockMvc · H2 |
| Build | Maven 3 |

---

## Requisitos previos

- Java 23+
- Maven 3.9+
- PostgreSQL 15+ (instancia local o remota)
- Cuenta de Gmail con una [contraseña de aplicación](https://support.google.com/accounts/answer/185833) de 16 dígitos
- (Opcional) Clave de API de [DeepSeek](https://platform.deepseek.com/) si quieres usar el chat con IA

---

## Configuración

Crea un fichero `.env` en la raíz del proyecto con las variables siguientes.  
La aplicación lo carga automáticamente en perfil `dev` y `prod`.

```properties
# ── Base de datos ──────────────────────────────────────
DB_USER=tu_usuario_postgres
DB_PASSWORD=tu_password_postgres

# ── JWT ────────────────────────────────────────────────
# Cadena Base64 de al menos 256 bits (32 bytes).
# Puedes generarla con: openssl rand -base64 32
APP_JWT_SECRET=tu_secreto_base64

# ── Gmail SMTP ─────────────────────────────────────────
MAIL_USERNAME=tu_cuenta@gmail.com
MAIL_PASSWORD=xxxx_xxxx_xxxx_xxxx   # contraseña de aplicación (16 dígitos)

# ── URLs de verificación de email ──────────────────────
# Ajusta a tu dominio si despliegas en producción.
APP_VERIFY_EMAIL_BACKEND_URL=http://localhost:8080/auth/verify-email
APP_VERIFY_EMAIL_SUCCESS_URL=http://localhost:3000/demos/1?emailVerified=1
APP_VERIFY_EMAIL_ERROR_URL=http://localhost:3000/demos/1?emailVerifyError=1

# ── DeepSeek (opcional, solo para el chat con IA) ──────
APP_DEEPSEEK_API_KEY=sk-...
APP_DEEPSEEK_BASE_URL=https://api.deepseek.com
APP_DEEPSEEK_MODEL=deepseek-chat
```

> El fichero `.env` ya está en `.gitignore`. No lo subas al repositorio.

---

## Lanzar en local

```bash
# 1. Clona el repositorio
git clone https://github.com/OscarPele/portfolioBackend.git
cd portfolioBackend

# 2. Crea el fichero .env con tus variables (ver sección anterior)

# 3. Arranca con el perfil dev (carga el .env automáticamente)
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.  
Swagger UI en `http://localhost:8080/swagger-ui.html`.

---

## Ejecutar los tests

Los tests usan H2 en memoria, no necesitan PostgreSQL ni `.env`.

```bash
# Todos los tests
mvn test

# Solo los tests del CRUD de tareas
mvn -Dtest="TaskServiceTest,TaskCreateControllerTest,TaskGetAllControllerTest,TaskUpdateControllerTest,TaskDeleteControllerTest" test
```

---

## Estructura del proyecto

```
src/
├── main/java/com/portfolioBackend/
│   ├── auth/           # Registro, login, verificación de email, reset de password
│   ├── CRUD/           # CRUD de tareas + documentación OpenAPI
│   │   └── dto/        # DTOs de request/response y errores
│   ├── AIChat/         # Chat en tiempo real con WebSocket + DeepSeek
│   ├── config/         # OpenApiConfig
│   └── security/       # SecurityConfig, JWTService, JwtUtils
└── test/java/com/portfolioBackend/
    └── CRUD/           # Tests unitarios (Mockito) e integración (MockMvc)
```

---

## Variables de entorno — referencia completa

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_USER` | Sí | Usuario de PostgreSQL |
| `DB_PASSWORD` | Sí | Password de PostgreSQL |
| `APP_JWT_SECRET` | Sí | Secreto Base64 para firmar los JWT |
| `MAIL_USERNAME` | Sí | Cuenta Gmail para envío de emails |
| `MAIL_PASSWORD` | Sí | Contraseña de aplicación Gmail (16 dígitos) |
| `APP_VERIFY_EMAIL_BACKEND_URL` | No | URL backend para verificar email (default: localhost) |
| `APP_VERIFY_EMAIL_SUCCESS_URL` | No | URL frontend tras verificación correcta |
| `APP_VERIFY_EMAIL_ERROR_URL` | No | URL frontend tras error de verificación |
| `APP_DEEPSEEK_API_KEY` | No | Clave DeepSeek para el chat con IA |
| `APP_DEEPSEEK_BASE_URL` | No | Base URL de DeepSeek (default: `https://api.deepseek.com`) |
| `APP_DEEPSEEK_MODEL` | No | Modelo DeepSeek a usar (default: `deepseek-chat`) |

---

## Notas

- La base de datos se crea y migra automáticamente con `ddl-auto: update`. No hay scripts SQL que ejecutar manualmente.
- El schema de Hibernate se llama `portfolioBackend_schema`. Puedes cambiarlo en `application.yml`.
- Sin `APP_DEEPSEEK_API_KEY`, el módulo de chat simplemente no conectará con la IA, pero el resto de la aplicación funciona con normalidad.
