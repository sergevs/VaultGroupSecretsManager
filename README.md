# [Vault](https://www.vaultproject.io/) group secrets manager

## Description
The application may be used to introduce a groups or teams shared secrets role based access. The problem is described at vault [issue 1284](https://github.com/hashicorp/vault/issues/1284). It does not introduce symlinks, but copy secrets according to configurations to desired paths which in turn may be allowed for teams using standard vault ACL.
As an example: 
There is a structured secrets path with a convenient for applications access.
Now you have to provide access to some secrets to teams or another groups. The group secrets manager allows to configure to copy particular secrets at another location. How it works:
* Read configuration file where sources and secrets are described
* Collect secrets at source locations
* Check if secrets already exists at target path and copy it if not.

## Requiremets
The application designed and tested for [KV Secrets Engine - Version 2](https://www.vaultproject.io/docs/secrets/kv/kv-v2).
KV Secrets Engine Version 1 is not supported.

## Build
[Gradle](https://gradle.org/) and [jdk8](https://openjdk.java.net/projects/jdk8u/) is required to build.
Build on host machine:
```
gradle build
```

Alternatively, if you prefer to run application as docker container:
```
docker build . -t vault-group-secrets-manager:1.0
```

## Configuration
### Authentication
Group secret manager can be configured to access vault with token, app role or kubernetes authentication. The authentication is configured using environment variables:

| Variable             | Description                                           |
| -------------------- | ----------------------------------------------------- |
| VAULT_ADDR           | vault FQDN or IP address                              |
| VAULT_TOKEN          | if set, use token for authentication                  |
| VAULT_APPROLE_ID     | if set, authenticate with app role                    |
| VAULT_APPROLE_SECRET | required if VAULT_ROLE_ID is specified                |
| VAULT_K8S_ROLE       | if set, authenticate with kubernetes                  |
| VAULT_K8S_TOKEN      | jwt token, required if VAULT_K8S_ROLE is specified    |
| VAULT_GSCM_CONFIG    | configuration file name, defaults to gscm-config.yaml |

### Secrets groups management
[Configuration file](gscm-config.yaml) syntax is in yaml format, it should contain secretMap key with list of sources which contains optional secrets list and at least one target at targets list.

## Run
### Standalone application
Set configuration variables and run. As an instance:
```
export VAULT_ADDR='https://my.vault.example.org'
export VAULT_TOKEN='my token with required permissions to access and write secrets'

java -jar build/libs/vault.gscm-1.0.jar
```

### Docker container
Ready to use docker image is available at (docker hub)[https://hub.docker.com/r/sergevs42/vault-group-secrets-manager]
As an instance:
```
docker run --rm -it -e VAULT_ADDR='https://my.vault.example.org' \
                    -e VAULT_TOKEN='my token with required permissions to access and write secrets' \
                    --mount type=bind,source=${PWD}/gscm-config.yaml,target=/app/gscm-config.yaml \
                    sergevs42/vault-group-secrets-manager
