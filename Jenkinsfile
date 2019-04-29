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
            junit testResults: '**/target/surefire-reports/*.xml'
            jacoco execPattern:'target/**.exec', classPattern: '**/classes', sourcePattern: '**/src/main/java'
            recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), javaDoc()]
            recordIssues enabledForFailure: true, tool:  checkStyle()
            recordIssues enabledForFailure: true, tool:  spotBugs()
        }
    }
}
