pipeline {

    agent any

    stages {
        
        stage('Build') {
            parallel {
                stage('checkstyle') {
                    steps {
                        sh 'mvn checkstyle:checkstyle'
                        checkstyle pattern: 'target/checkstyle-result.xml'
                    }
                }
                stage('install') {
                    steps {
                        sh 'mvn install'
                        junit 'target/surefire-reports/*.xml' 
                    }
                }
            }
        }
    }
}
