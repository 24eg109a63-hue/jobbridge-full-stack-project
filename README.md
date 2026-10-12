# JobBridge

JobBridge is split into two independently running applications:

- `frontend/`: React + Vite user interface on `http://localhost:5173`
- `src/`: Spring Boot JSON API on `http://localhost:8080`

The backend stores users, jobs, and applications in MySQL. The frontend talks to the backend over JSON HTTP requests and uses a secure session cookie for login.

## Requirements

- Java 17+
- Maven 3.9+
- Node.js 20+ and npm
- MySQL 8+ (optional when using the default local demo database)

## Run the backend

The backend uses an embedded, file-backed H2 database by default, so you can try signup and the app without installing MySQL. From the project root, run:

```sh
mvn spring-boot:run
```

The API is available at `http://localhost:8080/api`. Local database files are stored under `data/`.

### Create an administrator

Admin accounts are provisioned from environment variables; public registration can only create regular accounts. Stop the backend, then start it with a new admin email and a password of at least 12 characters:

```sh
ADMIN_EMAIL='admin@example.com' \
ADMIN_PASSWORD='use-a-unique-password-of-at-least-12-chars' \
mvn spring-boot:run
```

The account is created once in the configured database. Sign in through the frontend; administrators get an **Admin** navigation button to edit listings and remove or restore them from public search. Set a fresh admin email if that address already belongs to a regular account. Do not commit real admin credentials to source control.

### Use MySQL instead

Create the database:

```sh
mysql -u root -p -e 'CREATE DATABASE jobbridge;'
```

Set your actual MySQL credentials and select the MySQL profile:

```sh
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL='jdbc:mysql://127.0.0.1:3306/jobbridge?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USER='your_mysql_user'
export DB_PASSWORD='your_mysql_password'
mvn spring-boot:run
```

## Run the frontend

In a second terminal:

```sh
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

If the API is hosted at a different address, set `VITE_API_URL` before starting Vite, and set `APP_FRONTEND_ORIGIN` for the backend to the frontend's full origin:

```sh
VITE_API_URL='http://localhost:8080' npm run dev
```

```sh
APP_FRONTEND_ORIGIN='http://localhost:5173' mvn spring-boot:run
```

## API overview

- `GET /api/jobs?q=keyword` — list and search public jobs
- `GET /api/jobs/{id}` — get a public job
- `GET /api/auth/csrf` — obtain a CSRF token before changing data
- `POST /api/auth/register` — create an account
- `POST /api/auth/login` — start a session
- `GET /api/auth/me` — get the signed-in account
- `POST /api/auth/logout` — end the current session
- `POST /api/jobs` — publish a job (authenticated)
- `POST /api/jobs/{id}/applications` — apply for a job (authenticated)
- `GET /api/applications` — list the signed-in user's applications
- `GET /api/admin/jobs` — list all jobs (admin only)
- `PUT /api/admin/jobs/{id}` — edit a job listing (admin only)
- `PATCH /api/admin/jobs/{id}/publication` — remove or restore a job listing in public search (admin only)

During local development, the backend serves API responses and the React frontend runs independently.

## Deploy on Railway

The root `Dockerfile` builds the React frontend and serves it from the Spring Boot application, so the UI and API share one HTTPS origin. In Railway, create a project from this GitHub repository and deploy the repository root. Then configure these service settings:

- Add a persistent volume mounted at `/var/data` so the H2 database survives redeploys.
- Set `SPRING_DATASOURCE_URL` to `jdbc:h2:file:/var/data/jobbridge;AUTO_SERVER=TRUE`.
- Set `SPRING_DATASOURCE_USERNAME` to `sa`, `SPRING_DATASOURCE_PASSWORD` to an empty value, and `SPRING_DATASOURCE_DRIVER_CLASS_NAME` to `org.h2.Driver`.
- Set `SPRING_JPA_HIBERNATE_DDL_AUTO` to `update` and `SERVER_SERVLET_SESSION_COOKIE_SECURE` to `true`.
- Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` to the administrator credentials you want provisioned on first startup. Use a unique password of at least 12 characters; enter it as a Railway secret and do not commit it.
- Generate a Railway public domain. Railway supplies `PORT` automatically, and the container uses it.

`railway.toml` configures the Docker build and `/api/jobs` health check. The API and frontend use the same origin, so no separate CORS origin or frontend API URL is needed.

This is a student project starter, not a production deployment. Before production, configure HTTPS, managed secrets, rate limiting, email verification, audit logging, backups, and an administrator provisioning process.
