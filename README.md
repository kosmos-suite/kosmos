# Kosmos

Self-hosted media manager (movies, TV, anime). Quarkus backend, React/Vite frontend
bundled via Quinoa, WASM plugin host for metadata sources.

## Requirements

- JDK 21
- Node 20+ (for the `src/main/webui` frontend, run via Quinoa)
- PostgreSQL (default datasource — see `src/main/resources/application.properties`)
- A [TMDB API key](https://www.themoviedb.org/settings/api) for movie search — set
  `KOSMOS_METADATA_TMDB_API_KEY` (env var) or `-Dkosmos.metadata.tmdb.api-key=...`.
  Without it, `/metadata/search` fails with a clear error; the rest of the app still works.
- A library root path for imports — set `KOSMOS_LIBRARY_ROOT_PATH` (env var) or
  `-Dkosmos.library.root-path=...`. Without it, `/movies/{id}/import` fails with a clear
  error; the rest of the app still works. For hardlinks to actually work (rather than
  silently falling back to copy), this path needs to be on the same filesystem as your
  download client's save path.

## Running in dev mode

```shell
./gradlew quarkusDev
```

Dev UI: <http://localhost:8080/q/dev/>
OpenAPI: <http://localhost:8080/openapi>

## Building

```shell
./gradlew build
```

Native image:

```shell
./gradlew build -Dquarkus.native.enabled=true
```

## Running the whole stack in Docker

Builds the app and the frontend, then runs Kosmos and a Postgres container together
via Compose.

```shell
./gradlew build -x test
docker compose up -d --build
```

Kosmos: <http://localhost:8080>
OpenAPI: <http://localhost:8080/openapi>

Stop with `docker compose down` (add `-v` to also drop the Postgres data volume).

Schema changes during development aren't versioned yet (see below) — after pulling a
change to `V1__movies.sql`, run `docker compose down -v` before `up` again, or Flyway
will fail with a checksum-mismatch error against the old volume.

Defaults to `Dockerfile.jvm`. To build the native image instead:

```shell
KOSMOS_DOCKERFILE=Dockerfile.native docker compose up -d --build
```

## Project layout

Feature-first packages under `src/main/java/de/oppahansi/kosmos/`. Each feature owns
its own resource, service, and repository classes — no shared `api/`/`service/` split.

Flyway migrations: `src/main/resources/db/migration/`.
