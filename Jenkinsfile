pipeline {
    agent any

    environment {
        AWS_HOST = "52.79.70.229"
        DOCKER_IMAGE = "bae1234/woori-learn-server:latest"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Deploy on AWS') {
            steps {
                sshagent(['aws-ssh-key']) {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'db-credential',
                            usernameVariable: 'DB_USER',
                            passwordVariable: 'DB_PASS'
                        ),
                        string(credentialsId: 'db-url', variable: 'DB_URL'),
                        string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET'),
                        usernamePassword(
                            credentialsId: 'dockerhub-cred',
                            usernameVariable: 'DOCKERHUB_USR',
                            passwordVariable: 'DOCKERHUB_PSW'
                        )
                    ]) {
                        sh """
ssh -o StrictHostKeyChecking=no ubuntu@${AWS_HOST} << EOF

# --------------------------
# 1) Repo 준비
# --------------------------
if [ ! -d "woori-learn-server" ]; then
    git clone https://github.com/woori-fisa-final-project/woori-learn-server.git
fi

cd woori-learn-server
git pull origin aws-test

# --------------------------
# 2) Gradle Build (AWS에서 실행)
# --------------------------
chmod +x gradlew
./gradlew clean build -x test

# --------------------------
# 3) Docker Build (AWS에서 실행)
# --------------------------
docker build -t ${DOCKER_IMAGE} .

# >>> Dangling 이미지 정리
docker image prune -f
docker builder prune -f

# --------------------------
# 4) DockerHub Login & Push
# --------------------------
echo "${DOCKERHUB_PSW}" | docker login -u "${DOCKERHUB_USR}" --password-stdin
docker push ${DOCKER_IMAGE}

# --------------------------
# 5) 기존 컨테이너 종료 후 재실행
# --------------------------
docker rm -f woori_backend || true

docker run -d --name woori_backend -p 8080:8080 \
    -e SPRING_DATASOURCE_URL="${DB_URL}" \
    -e SPRING_DATASOURCE_USERNAME="${DB_USER}" \
    -e SPRING_DATASOURCE_PASSWORD="${DB_PASS}" \
    -e JWT_SECRET="${JWT_SECRET}" \
    -e CLIENT_BASE_URL="http://${AWS_HOST}:3000" \
    -e ACCOUNT_EXTERNAL_AUTH_BASE_URL="http://localhost:9000" \
    -e external.bank.base-url="http://localhost:9000" \
    -e spring.env.app-key="this-is-app-key" \
    -e spring.env.secret-key="MY_SECRET_ABC123" \
    -e external.bank.account-url="http://localhost:9000/account" \
    ${DOCKER_IMAGE}

# >>> 컨테이너 띄운 후에도 이미지 한 번 더 정리
docker image prune -f

EOF
"""
                    }
                }
            }
        }
    }
}

