# Security Policy

## Supported versions

Security fixes are applied to the latest release on the default branch. Until the project
reaches a stable release, older snapshots are not supported.

## Reporting a vulnerability

Use GitHub private vulnerability reporting for this repository. If it is not enabled, ask the
maintainers to enable it without publishing exploit details. Do not open a public issue that
contains credentials, tokens, private infrastructure details, or a working exploit.

Hevy API keys, OAuth access or refresh tokens, OAuth client secrets, and deployment `.env`
files must never be committed. Revoke and rotate any credential that may have been exposed.
