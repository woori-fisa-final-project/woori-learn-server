pipeline {
    agent any

    environment {
        AWS_HOST      = "54.116.2.46"
        DOCKER_IMAGE  = "bae1234/woori-learn-server:latest"
    }

    stages {

        /* --------------------------
         * 1) Git Checkout
         * -------------------------- */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* --------------------------
         * 2) Gradle Build (Jenkins 내부)
         * -------------------------- */
        stage('Gradle Build') {
            steps {
                sh '''
                chmod +x gradlew
                ./gradlew clean build -x test
                '''
            }
        }

        /* --------------------------
         * 3) Docker Build
         * -------------------------- */
        stage('Docker Build') {
            steps {
                sh "docker rmi -f ${DOCKER_IMAGE} || true"
                sh "docker build -t ${DOCKER_IMAGE} ."
            }
        }

        /* --------------------------
         * 4) Docker Hub Push
         * -------------------------- */
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-cred',
                    usernameVariable: 'DOCKERHUB_USR',
                    passwordVariable: 'DOCKERHUB_PSW'
                )]) {
                    sh '''
                    echo "$DOCKERHUB_PSW" | docker login -u "$DOCKERHUB_USR" --password-stdin
                    docker push ${DOCKER_IMAGE}
                    '''
                }
            }
        }

        /* --------------------------
         * 5) AWS Deploy
         * -------------------------- */
        stage('Deploy to AWS') {
            steps {
                sshagent(['aws-ssh-key']) {

                    withCredentials([
                        usernamePassword(credentialsId: 'db-credential',
                            usernameVariable: 'DB_USER',
                            passwordVariable: 'DB_PASS'
                        ),
                        string(credentialsId: 'db-url', variable: 'DB_URL'),
                        string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET'),
			string(credentialsId: 'admin-account-number', variable: 'APP_ADMIN_ACCOUNT_NUMBER')
                    ]) {

                        sh '''
ssh -o StrictHostKeyChecking=no ubuntu@${AWS_HOST} << EOF

# 도커 이미지 pull
docker pull ${DOCKER_IMAGE}

# 기존 컨테이너 종료
docker rm -f woori_backend || true

# 새 컨테이너 실행
docker run -d --name woori_backend -p 8080:8080 \
    -e SPRING_DATASOURCE_URL="${DB_URL}" \
    -e SPRING_DATASOURCE_USERNAME="${DB_USER}" \
    -e SPRING_DATASOURCE_PASSWORD="${DB_PASS}" \
    -e JWT_SECRET="${JWT_SECRET}" \
    -e CLIENT_BASE_URL="http://woorilearn.site" \
    -e SPRING_DATA_REDIS_HOST="172.31.2.246" \
    -e SPRING_DATA_REDIS_PORT="6379" \
    -e ACCOUNT_EXTERNAL_AUTH_BASE_URL="http://43.201.222.157:8081" \
    -e ACCOUNT_EXTERNAL_AUTH_REQUEST_PATH="/otp" \
    -e EXTERNAL_BANK_BASE_URL="http://43.201.222.157:8081" \
    -e EXTERNAL_BANK_ACCOUNT_URL="/account" \
    -e spring.env.app-key="YOUR_APP_KEY_123" \
    -e spring.env.secret-key="MY_SECRET_ABC123" \
    -e APP_ADMIN_ACCOUNT_NUMBER="${APP_ADMIN_ACCOUNT_NUMBER}" \
    ${DOCKER_IMAGE}

# dangling image cleanup
docker image prune -f

EOF
'''
                    }
                }
            }
        }
    }
}

