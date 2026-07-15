# Hevy MCP Server

An open-source, authenticated Model Context Protocol (MCP) server for the Hevy API. It exposes
five focused workout and routine tools over Streamable HTTP and protects them as an OAuth 2.1
resource server.

The application uses Java 21, Spring Boot 4.1.0, Spring AI 2.0.0, Spring Security, and Maven.
The MCP endpoint is `/mcp`.

> This project is not affiliated with, endorsed by, or sponsored by Hevy.

## Architecture

```text
External Authorization Server
          |
          | OAuth access token
          v
      MCP Client
          |
          | Bearer access token
          v
    Hevy MCP Server
          |
          | Hevy API key
          v
       Hevy API
```

The external authorization server issues a JWT access token to the MCP client. This server
validates its signature, timestamps, issuer, audience, and scopes. Separately, the server uses a
private Hevy API key for upstream calls. The OAuth token is never forwarded to Hevy, and the Hevy
key is never returned to the MCP client.

OAuth client credentials belong in the MCP client, not this resource server. This application
does not need or accept an OAuth client ID or client secret.

## MCP tools

| Tool | Capability | Required scope |
| --- | --- | --- |
| `get_workouts` | Read a paginated workout list | `read:workouts` |
| `get_workout` | Read one workout by ID | `read:workouts` |
| `get_routines` | Read a paginated routine list | `read:routines` |
| `get_routine` | Read one routine by ID | `read:routines` |
| `update_routine` | Replace an existing routine definition | `write:routines` |

Hevy limits workout and routine pages to 10 items. The list tools default to page 1 and 10 items.
Read a routine before updating it: Hevy's update operation accepts a replacement routine payload,
so callers should preserve every exercise and set that is not intentionally changed.

## Configuration

All deployment-specific settings are environment variables. Copy `.env.example` for local use;
never commit the resulting `.env`.

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `HEVY_API_KEY` | Yes | — | Private Hevy Pro API key |
| `HEVY_API_BASE_URL` | No | `https://api.hevyapp.com` | Hevy API origin |
| `OAUTH_ISSUER_URI` | Yes | — | Exact JWT issuer and discovery URI |
| `OAUTH_AUDIENCE` | Yes | — | Required value in the JWT `aud` claim |
| `MCP_RESOURCE_URI` | Yes | — | Public MCP resource identifier, normally ending in `/mcp` |
| `MCP_SERVER_NAME` | No | `hevy-mcp` | MCP and Spring application name |
| `SERVER_PORT` | No | `8080` | Container HTTP port |

Required values are validated during startup. Failure messages identify the missing property but
do not print secret values.

## Authorization server requirements

Configure an OAuth 2.1/OIDC authorization server that:

- issues signed JWT access tokens;
- publishes OIDC or RFC 8414 discovery metadata and a JWK set;
- uses `OAUTH_ISSUER_URI` as the exact `iss` claim;
- includes `OAUTH_AUDIENCE` in `aud`;
- can grant `read:workouts`, `read:routines`, and `write:routines`;
- supports the OAuth client registration mechanism required by your MCP client and uses PKCE for
  public authorization-code clients.

The server publishes RFC 9728 protected-resource metadata at both
`/.well-known/oauth-protected-resource` and
`/.well-known/oauth-protected-resource/mcp`. A 401 response advertises the endpoint-specific URL
through the `WWW-Authenticate` `resource_metadata` parameter. If `MCP_RESOURCE_URI` does not end
in `/mcp`, the challenge derives the RFC 9728 path from the configured resource URI; the two
committed controller paths cover the intended `/mcp` deployment.

## Local development

Prerequisites are JDK 21 and no globally installed Maven—the committed wrapper is used.

```bash
cp .env.example .env
# Replace every required placeholder, then export the file using your shell's preferred method.
./mvnw spring-boot:run
```

Useful Maven commands:

```bash
./mvnw clean test
./mvnw clean package
```

The executable jar is written under `target/`.

## Docker

Build and run directly:

```bash
docker build -t hevy-mcp .
docker run --rm -p 8080:8080 \
  -e HEVY_API_KEY \
  -e OAUTH_ISSUER_URI \
  -e OAUTH_AUDIENCE \
  -e MCP_RESOURCE_URI \
  hevy-mcp
```

Or use Compose after creating a local `.env`:

```bash
docker compose up --build
```

The runtime image uses Java 21 and a non-root user. No configuration or secret is baked into the
image. The image intentionally has no Docker `HEALTHCHECK` because the minimal runtime does not
contain a reliable HTTP probe; orchestrators can probe `GET /actuator/health`.

## Connecting an MCP client

Give an OAuth-capable MCP client the public Streamable HTTP URL, for example
`https://mcp.example/mcp`. Configure the OAuth client at the external authorization server with
the client's redirect URI and grant only the scopes it needs. The client should discover this
server's protected-resource metadata from the 401 challenge and then discover the authorization
server from that document.

The transport can use authenticated `POST`, `GET`, and session `DELETE` requests at `/mcp`. The
server is stateless from Spring Security's perspective, though the MCP Streamable HTTP transport
may maintain protocol sessions.

## Hevy API behavior

The client uses the official API operations:

- `GET /v1/workouts?page={page}&pageSize={pageSize}`
- `GET /v1/workouts/{workoutId}`
- `GET /v1/routines?page={page}&pageSize={pageSize}`
- `GET /v1/routines/{routineId}`
- `PUT /v1/routines/{routineId}`

Every upstream request uses Hevy's `api-key` header. The public Hevy API is currently documented
as a Hevy Pro feature and warns that its schema may change. This server maps upstream errors to
sanitized messages and never includes response bodies or credentials in MCP errors.

## Security considerations

- Never commit `.env`, API keys, JWTs, OAuth secrets, or private infrastructure details.
- Grant `write:routines` only to clients that should be allowed to change routines.
- Keep `MCP_RESOURCE_URI`, the authorization-server audience, and the MCP URL aligned.
- Terminate TLS before exposing the service publicly.
- Do not enable Spring Security trace logging or WebClient wire logging in production.
- Only `/mcp`, protected-resource metadata, and health are routed; there is no generic Hevy proxy.
- Actuator exposes only health and does not show details.

See [SECURITY.md](SECURITY.md) for private vulnerability reporting guidance.

## Troubleshooting

- **Startup reports a missing property:** set every required variable from the table above.
- **401 from `/mcp`:** verify the bearer JWT is signed by the configured issuer and is not expired.
- **JWT rejected despite a valid signature:** compare `iss` exactly and ensure `aud` contains
  `OAUTH_AUDIENCE`.
- **403 from a tool:** grant that tool's required scope; scopes are mapped to `SCOPE_...`
  authorities by Spring Security.
- **Hevy credentials rejected:** rotate or correct `HEVY_API_KEY`; do not paste it into an issue.
- **Hevy request invalid:** check page limits and ensure routine updates contain the complete
  intended routine structure.

## License

Apache License 2.0. See [LICENSE](LICENSE).
