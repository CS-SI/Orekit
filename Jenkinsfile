pipeline {

    agent any
    
    stages {

        withEnv(["JAVA_HOME=${tool 'openjdk-8'}", "PATH+MAVEN=${tool 'maven-default'}/bin:${env.JAVA_HOME}/bin"]) {
        
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
}
