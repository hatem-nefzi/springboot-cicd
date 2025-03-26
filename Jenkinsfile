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
    image: alpine/k8s:1.25.10  # Changed to more complete image
    command: ['sleep']
    args: ['infinity']  # Keeps container running
    workingDir: /var/lib/jenkins/workspace
    volumeMounts:
    - name: workspace-volume
      mountPath: /var/lib/jenkins/workspace
    - name: jenkins-minikube   # changed volume name
      mountPath: /var/lib/jenkins/.minikube
    - name: kube-config
      mountPath: /home/jenkins/.kube
    env:
    - name: KUBECONFIG
      value: "/home/jenkins/.kube/config"
  volumes:
  - name: workspace-volume
    hostPath:
      path: /var/lib/jenkins/workspace
  - name: docker-socket
    hostPath:
      path: /var/run/docker.sock
  - name: kube-config
    hostPath:
      path: /home/jenkins/.kube  # Changed to match your actual path
  - name: jenkins-minikube  # Added volume for Minikube
    hostPath:
      path: /var/lib/jenkins/.minikube  # Changed to match your actual path
"""
        }
    }

    environment {
        DOCKER_IMAGE = "hatemnefzi/spring-boot-app:latest"
        KUBE_CONFIG_PATH = "/home/jenkins/.kube/config"  
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
                    script {
                        sh '''
                            echo "=== Verifying Minikube Access ==="
                            ls -la /home/jenkins/.minikube/profiles/minikube/
                            ls -la /home/jenkins/.kube/
                            
                            echo "=== Current kubectl Configuration ==="
                            kubectl config get-contexts
                            kubectl cluster-info
                            
                            echo "=== Deploying $DOCKER_IMAGE ==="
                            kubectl set image deployment/spring-boot-app spring-boot-app=$DOCKER_IMAGE --record
                            kubectl rollout status deployment/spring-boot-app --timeout=300s
                            
                            echo "=== Verification ==="
                            kubectl get deployments
                            kubectl get pods
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
                        # Only run if pom.xml exists
                        if [ -f "pom.xml" ]; then
                            mvn dependency:purge-local-repository -DactTransitively=false -DreResolve=false
                        else
                            echo "No pom.xml found - skipping Maven cleanup"
                        fi
                        
                        # General workspace cleanup
                        find . -mindepth 1 -maxdepth 1 ! -name 'workspace' -exec rm -rf {} +
                    '''
                }
            }
        }
        success {
            slackSend channel: '#dev-team', message: 'Pipeline succeeded!'
        }
        failure {
            slackSend channel: '#dev-team', message: 'Pipeline failed!'
        }
    }
}