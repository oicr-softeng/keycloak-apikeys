@Library(value='jenkins-pipeline-library@master', changelog=false) _
pipelineSoftEngKeycloakApiKeys(
    gitRepo: "oicr-softeng/keycloak-apikeys",
    testCommand: "./mvnw test",
    buildCommand: './mvnw clean package -DskipTests -Dbuild.commit=${commit}'
)
