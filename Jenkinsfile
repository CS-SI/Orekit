pipeline {

    agent any
    
    stages {
        
        stage('Build') {
            
            parallel {
                
                stage('checkstyle') {
                    steps {
                        withEnv(["JAVA_HOME=${tool 'openjdk-8'}", "PATH+MAVEN=${tool 'mvn-default'}/bin:${env.JAVA_HOME}/bin"]) {
                            sh 'mvn checkstyle:checkstyle'
                            checkstyle pattern: 'target/checkstyle-result.xml'
                        }
                    }
                }
                
                stage('install') {
                    steps {
                        withEnv(["JAVA_HOME=${tool 'openjdk-8'}", "PATH+MAVEN=${tool 'mvn-default'}/bin:${env.JAVA_HOME}/bin"]) {
                            sh 'mvn install'
                            junit 'target/surefire-reports/*.xml' 
                        }
                    }
                }
            }
        }
    }
}
