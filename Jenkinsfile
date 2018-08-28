pipeline {

    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {

        stage('Cleaning') {
            steps {
                sh 'git clean -fdx'
            }
        }

        stage('Build') {
            steps {
                withEnv(["JAVA_HOME=${tool 'openjdk-8'}",
                         "PATH+MAVEN=${tool 'mvn-default'}/bin:${env.JAVA_HOME}/bin"]) {
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
