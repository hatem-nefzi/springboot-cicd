pipeline {
    agent {
        kubernetes {
            label 'jenkins-agent'
            yaml """
apiVersion: v1
kind: Pod
metadata:
  name: jenkins-agent
spec:
  containers:
  - name: kubectl
    image: bitnami/kubectl:latest
    command: ['sleep']
    args: ['infinity']
  volumes:
  - name: kubeconfig-dir
    emptyDir: {}
"""
        }
    }

    parameters {
        choice(
            name: 'DEPLOYMENT_MODE',
            choices: ['rolling', 'blue-green', 'canary', 'recreate'],
            description: 'Select deployment strategy'
        )
    }

    environment {
        DOCKER_IMAGE = "hatemnefzi/spring-boot-app:latest"
        APP_NAME = "spring-boot-app"
        GREEN_NAME = "spring-boot-app-green"
        CANARY_NAME = "spring-boot-app-canary"
        SERVICE_NAME = "spring-boot-app-service"
    }

    stages {
        stage('Deploy') {
            steps {
                container('kubectl') {
                    withCredentials([file(credentialsId: 'kubeconfig1', variable: 'KUBECONFIG_FILE')]) {
                        script {
                            switch (params.DEPLOYMENT_MODE) {
                                case 'rolling':
                                    sh """
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} set image deployment/${APP_NAME} ${APP_NAME}=${DOCKER_IMAGE}
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} rollout status deployment/${APP_NAME} --timeout=300s
                                    """
                                    break

                                case 'blue-green':
                                    sh """
                                        # Clone deployment and modify name
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} get deployment ${APP_NAME} -o yaml \\
                                            | sed "s/name: ${APP_NAME}/name: ${GREEN_NAME}/" \\
                                            | sed "s/app: ${APP_NAME}/app: ${GREEN_NAME}/" \\
                                            | sed "s|image: .*|image: ${DOCKER_IMAGE}|" \\
                                            | kubectl --kubeconfig=${KUBECONFIG_FILE} apply -f -

                                        # Wait for green to be ready
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} rollout status deployment/${GREEN_NAME} --timeout=300s

                                        # Switch service selector
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} patch svc ${SERVICE_NAME} \\
                                            -p '{"spec":{"selector":{"app":"spring-boot-app-green"}}}'

                                        # Scale down blue
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} scale deployment/${APP_NAME} --replicas=0
                                    """
                                    break

                                case 'canary':
                                    sh """
                                        # Create canary deployment if not exists
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} get deployment ${CANARY_NAME} || \\
                                            kubectl --kubeconfig=${KUBECONFIG_FILE} get deployment ${APP_NAME} -o yaml \\
                                                | sed "s/name: ${APP_NAME}/name: ${CANARY_NAME}/" \\
                                                | sed "s/app: ${APP_NAME}/app: ${CANARY_NAME}/" \\
                                                | sed "s/replicas: .*/replicas: 1/" \\
                                                | sed "s|image: .*|image: ${DOCKER_IMAGE}|" \\
                                                | kubectl --kubeconfig=${KUBECONFIG_FILE} apply -f -

                                        kubectl --kubeconfig=${KUBECONFIG_FILE} rollout status deployment/${CANARY_NAME} --timeout=300s

                                        echo "Sleeping 90s to observe Canary..."
                                        sleep 90

                                        # Promote canary to production
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} set image deployment/${APP_NAME} ${APP_NAME}=${DOCKER_IMAGE}
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} rollout status deployment/${APP_NAME} --timeout=300s

                                        # Delete or scale down canary
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} delete deployment ${CANARY_NAME} --ignore-not-found
                                    """
                                    break

                                case 'recreate':
                                    sh """
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} scale deployment/${APP_NAME} --replicas=0
                                        sleep 5
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} set image deployment/${APP_NAME} ${APP_NAME}=${DOCKER_IMAGE}
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} scale deployment/${APP_NAME} --replicas=2
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} rollout status deployment/${APP_NAME} --timeout=300s
                                    """
                                    break
                            }
                        }
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                container('kubectl') {
                    withCredentials([file(credentialsId: 'kubeconfig1', variable: 'KUBECONFIG_FILE')]) {
                        sh """
                            echo "=== Deployments ==="
                            kubectl --kubeconfig=${KUBECONFIG_FILE} get deployments
                            
                            echo "\\n=== Pods ==="
                            kubectl --kubeconfig=${KUBECONFIG_FILE} get pods -l app=${APP_NAME} || true
                            kubectl --kubeconfig=${KUBECONFIG_FILE} get pods -l app=${GREEN_NAME} || true
                            kubectl --kubeconfig=${KUBECONFIG_FILE} get pods -l app=${CANARY_NAME} || true

                            echo "\\n=== Services ==="
                            kubectl --kubeconfig=${KUBECONFIG_FILE} get svc ${SERVICE_NAME}
                        """
                    }
                }
            }
        }
    }
}
