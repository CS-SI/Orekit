pipeline {

    agent any

    stages {
        
        stage('Build') {
            steps {
                sh 'mvn checkstyle:checkstyle install'
            }
            post {
                checkstyle pattern: 'target/checkstyle-result.xml'
                junit 'target/surefire-reports/*.xml' 
            }
        }
    }
}
