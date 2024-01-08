String podSpec = '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jdk
    tty: true
    image: adoptopenjdk/openjdk11:jdk-11.0.7_10-alpine-slim
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
'''

pipeline {
    agent {
        kubernetes {
            yaml podSpec
        }
    }

    environment {
        gitHubRegistry = 'ghcr.io'
        gitHubRepo = 'oicr-softeng/keycloak-apikeys'
        githubPackages = "${gitHubRegistry}/${gitHubRepo}"

        commit = sh(
            returnStdout: true,
            script: 'git describe --always'
        ).trim()

        version = readMavenPom().getVersion()
        artifactId = readMavenPom().getArtifactId()
        artifactName = "${artifactId}${version}.jar"

    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }


    stages {
        stage('Test') {
            steps {
                container('jdk') {
                    echo 'Running test'
                    sh './mvnw test'
                }
            }
        }

        stage('Build project') {
            steps {
                container('jdk') {
                    echo 'Building project'
                    sh './mvnw clean package'
                }
            }
        }

        stage('Publish tag to github') {
            when {
                branch 'main'
                branch 'develop'
                branch 'test-develop'
            }
            steps {
                container('node') {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'OvertureBioGithub',
                            passwordVariable: 'GIT_PASSWORD',
                            usernameVariable: 'GIT_USERNAME'
                        ),
                    ]) {
                        script {
                            if (env.BRANCH_NAME ==~ 'main') {
                                sh "git tag ${version}"
                                sh "git tag latest"
                            }else if (env.BRANCH_NAME ==~ 'develop') {
                                sh "git tag ${commit}"
                                sh "git tag edge"
                            } else { // push commit tag
                                sh "git tag ${commit}"
                            }
                            sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${gitHubRepo} --tags"
                        }
                    }
                }
            }
        }

        stage('Publish a Github Release') {
            steps {
                script {
                    def githubToken = env.GITHUB_TOKEN // Set GITHUB_TOKEN in Jenkins credentials
                    // Create a GitHub release using GitHub API
                    def releaseName = "Release ${version}"

                    def createReleaseResponse = sh(
                            script: "curl \
                                    -X POST \
                                    -H 'Authorization: token ${githubToken}' \
                                    -d '{\"tag_name\": \"${version}\", \"name\": \"${releaseName}\", \"body\": \"\"}' \
                                    https://api.github.com/repos/${githubPackages}/releases",
                            returnStdout: true
                    ).trim()

                    def uploadUrl = sh(
                            script: "echo ${createReleaseResponse} | jq -r '.upload_url'",
                            returnStdout: true
                    ).trim().replace("{?name,label}", "")

                    // Upload the .jar file as a release asset
                    sh "curl \
                        -X POST \
                        -H 'Authorization: token ${githubToken}' \
                        -H 'Content-Type: application/java-archive' \
                        --data-binary target/${artifactName} ${uploadUrl}?name=${artifactId}.jar"
                }
            }
        }
    }
}