pipeline {
    agent {
        kubernetes {
            label 'jenkins-agent'
            yaml """
apiVersion: v1
kind: Pod
metadata:
  name: jenkins-agent
  labels:
    jenkins-agent: true
spec:
  serviceAccountName: jenkins-agent-sa
  containers:
  - name: jnlp
    image: jenkins/inbound-agent:latest
    args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
    workingDir: /var/lib/jenkins/workspace
    tty: true
    volumeMounts:
    - name: workspace-volume
      mountPath: /var/lib/jenkins/workspace
    securityContext:
      runAsUser: 1000
      fsGroup: 1000
    resources:
      limits:
        cpu: "1"
        memory: "1Gi"
      requests:
        cpu: "500m"
        memory: "512Mi"
  - name: maven
    image: hatemnefzi/maven-docker:latest
    command: ['cat']
    tty: true
    workingDir: /var/lib/jenkins/workspace
    volumeMounts:
    - name: workspace-volume
      mountPath: /var/lib/jenkins/workspace
    - name: docker-socket
      mountPath: /var/run/docker.sock
    resources:
      limits:
        cpu: "1"
        memory: "1Gi"
      requests:
        cpu: "500m"
        memory: "512Mi"
  - name: kubectl
    image: alpine/k8s:1.25.10
    command: ['sleep']
    args: ['infinity']
    workingDir: /var/lib/jenkins/workspace
    volumeMounts:
    - name: workspace-volume
      mountPath: /var/lib/jenkins/workspace
    - name: minikube-dir
      mountPath: /host-minikube
    - name: kubeconfig-dir
      mountPath: /tmp/kubeconfig
    env:
    - name: MINIKUBE_HOME
      value: "/host-minikube"
  volumes:
  - name: workspace-volume
    hostPath:
      path: /var/lib/jenkins/workspace
  - name: docker-socket
    hostPath:
      path: /var/run/docker.sock
  - name: minikube-dir
    hostPath:
      path: /var/lib/jenkins/.minikube
      type: Directory
  - name: kubeconfig-dir
    emptyDir: {}
"""
        }
    }

    environment {
        DOCKER_IMAGE = "hatemnefzi/spring-boot-app:latest"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[
                        url: 'git@github.com:hatem-nefzi/springboot-cicd.git',
                        credentialsId: 'SSH'
                    ]]
                ])
            }
        }

        stage('Build') {
            steps {
                container('maven') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                container('maven') {
                    sh 'mvn test'  // Runs only UnitTests.java
            }
            }
        }

        stage('Integration Tests') {
            steps {
                container('maven') {
                    sh 'mvn verify -DskipTests'  // Runs only ApiTests.java
            }
            }
            
        }

        stage('Run Gatling Tests') {
            steps {
                container('maven') {
                    sh 'mvn gatling:test'
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/gatling/**/*', allowEmptyArchive: true
                    gatlingArchive()
                }
            }
        }

        stage('Clean Playwright Cache') {
            steps {
                container('maven') {
                    sh 'rm -rf /home/jenkins/agent/playwright-browsers'
                    sh 'mvn exec:java -Dexec.mainClass="com.microsoft.playwright.CLI" -Dexec.args="install --force"'
                }
            }
        }

        stage('Functional Tests') {
            steps {
                container('maven') {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    script {
                        def testReports = findFiles(glob: 'target/surefire-reports/*.xml')
                        if (testReports) {
                            junit 'target/surefire-reports/*.xml'
                        } else {
                            echo 'No test report files found. Skipping JUnit step.'
                        }
                    }
                    archiveArtifacts artifacts: 'target/surefire-reports/**/*', allowEmptyArchive: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                container('maven') {
                    sh '''
                        docker buildx create --use
                        docker buildx build --pull --no-cache -t $DOCKER_IMAGE --load .
                        docker buildx prune -af
                    '''
                }
            }
        }

        stage('Debug') {
            steps {
                container('maven') {
                    sh '''
                        echo "PATH: $PATH"
                        which docker
                        docker --version
                    '''
                }
            }
        }

        stage('Docker Push') {
            steps {
                container('maven') {
                    withDockerRegistry([credentialsId: 'docker-hub-credentials', url: 'https://index.docker.io/v1/']) {
                        sh '''
                            echo "PATH: $PATH"
                            echo "Docker version:"
                            docker --version
                            echo "Docker images:"
                            docker images
                            echo "Pushing Docker image: $DOCKER_IMAGE"
                            docker push $DOCKER_IMAGE
                        '''
                    }
                }
            }
        }

        stage('Deploy') {
    steps {
        container('kubectl') {
            withCredentials([file(credentialsId: 'kubeconfig1', variable: 'KUBECONFIG_FILE')]) {
                sh '''
                    # Ensure KUBECONFIG file exists
                    if [ ! -f "$KUBECONFIG_FILE" ]; then
                        echo "ERROR: Kubeconfig file not found at $KUBECONFIG_FILE"
                        exit 1
                    fi

                    echo "Using KUBECONFIG at: $KUBECONFIG_FILE"

                    # Update deployment image
                    kubectl --kubeconfig=$KUBECONFIG_FILE \
                        set image deployment/spring-boot-app \
                        spring-boot-app=$DOCKER_IMAGE
                    
                    # Wait for rollout to complete
                    kubectl --kubeconfig=$KUBECONFIG_FILE \
                        rollout status deployment/spring-boot-app --timeout=300s
                    
                    # Verify pods are ready
                    kubectl --kubeconfig=$KUBECONFIG_FILE \
                        wait --for=condition=ready pod \
                        -l app=spring-boot-app --timeout=120s
                '''
            }
        }
    }
}
        stage('Verify Endpoints') {
    steps {
        container('kubectl') {
            script {
                // Method 1: Use ClusterIP directly (most reliable)
                def CLUSTER_IP = sh(script: """
                    kubectl get svc spring-boot-app-service \
                    -o jsonpath='{.spec.clusterIP}'
                """, returnStdout: true).trim()
                
                def endpoints = ['/hello', '/status']
                endpoints.each { endpoint ->
                    def httpCode = sh(script: """
                        curl -s -o /dev/null -w '%{http_code}' \
                        --max-time 5 \
                        http://${CLUSTER_IP}${endpoint}
                    """, returnStdout: true).trim()
                    
                    if (httpCode != "200") {
                        error("${endpoint} failed with HTTP ${httpCode}")
                    } else {
                        echo "Verified ${endpoint} (HTTP 200)"
                    }
                }
                
                // Method 2: Alternative using Service DNS (if needed)
                def SERVICE_DNS = "spring-boot-app-service.default.svc.cluster.local"
                sh """
                    echo "Testing via Service DNS..."
                    curl -v http://${SERVICE_DNS}/hello
                """
            }
        }
    }
}

    }
    post {
    always {
        container('maven') {
            dir("${WORKSPACE}") {
                sh '''
                    echo "=== Starting Advanced Cleanup ==="
                    echo "Initial disk usage:"
                    df -h .

                    # Maven Cleanup: Removes old dependencies, keeping only useful caches
                    echo "Cleaning Maven build artifacts..."
                    mvn clean
                    rm -rf ~/.m2/repository/org/apache/maven/plugins
                    rm -rf ~/.m2/repository/org/apache/maven/shared
                    rm -rf ~/.m2/repository/com/sun
                    rm -rf ~/.m2/repository/org/codehaus
                    find ~/.m2/repository -type f -name "*lastUpdated" -delete

                    # Node Cleanup (Playwright & other cached data)
                    echo "Cleaning Node.js cache..."
                    rm -rf ~/.npm/_cacache/*
                    rm -rf ~/.cache/ms-playwright

                    # Docker Cleanup: Remove old images/containers while keeping actively used ones
                    echo "Cleaning Docker..."
                    docker system prune -af --volumes || true
                    docker images --format '{{.Repository}}:{{.Tag}}' | grep '<none>' | xargs -r docker rmi || true
                    docker container prune -f || true

                    # Check final disk usage
                    echo "Final disk usage after cleanup:"
                    df -h .
                    
                    echo "=== Cleanup Completed Successfully ==="
                '''
            }
        }
    }
}

} 