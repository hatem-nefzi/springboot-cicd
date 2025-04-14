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
    image: alpine/k8s:1.25.10
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
        SERVICE_NAME = "spring-boot-app-service"
    }

    stages {
        stage('Deploy') {
            steps {
                container('kubectl') {
                    withCredentials([file(credentialsId: 'kubeconfig1', variable: 'KUBECONFIG_FILE')]) {
                        script {
                            switch(params.DEPLOYMENT_MODE) {
                                // 1. Rolling Update (Default)
                                case 'rolling':
                                    sh '''
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            set image deployment/${APP_NAME} ${APP_NAME}=${DOCKER_IMAGE}
                                        
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            rollout status deployment/${APP_NAME} --timeout=300s
                                    '''
                                    break

                                // 2. Blue-Green Deployment
                                case 'blue-green':
                                    sh '''
                                        # Create green deployment
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            get deployment ${APP_NAME} -o yaml \
                                            | sed -e "s/name: ${APP_NAME}/name: ${APP_NAME}-green/" \
                                                  -e "s/${APP_NAME}:.*/${DOCKER_IMAGE}/" \
                                            | kubectl --kubeconfig=${KUBECONFIG_FILE} apply -f -
                                        
                                        # Wait for green deployment to be ready
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            rollout status deployment/${APP_NAME}-green --timeout=300s
                                        
                                        # Update service selector (assuming your service has deployment=blue/green labels)
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            patch svc ${SERVICE_NAME} \
                                            -p '{"spec":{"selector":{"deployment":"green"}}}'
                                        
                                        # Scale down blue deployment
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            scale deployment ${APP_NAME} --replicas=0
                                    '''
    break

                                // 3. Canary Deployment
                                case 'canary':
                                    sh '''
                                        # Create canary if missing
                                        if ! kubectl --kubeconfig=${KUBECONFIG_FILE} get deployment ${APP_NAME}-canary; then
                                            kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                                get deployment ${APP_NAME} -o yaml \
                                                | sed "s/name: ${APP_NAME}/name: ${APP_NAME}-canary/" \
                                                | sed "s/replicas: 3/replicas: 1/" \
                                                | kubectl --kubeconfig=${KUBECONFIG_FILE} apply -f -
                                        fi

                                        # Update canary
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            set image deployment/${APP_NAME}-canary ${APP_NAME}=${DOCKER_IMAGE}
                                        
                                        # Verify period
                                        sleep 120
                                        
                                        # Full rollout
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            set image deployment/${APP_NAME} ${APP_NAME}=${DOCKER_IMAGE}
                                        
                                        # Cleanup
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            scale deployment/${APP_NAME}-canary --replicas=0
                                    '''
                                    break

                                // 4. Recreate Strategy
                                case 'recreate':
                                    sh '''
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            scale deployment ${APP_NAME} --replicas=0
                                        
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            set image deployment/${APP_NAME} ${APP_NAME}=${DOCKER_IMAGE}
                                        
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            scale deployment ${APP_NAME} --replicas=3
                                        
                                        kubectl --kubeconfig=${KUBECONFIG_FILE} \
                                            rollout status deployment/${APP_NAME} --timeout=300s
                                    '''
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
                        sh '''
                            echo "=== Deployment ==="
                            kubectl --kubeconfig=${KUBECONFIG_FILE} get deployments
                            echo "\n=== Pods ==="
                            kubectl --kubeconfig=${KUBECONFIG_FILE} get pods -l app=${APP_NAME}
                            echo "\n=== Service ==="
                            kubectl --kubeconfig=${KUBECONFIG_FILE} get svc ${SERVICE_NAME}
                        '''
                    }
                }
            }
        }
    }
}