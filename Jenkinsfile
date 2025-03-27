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
                        docker buildx build -t $DOCKER_IMAGE --load .
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
            withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG_FILE')]) {
                sh '''
                    # Use the provided kubeconfig directly
                    kubectl --kubeconfig=${KUBECONFIG_FILE} \
                        set image deployment/spring-boot-app \
                        spring-boot-app=${DOCKER_IMAGE} --record
                    
                    kubectl --kubeconfig=${KUBECONFIG_FILE} \
                        rollout status deployment/spring-boot-app --timeout=300s
                '''
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
                        echo "=== Starting Safe Cleanup ==="
                        echo "Current disk usage:"
                        df -h .
                        
                        # 1. Maven-specific cleanup (safe)
                        if [ -f "pom.xml" ]; then
                            echo "Cleaning Maven build artifacts..."
                            mvn clean
                            mvn dependency:purge-local-repository -DactTransitively=false -DreResolve=false
                        fi
                        
                        
                        
                        # 3. Docker cleanup (safe - only removes dangling artifacts)
                        echo "Cleaning Docker..."
                        docker image prune -f || true
                        docker container prune -f || true
                        
                        echo "Safe cleanup complete. Final disk usage:"
                        df -h .
                    '''
                }
            }
        }
        
    }
}