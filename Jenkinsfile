String podSpec = '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jdk
    tty: true
    image: eclipse-temurin:17-jdk-jammy
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
        privileged: true
        runAsUser: 0
    volumeMounts:
      - name: docker-graph-storage
        mountPath: /var/lib/docker
  - name: docker
    image: docker:18-git
    tty: true
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
    - name: HOME
      value: /home/jenkins/agent
  securityContext:
    runAsUser: 1000
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
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