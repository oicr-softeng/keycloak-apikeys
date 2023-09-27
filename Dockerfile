FROM docker.io/bitnami/keycloak:22
COPY target/keycloak-apikeys-1.0-SNAPSHOT.jar /opt/bitnami/keycloak/providers/