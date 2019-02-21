pipeline {

    agent any
    tools {
        maven 'mvn-default'
        jdk   'openjdk-8'
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
    }

    stages {

        stage('Cleaning') {
            steps {
                sh 'git clean -fdx'
            }
        }

        stage('Build') {
            steps {
                script {
                    if ( env.BRANCH_NAME ==~ /^release-[.0-9]+$/ ) {
                        sh 'mvn verify assembly:single'
                    }
                    else {
                        sh 'mvn verify site'
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            script {
                if ( env.BRANCH_NAME ==~ /^release-[.0-9]+$/ ) {
                    archiveArtifacts artifacts: 'target/*.zip', fingerprint: true
                }
            }
            checkstyle pattern: 'target/checkstyle-result.xml'
            junit 'target/surefire-reports/*.xml'
            jacoco execPattern:'target/**.exec', classPattern: '**/classes', sourcePattern: '**/src/main/java'
        }
    }
}
