pipeline {

    agent any

    environment {
        MAVEN_CLI_OPTS = "-s .CI/maven-settings.xml"
    }

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
                        sh 'mvn verify site'
                    }
                    else if ( env.BRANCH_NAME ==~ /^develop$/ ) {
                        sh 'mvn install site'
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
            junit testResults: '**/target/surefire-reports/*.xml'
            jacoco execPattern: 'target/**.exec',
                   classPattern: '**/classes',
                   sourcePattern: '**/src/main/java',
                   exclusionPattern: 'fr/cs/examples/**/*.class',
                   changeBuildStatus: true,
                   minimumBranchCoverage: '80', maximumBranchCoverage: '85',
                   minimumClassCoverage: '95', maximumClassCoverage: '100',
                   minimumComplexityCoverage: '80', maximumComplexityCoverage: '85',
                   minimumInstructionCoverage: '85', maximumInstructionCoverage: '90',
                   minimumLineCoverage: '85', maximumLineCoverage: '90',
                   minimumMethodCoverage: '90', maximumMethodCoverage: '95'
            recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), javaDoc()]
            recordIssues enabledForFailure: true, tool:  checkStyle()
            recordIssues enabledForFailure: true, tool:  spotBugs()
        }
    }
}
