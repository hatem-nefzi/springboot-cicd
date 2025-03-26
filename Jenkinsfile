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
    - name: minikube-certs
      mountPath: /host-minikube  # Changed from jenkins-minikube
    - name: kubeconfig-dir
      mountPath: /tmp/kubeconfig # changed to /tmp which exists in container
    env:
    - name: KUBECONFIG
      value: "/tmp/kubeconfig/config" #instead of /var/lib/jenkins/.kube/config
  volumes:
  - name: workspace-volume
    hostPath:
      path: /var/lib/jenkins/workspace
  - name: docker-socket
    hostPath:
      path: /var/run/docker.sock
  - name: minikube-certs
    hostPath:
      path: /var/lib/jenkins/.minikube
  - name: kubeconfig-dir
    emptyDir: {}
    
"""
        }
    }

    environment {
        DOCKER_IMAGE = "hatemnefzi/spring-boot-app:latest"
        //KUBE_CONFIG_PATH = "/var/lib/jenkins/.kube/config"
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
                            # Verify mounted certificates
                            echo "=== Certificate Verification ==="
                            ls -la /host-minikube/profiles/minikube/client.*
                            ls -la /host-minikube/ca.crt
                            
                            # Create kubeconfig directory (now in /tmp)
                            mkdir -p /tmp/kubeconfig
                            
                            # Get cluster endpoint (works for both minikube and standard clusters)
                            APISERVER=$(kubectl config view -o jsonpath='{.clusters[0].cluster.server}')
                            if [ -z "$APISERVER" ]; then
                                echo "=== Using Minikube Default ==="
                                APISERVER="https://$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}'):8443"
                                [ -z "$APISERVER" ] && APISERVER="https://127.0.0.1:8443"
                            fi
                            echo "Using API Server: $APISERVER"
                            
                            # Create kubeconfig
                            cat > /tmp/kubeconfig/config <<EOF
apiVersion: v1
clusters:
- cluster:
    certificate-authority: /host-minikube/ca.crt
    server: ${APISERVER}
  name: minikube
contexts:
- context:
    cluster: minikube
    user: minikube
  name: minikube
current-context: minikube
kind: Config
preferences: {}
users:
- name: minikube
  user:
    client-certificate: /host-minikube/profiles/minikube/client.crt
    client-key: /host-minikube/profiles/minikube/client.key
EOF

                            # Verify configuration
                            echo "=== kubeconfig Contents ==="
                            cat /tmp/kubeconfig/config
                            
                            # Test connection
                            echo "=== Testing Connection ==="
                            export KUBECONFIG=/tmp/kubeconfig/config
                            kubectl config view
                            kubectl cluster-info
                            
                            # Deployment
                            echo "=== Deploying ${DOCKER_IMAGE} ==="
                            kubectl set image deployment/spring-boot-app spring-boot-app=${DOCKER_IMAGE} --record
                            kubectl rollout status deployment/spring-boot-app --timeout=300s
                            
                            # Verification
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