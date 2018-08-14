pipeline {

    agent any

    stages {
        
        stage('Build') {
            parallel {
                stage('checkstyle') {
                    sh 'mvn checkstyle:checkstyle'
                }
                post {
                    checkstyle pattern: 'target/checkstyle-result.xml'
                }
                stage('install') {
                    sh 'mvn install'
                }
                post {
                    junit 'target/surefire-reports/*.xml' 
                }
            }
        }
    }
}
