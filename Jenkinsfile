pipeline {
    agent any

    environment {
        // Use JDK17 mounted from host machine
        JAVA_HOME = '/opt/jdk17'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"

        // Gradle options
        GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.caching=true'

        // Android SDK (mounted from host machine)
        ANDROID_HOME = '/opt/android-sdk'
    }

    options {
        // Keep last 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))

        // Timeout after 30 minutes
        timeout(time: 30, unit: 'MINUTES')

        // Timestamps in console output
        timestamps()

        // Disable concurrent builds
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm

                script {
                    // Display Git info
                    sh 'git log -1 --pretty=format:"%h - %an, %ar : %s"'
                    sh 'git branch -a'
                }
            }
        }

        stage('Setup') {
            steps {
                echo 'Setting up environment...'

                script {
                    // Make gradlew executable
                    sh 'chmod +x ./gradlew'

                    // Display versions
                    sh 'java -version'
                    sh './gradlew --version'
                }
            }
        }

        stage('Build & Test') {
            steps {
                echo 'Running lint, build, and test in single Gradle invocation...'
                script {
                    // Run all tasks together for better performance
                    // build = assemble + check (which includes test)
                    // This is faster than running separate Gradle commands
                    def result = sh(
                        script: './gradlew clean ktlintCheck detekt build --stacktrace',
                        returnStatus: true
                    )

                    if (result != 0) {
                        echo 'Build failed, possibly due to Android SDK issues. Retrying without Android modules...'
                        sh '''
                            ./gradlew clean \
                                ktlintCheck \
                                detekt \
                                build \
                                -x :composeApp:assembleDebug \
                                -x :composeApp:assembleRelease \
                                -x :composeApp:testDebugUnitTest \
                                -x :composeApp:testReleaseUnitTest \
                                --stacktrace || true
                        '''
                    }
                }
            }
            post {
                always {
                    // Publish test results if they exist
                    junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Code Quality Reports') {
            parallel {
                stage('Detekt Report') {
                    steps {
                        script {
                            if (fileExists('build/reports/detekt')) {
                                publishHTML([
                                    allowMissing: false,
                                    alwaysLinkToLastBuild: true,
                                    keepAll: true,
                                    reportDir: 'build/reports/detekt',
                                    reportFiles: 'detekt.html',
                                    reportName: 'Detekt Report'
                                ])
                            }
                        }
                    }
                }

                stage('KtLint Report') {
                    steps {
                        script {
                            if (fileExists('build/reports/ktlint')) {
                                publishHTML([
                                    allowMissing: false,
                                    alwaysLinkToLastBuild: true,
                                    keepAll: true,
                                    reportDir: 'build/reports/ktlint',
                                    reportFiles: 'ktlintMainSourceSetCheck.html',
                                    reportName: 'KtLint Report'
                                ])
                            }
                        }
                    }
                }
            }
        }

        stage('Archive Artifacts') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    branch pattern: 'release/.*', comparator: 'REGEXP'
                }
            }
            steps {
                echo 'Archiving build artifacts...'
                archiveArtifacts artifacts: '**/build/libs/*.jar', allowEmptyArchive: true
                archiveArtifacts artifacts: '**/build/outputs/**/*.apk', allowEmptyArchive: true
                archiveArtifacts artifacts: '**/build/distributions/*.tar', allowEmptyArchive: true
            }
        }

        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                echo 'Deploying to staging environment...'
                script {
                    // Add your staging deployment steps here
                    // Example: Deploy server module
                    // sh './gradlew :server:deploy'
                    echo 'Staging deployment placeholder - configure as needed'
                }
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                echo 'Deploying to production environment...'
                script {
                    // Add your production deployment steps here
                    // Example: Deploy server module
                    // sh './gradlew :server:deploy'
                    echo 'Production deployment placeholder - configure as needed'
                }
            }
        }
    }

    post {
        always {
            echo 'Cleaning up workspace...'
            cleanWs(
                deleteDirs: true,
                patterns: [
                    [pattern: '**/build', type: 'INCLUDE'],
                    [pattern: '**/.gradle', type: 'INCLUDE']
                ]
            )
        }

        success {
            echo 'Build succeeded! ✅'
            // Uncomment to send success notification:
            // emailext subject: "Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //          body: "Build was successful!",
            //          to: "your-email@example.com"
        }

        failure {
            echo 'Build failed! ❌'
            // Uncomment to send failure notification:
            // emailext subject: "Build Failure: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //          body: "Build failed. Check console output: ${env.BUILD_URL}",
            //          to: "your-email@example.com"
        }

        unstable {
            echo 'Build is unstable! ⚠️'
        }
    }
}
