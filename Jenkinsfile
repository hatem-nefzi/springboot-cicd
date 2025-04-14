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
                                    sh '''
                                        kubectl --kubeconfig=$KUBECONFIG_FILE set image deployment/$APP_NAME $APP_NAME=$DOCKER_IMAGE
                                        kubectl --kubeconfig=$KUBECONFIG_FILE rollout status deployment/$APP_NAME --timeout=300s
                                    '''
                                    break

                                case 'blue-green':
                                    sh '''
                                        # Delete the green deployment if it exists (clean slate)
                                        kubectl --kubeconfig=$KUBECONFIG_FILE delete deployment $GREEN_NAME --ignore-not-found
                                        
                                        # Create new green deployment
                                        kubectl --kubeconfig=$KUBECONFIG_FILE create deployment $GREEN_NAME \
                                            --image=$DOCKER_IMAGE \
                                            --dry-run=client -o yaml > green-deployment.yaml
                                        
                                        # Add labels and any other necessary configurations
                                        sed -i "s/app: $GREEN_NAME/app: $GREEN_NAME/" green-deployment.yaml
                                        sed -i "s/matchLabels: {}/matchLabels:\n      app: $GREEN_NAME/" green-deployment.yaml
                                        sed -i "s/ labels: {}/ labels:\n    app: $GREEN_NAME/" green-deployment.yaml
                                        
                                        # Apply the green deployment
                                        kubectl --kubeconfig=$KUBECONFIG_FILE apply -f green-deployment.yaml
                                        
                                        # Expose the green deployment with a temporary service (optional)
                                        kubectl --kubeconfig=$KUBECONFIG_FILE expose deployment $GREEN_NAME \
                                            --name=$GREEN_NAME-svc --port=8080 --type=ClusterIP \
                                            --dry-run=client -o yaml | kubectl --kubeconfig=$KUBECONFIG_FILE apply -f -

                                        # Wait for green to be ready
                                        kubectl --kubeconfig=$KUBECONFIG_FILE rollout status deployment/$GREEN_NAME --timeout=300s

                                        # Switch service selector
                                        kubectl --kubeconfig=$KUBECONFIG_FILE patch svc $SERVICE_NAME \
                                            -p '{"spec":{"selector":{"app":"'$GREEN_NAME'"}}}'

                                        # Scale down blue
                                        kubectl --kubeconfig=$KUBECONFIG_FILE scale deployment/$APP_NAME --replicas=0
                                        
                                        # Clean up temporary service
                                        kubectl --kubeconfig=$KUBECONFIG_FILE delete svc $GREEN_NAME-svc --ignore-not-found
                                    '''
                                    break

                                case 'canary':
                                    sh '''
                                        # Delete any existing canary deployment
                                        kubectl --kubeconfig=$KUBECONFIG_FILE delete deployment $CANARY_NAME --ignore-not-found
                                        
                                        # Create canary deployment
                                        kubectl --kubeconfig=$KUBECONFIG_FILE create deployment $CANARY_NAME \
                                            --image=$DOCKER_IMAGE \
                                            --dry-run=client -o yaml > canary-deployment.yaml
                                        
                                        # Modify replicas and labels
                                        sed -i "s/replicas: 1/replicas: 1/" canary-deployment.yaml
                                        sed -i "s/app: $CANARY_NAME/app: $CANARY_NAME/" canary-deployment.yaml
                                        sed -i "s/matchLabels: {}/matchLabels:\n      app: $CANARY_NAME/" canary-deployment.yaml
                                        
                                        kubectl --kubeconfig=$KUBECONFIG_FILE apply -f canary-deployment.yaml
                                        kubectl --kubeconfig=$KUBECONFIG_FILE rollout status deployment/$CANARY_NAME --timeout=300s

                                        echo "Sleeping 90s to observe Canary..."
                                        sleep 90

                                        # Promote canary to production
                                        kubectl --kubeconfig=$KUBECONFIG_FILE set image deployment/$APP_NAME $APP_NAME=$DOCKER_IMAGE
                                        kubectl --kubeconfig=$KUBECONFIG_FILE rollout status deployment/$APP_NAME --timeout=300s

                                        # Delete canary
                                        kubectl --kubeconfig=$KUBECONFIG_FILE delete deployment $CANARY_NAME --ignore-not-found
                                    '''
                                    break

                                case 'recreate':
                                    sh '''
                                        kubectl --kubeconfig=$KUBECONFIG_FILE scale deployment/$APP_NAME --replicas=0
                                        sleep 5
                                        kubectl --kubeconfig=$KUBECONFIG_FILE set image deployment/$APP_NAME $APP_NAME=$DOCKER_IMAGE
                                        kubectl --kubeconfig=$KUBECONFIG_FILE scale deployment/$APP_NAME --replicas=2
                                        kubectl --kubeconfig=$KUBECONFIG_FILE rollout status deployment/$APP_NAME --timeout=300s
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
                            echo "=== Deployments ==="
                            kubectl --kubeconfig=$KUBECONFIG_FILE get deployments
                            
                            echo "\n=== Pods ==="
                            kubectl --kubeconfig=$KUBECONFIG_FILE get pods -l app=$APP_NAME || true
                            kubectl --kubeconfig=$KUBECONFIG_FILE get pods -l app=$GREEN_NAME || true
                            kubectl --kubeconfig=$KUBECONFIG_FILE get pods -l app=$CANARY_NAME || true

                            echo "\n=== Services ==="
                            kubectl --kubeconfig=$KUBECONFIG_FILE get svc $SERVICE_NAME
                        '''
                    }
                }
            }
        }
    }
}