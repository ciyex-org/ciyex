# Ciyex: Fullstack Healthcare Platform

This repository contains:

- **Ciyex Backend:** Spring Boot (Java 21+), Gradle, MySQL
- **Ciyex UI:** Next.js (React 19+), pnpm, Tailwind CSS

---

## Prerequisites

- [Java 21+](https://adoptium.net/)
- [Node.js 20+](https://nodejs.org/en/download/)
- [pnpm](https://pnpm.io/installation)
- [MySQL](https://dev.mysql.com/downloads/mysql/) or MariaDB
- [Gradle](https://gradle.org/) (or use the included Gradle wrapper)
- [Docker](https://www.docker.com/) *(optional, for local DB or fullstack deployment)*

---

## Project Structure

```

ciyex/
├── ciyex-ui/              # Next.js 15+ frontend (pnpm)
│   ├── src/
│   ├── package.json
│   ├── pnpm-lock.yaml
│   └── ...
├── src/                   # Spring Boot backend source
├── build.gradle
├── settings.gradle
└── ...

````

---

## 1. Backend Setup (Spring Boot)

### a. Database

1. **Start MySQL** (default port: 3306).
2. **Create a database and user** for Ciyex (or use existing).
3. **Configure your DB settings** in `application.yml` (or `application.properties`):

    ```yaml
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/ciyex
        username: ciyex
        password: yourpassword
    ```

4. *(Optional)* Import initial schema if you have SQL files provided.

---

### b. Build & Run the Backend

```sh
# From the ciyex/ root directory
./gradlew build         # Compiles and runs tests
./gradlew bootRun       # Starts Spring Boot at http://localhost:8080
````

---

## 2. Frontend Setup (`ciyex-ui`)

### a. Install dependencies

```sh
cd ciyex-ui
pnpm install
```

*If you see warnings about multiple lockfiles, delete `package-lock.json` and `yarn.lock`. Only keep `pnpm-lock.yaml`.*

### b. Configure environment variables

Copy or create `.env.local` in `ciyex-ui/`:

```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

You can also use `.env`, `.env.stage`, etc. for other environments.

### c. Start the development server

```sh
pnpm run dev
```

Frontend runs at: [http://localhost:3000](http://localhost:3000)

---

## 3. Build for Production

### Frontend

```sh
cd ciyex-ui
pnpm run build
pnpm run start
```

### Backend

```sh
./gradlew build
java -jar build/libs/ciyex-*.jar
```

---

## 4. Common Troubleshooting

* **Module not found**:
  Double check import paths and rerun `pnpm install`.
* **Lockfile warnings**:
  Run `rm package-lock.json yarn.lock` in both root and `ciyex-ui`.
* **Next.js Suspense or CSR errors**:
  Wrap any page using hooks like `useSearchParams` in a `<Suspense>` boundary.
* **Database connection issues**:
  Make sure MySQL is running and matches your configuration.
* **Port in use**:
  Check that ports `8080` (backend) and `3000` (frontend) are free.

---

## 5. Linting and Formatting

```sh
# Frontend (in ciyex-ui)
pnpm run lint
pnpm run format

# Backend: use your IDE or Java linter plugins
```

---

## 6. Running Tests

```sh
# Frontend
pnpm run test

# Backend
./gradlew test
```

---

## 7. Useful Scripts

* **Frontend (`ciyex-ui`):**

    * `pnpm run dev` — Dev server (hot reload)
    * `pnpm run build` — Production build
    * `pnpm run lint` — Lint code
    * `pnpm run start` — Run built production server

* **Backend:**

    * `./gradlew bootRun` — Dev server (auto reload)
    * `./gradlew build` — Build JAR file
    * `./gradlew test` — Run tests

---

## 8. Docker Compose (Optional)

You can use Docker Compose to run MySQL, the backend, and the frontend together.

Ask your team for a sample `docker-compose.yml` if you want a containerized setup.

---

## 9. FAQ

* **Can I use npm/yarn for the frontend?**
  Prefer pnpm for consistency (and no lockfile warnings).
* **How do I reset my DB?**
  Use `DROP DATABASE` and recreate, or run your schema SQL scripts.
* **Who do I contact for onboarding?**
  Open an issue or ask your dev team!

---

## 10. Contributing

1. Fork this repo
2. Create your feature branch: `git checkout -b my-feature`
3. Commit your changes
4. Push to your branch
5. Open a pull request

---

## 🚀 Happy Coding with Ciyex!

---
