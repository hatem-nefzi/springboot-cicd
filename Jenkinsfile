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
    containers:
    - name: jnlp
        image: jenkins/inbound-agent:latest
        args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
        workingDir: /home/jenkins/agent
        tty: true
        ports:
        - containerPort: 50000
            protocol: TCP
        resources:
            limits:
                cpu: "1"
                memory: "1Gi"
            requests:
                cpu: "500m"
                memory: "512Mi"
    - name: maven
        image: maven:3.8-openjdk-17
        command:
        - cat
        tty: true
        workingDir: /home/jenkins/agent
        resources:
            limits:
                cpu: "1"
                memory: "1Gi"
            requests:
                cpu: "500m"
                memory: "512Mi"
    - name: gatling
        image: denvazh/gatling:latest
        command:
        - cat
        tty: true
        workingDir: /home/jenkins/agent
        resources:
            limits:
                cpu: "1"
                memory: "1Gi"
            requests:
                cpu: "500m"
                memory: "512Mi"
    - name: playwright
        image: mcr.microsoft.com/playwright:focal
        command:
        - cat
        tty: true
        workingDir: /home/jenkins/agent
        resources:
            limits:
                cpu: "1"
                memory: "1Gi"
            requests:
                cpu: "500m"
                memory: "512Mi"
        }
    }

    environment {
        DOCKER_IMAGE = "hatemnefzi/spring-boot-app:latest"
        KUBE_CONFIG_PATH = "/var/lib/jenkins/.kube/config"
    }

    stages {
        stage('Checkout') {
            steps {
                git(
                    url: 'https://github.com/hatem-nefzi/springboot-cicd',
                    credentialsId: 'github-pat-credentials',
                    branch: 'main'
                )
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
                    // Archive Gatling reports
                    archiveArtifacts artifacts: 'target/gatling/**/*', allowEmptyArchive: true
                    // Publish Gatling reports (optional)
                    gatlingArchive()
                }
            }
        }

        stage('Functional Tests') {
            steps {
                container('playwright') {
                    
                    // Install Node.js and npm (required for Playwright)
                    sh '''
                        apt-get update
                        apt-get install -y nodejs npm
                        npm install playwright
                        npx playwright install
                        echo "Playwright installation complete"
                    '''
                    sh 'mvn test' // Runs Playwright tests
                }
            }
            post {
                always {
                    script {
                        // Check if test reports exist
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

        stage('Docker Push') {
            steps {
                withDockerRegistry([credentialsId: 'docker-hub-credentials', url: 'https://index.docker.io/v1/']) {
                    container('maven') {
                        sh 'docker push $DOCKER_IMAGE'
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    container('maven') {
                        sh '''
                            kubectl set image deployment/spring-boot-app spring-boot-app=hatemnefzi/spring-boot-app:latest --record
                            kubectl rollout status deployment/spring-boot-app
                            kubectl get pods
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            slackSend channel: '#dev-team', message: 'Pipeline succeeded!'
        }
        failure {
            slackSend channel: '#dev-team', message: 'Pipeline failed!'
        }
    }
}
