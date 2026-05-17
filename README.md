# portfolioBackend

Backend del portfolio y del proyecto ERP, construido con **Spring Boot 3** y **Java 23**.

Expone una API REST centrada en autenticacion JWT, verificacion de email, reset de password, formulario de contacto y notificaciones por correo.

---

## Stack

| Capa | Tecnologia |
|---|---|
| Framework | Spring Boot 3.4, Spring Security, Spring Web |
| Autenticacion | JWT (OAuth2 Resource Server) |
| Base de datos | PostgreSQL (produccion) · H2 en memoria (tests) |
| ORM | Spring Data JPA / Hibernate |
| Mail | Spring Mail · Gmail SMTP |
| Tests | JUnit 5 · H2 |
| Build | Maven 3 |

---

## Requisitos previos

- Java 23+
- Maven 3.9+
- PostgreSQL 15+ (instancia local o remota)
- Cuenta de Gmail con una contrasena de aplicacion de 16 digitos

---

## Configuracion

Crea un fichero `.env` en la raiz del proyecto con las variables siguientes.
La aplicacion lo carga automaticamente en perfil `dev` y `prod`.

```properties
# Base de datos
DB_USER=tu_usuario_postgres
DB_PASSWORD=tu_password_postgres

# JWT
# Cadena Base64 de al menos 256 bits (32 bytes).
# Puedes generarla con: openssl rand -base64 32
APP_JWT_SECRET=tu_secreto_base64

# Gmail SMTP
MAIL_USERNAME=tu_cuenta@gmail.com
MAIL_PASSWORD=xxxx_xxxx_xxxx_xxxx

# URLs de verificacion de email
APP_VERIFY_EMAIL_BACKEND_URL=http://localhost:8080/auth/verify-email
APP_VERIFY_EMAIL_SUCCESS_URL=http://localhost:3000/proyecto?emailVerified=1
APP_VERIFY_EMAIL_ERROR_URL=http://localhost:3000/proyecto?emailVerifyError=1

# Notificaciones al propietario
APP_OWNER_NOTIFICATION_EMAIL=tu_email@dominio.com
```

> El fichero `.env` esta en `.gitignore`. No lo subas al repositorio.

---

## Lanzar en local

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

---

## Ejecutar tests

Los tests usan H2 en memoria, no necesitan PostgreSQL ni `.env`.

```bash
mvn test
```

---

## Estructura del proyecto

```text
src/
├── main/java/com/portfolioBackend/
│   ├── auth/           # Registro, login, verificacion de email y reset de password
│   ├── contact/        # Formulario de contacto del portfolio
│   ├── notifications/  # Avisos por email al propietario
│   └── security/       # SecurityConfig, JWTService, JwtUtils
└── test/
    └── ...
```

---

## Variables de entorno

| Variable | Obligatoria | Descripcion |
|---|---|---|
| `DB_USER` | Si | Usuario de PostgreSQL |
| `DB_PASSWORD` | Si | Password de PostgreSQL |
| `APP_JWT_SECRET` | Si | Secreto Base64 para firmar los JWT |
| `MAIL_USERNAME` | Si | Cuenta Gmail para envio de emails |
| `MAIL_PASSWORD` | Si | Contrasena de aplicacion Gmail |
| `APP_VERIFY_EMAIL_BACKEND_URL` | No | URL backend para verificar email |
| `APP_VERIFY_EMAIL_SUCCESS_URL` | No | URL frontend tras verificacion correcta |
| `APP_VERIFY_EMAIL_ERROR_URL` | No | URL frontend tras error de verificacion |
| `APP_OWNER_NOTIFICATION_EMAIL` | No | Email que recibe avisos de registro y contacto |

---

## Notas

- La base de datos se crea y actualiza automaticamente con `ddl-auto: update`.
- El schema de Hibernate se llama `portfolioBackend_schema`. Puedes cambiarlo en `application.yml`.
