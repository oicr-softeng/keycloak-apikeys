FROM docker.io/bitnami/keycloak:22

CMD ["kc.sh", "start-dev", "--import-realm"]