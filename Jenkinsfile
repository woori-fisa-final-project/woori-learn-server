pipeline {
    agent any

    environment {
        AWS_HOST = "43.202.43.243"
        DOCKER_IMAGE = "bae1234/woori-learn-server:latest"
   }
    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Gradle Build') {
            steps {
                sh '''
                chmod +x gradlew
                ./gradlew clean build -x test
                '''
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${DOCKER_IMAGE} ."
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-cred',
                                                 usernameVariable: 'DOCKERHUB_USR',
                                                 passwordVariable: 'DOCKERHUB_PSW')]) {

                    sh '''
                    echo "${DOCKERHUB_PSW}" | docker login -u "${DOCKERHUB_USR}" --password-stdin
                    docker push ${DOCKER_IMAGE}
                    '''
                }
            }
        }

        stage('Deploy to AWS') {
            steps {
                sshagent(['aws-ssh-key']) {

                    withCredentials([
                        usernamePassword(credentialsId: 'db-credential',
                                         usernameVariable: 'DB_USER',
                                         passwordVariable: 'DB_PASS'),
                        string(credentialsId: 'db-url', variable: 'DB_URL'),
                        string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET')
                    ]) {

                        sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${AWS_HOST} << 'EOF'
                            docker pull ${DOCKER_IMAGE}
                            docker rm -f woori_backend || true
                            docker run -d --name woori_backend -p 8080:8080 \
                                -e SPRING_DATASOURCE_URL="${DB_URL}" \
                                -e SPRING_DATASOURCE_USERNAME="${DB_USER}" \
                                -e SPRING_DATASOURCE_PASSWORD="${DB_PASS}" \
                                -e JWT_SECRET="${JWT_SECRET}" \
                                ${DOCKER_IMAGE}
                        EOF
                        """
                    }
                }
            }
        }
    }
}

