# Ministry of Justice Digital Prison Reporting Domain Builder

[![repo standards badge](https://img.shields.io/endpoint?labelColor=231f20&color=005ea5&style=for-the-badge&label=MoJ%20Compliant&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fendpoint%2Fdigital-prison-reporting-domain-builder&logo=data:image/png;base64,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)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#digital-prison-reporting-domain-builder)

#### CODEOWNER

- Team : [hmpps-digital-prison-reporting](https://github.com/orgs/ministryofjustice/teams/hmpps-digital-prison-reporting)
- Email : digitalprisonreporting@digital.justice.gov.uk

## Overview

Provides frontend and backend services to support creation and management of data domains for the
digital prison reporting project.

The project is split into the following modules

- [common](common) - code that can be used by other modules
- [backend](backend) - backend REST API used by frontend code to manage domains
- [cli](cli) - cli frontend providing batch and interactive modes

This project uses gradle which is bundled with the repository and also makes use of

- [micronaut](https://micronaut.io/) - for compile time dependency injection
- [jacoco](https://docs.gradle.org/current/userguide/jacoco_plugin.html) - for test coverage reports
- [postgresql](https://www.postgresql.org/) - for data storage in the backend service
- [docker](https://www.docker.com/) - to run postgres locally
- [flyway](https://flywaydb.org/) - to manage database migrations

The project is written in Kotlin and targets Java 11.

## Local Development

### Scripts supporting local development

The following scripts have been provided to support local development.

#### [start-postgres](docker/start-postgres)

This script takes care of

- ensuring docker and docker-compose are installed
- starting docker if it's not running already
- configuring and starting a local postgres container
- creating the local `domain_builder` database if it doesn't exist already
- applying migrations
- inserting test data if the `domain` table is empty

> _Note_ [colima](https://github.com/abiosoft/colima) is also supported as an alternative to docker desktop

Usage

```shell
    ./docker/start-postgres
```

The script will run a number of checks and commands. If any of these failed you will be prompted to either install a
missing command or check the output of a failed step.

It's safe to run the script multiple times since action will only be taken where necessary, e.g. if docker is stopped
or there is a new migration to apply.

#### [run-backend](bin/run-backend)

This script checks docker is up, runs a build and then launches the backend API locally.

Usage

```shell
    ./bin/run-backend
```

By default the backend will run on `localhost:8080`.

While the API is running it will log to `stdout`.

#### [domain-builder-interactive](bin/domain-builder-interactive)

Launches the domain-builder CLI frontend in interactive mode.

Usage

```shell
    ./bin/domain-builder-interactive
```

By default the CLI frontend will attempt to connect to `http://localhost:8080`

When in interactive mode, use the `help` command to see what commands are available. These will mirror the commands
available in the batch mode domain-builder command (see below).

The interactive also provides a built in `less` implementation which is enabled when the output exceeds the size of
the terminal.

#### [domain-builder](bin/domain-builder)

Launches the domain-builder CLI in 'batch' mode.

Usage

```shell
    # See available options and commands
    ./bin/domain-builder --help

    # Get help for a specific command
    ./bin/domain-builder list --help

    # List all domains
    ./bin/domain-builder list

    # View a specific domain by name
    ./bin/domain-builder view -n Some Domain
```

Additional options are provided to enable ANSI color output and interactive mode, which are enabled by the
`domain-builder-interactive` script.

#### [push-backend-jar](bin/push-backend-jar)

Builds and pushes the backend jar to a given s3 bucket to support local development and testing of the API when run as
an AWS Lambda.

Assumes that the `aws` command is installed locally with valid credentials.

Usage

```shell
    ./bin/push-backend-jar s3://bucket/path
```

#### [apply-migrations](bin/apply-migrations)

Applies migrations to the locally running database by default by invoking the
[MigrationRunner](src/main/kotlin/uk/gov/justice/digital/MigrationRunner.kt) class which uses Flyway to apply any
outstanding migrations.

Usage

```shell
    ./bin/apply-migrations
```

This script can also be used as part of the deployment pipeline to apply migrations before pushing a new version of
the code.

### Packaging

This project makes use of the [shadow jar plugin](https://github.com/johnrengelman/shadow)
which takes care of creating a jar containing all dependencies.

```shell

    # Build all artefacts
    ./gradlew build

    # Build backend only
    ./gradlew :backend:build

    # Build cli only
    ./gradlew :cli:build
```

Executable jars are written to the following locations for each module that represents an executable service or
command

- `backend` backend/build/libs/domain-builder-backend-api-<VERSION>-all.jar
- `cli` cli/build/libs/cli-<VERSION>-all.jar

### Running locally

Use the following commands to bring up the backend services

```shell
    docker/start-postgres # optional if you wish to observe the output of this command
    bin/run-backend
```

> _Note_ The `run-backend` script will run `start-postgres` automatically so you can avoid having to run this
> command manually.

You can then launch the cli in interactive or batch mode e.g.

```shell
    bin/domain-builder-interactive
    # or
    bin/domain-builder view -n domain-name
```

## Testing

> **Note** - test coverage reports are enabled by default and after running the
> tests the report will be written to <module>/build/reports/jacoco/test/html
> Coverage is written separately for each module due to the project structure.

### Unit Tests

The unit tests use JUnit5 and Mockito where appropriate. Use the following to
run the tests.

```
    ./gradlew clean test
```

Tests for a specific module can be run by specifying the module name. For example to run the backend tests only run

```
    ./gradlew :backend:test
```

### Integration Tests

Integration tests are run as part of the overall `test` command.

The backend module makes use of [testcontainers](https://www.testcontainers.org/) to bring up a test specific instance
of postgresql used by the tests e.g.
[DomainRepositoryTest](backend/src/test/kotlin/uk/gov/justice/digital/repository/DomainRepositoryTest.kt).

Any tests using the `@TestContainers` annotation expect docker to be running or they will fail.

These can be configured to skip instead if docker is down.

### Acceptance Tests

There are no acceptance tests at the time of writing.

## Configuration

The backend and frontend code can be configured using environment as follows.

### Backend

The following environment variables are referenced in the
[backend application.yml](backend/src/main/resources/application.yml). No default values are provided in the main
configuration so the backend will fail if any of these variables are _not_ set on the environment.

- `POSTGRES_HOST`
- `POSTGRES_PORT`
- `POSTGRES_DB_NAME`
- `POSTGRES_USERNAME`
- `POSTGRES_PASSWORD`

> _Note_ The scripts set these variables with suitable values for local development.

### Cli

The following environment variable is reference in the cli [application.yml](cli/src/main/resources/application.yml).

- `DOMAIN_API_URL`

For local usage this is set to `http://localhost:8080`

## Deployment

The deployment process is TBD.

Part of this process will need to trigger the `apply-migrations` script with appropriate configuration in order to run
the migrations as part of the deployment process.

> _Note_ At the time of writing some modifications to this script will be needed to support this.

## Contributing

Please adhere to the following guidelines when making contributions to the
project.

### Documentation

- Keep all code commentary and documentation up to date

### Branch Naming

- Use a JIRA ticket number where available
- Otherwise a short descriptive name is acceptable

### Commit Messages

- Prefix any commit messages with the JIRA ticket number where available
- Otherwise use the prefix `NOJIRA`

### Pull Requests

- Reference or link any relevant JIRA tickets in the pull request notes
- At least one approval is required before a PR can be merged

## TODO

- Modify the Dependabot file to suit the [dependency manager](https://docs.github.com/en/code-security/dependabot/dependabot-version-updates/configuration-options-for-the-dependabot.yml-file#package-ecosystem) you plan to use and for [automated pull requests for package updates](https://docs.github.com/en/code-security/supply-chain-security/keeping-your-dependencies-updated-automatically/enabling-and-disabling-dependabot-version-updates#enabling-dependabot-version-updates). Dependabot is enabled in the settings by default.
- Ensure as many of the [GitHub Standards](https://github.com/ministryofjustice/github-repository-standards) rules are maintained as possibly can.
