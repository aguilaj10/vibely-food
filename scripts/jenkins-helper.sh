#!/bin/bash

# Jenkins Helper Script
# Provides convenient commands to interact with Jenkins

set -e

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '#' | xargs)
else
    echo "Error: .env file not found"
    exit 1
fi

# Jenkins CLI wrapper function
jenkins_cli() {
    java -jar "${JENKINS_CLI_JAR}" -s "${JENKINS_URL}" -auth "${JENKINS_USER}:${JENKINS_TOKEN}" "$@"
}

# Display help
show_help() {
    cat << EOF
Jenkins Helper Script for Vibely Food

Usage: $0 [COMMAND]

Commands:
    build           Trigger a build for vibely-food job
    status          Show the status of the last build
    logs            Show console output of the last build
    list-jobs       List all Jenkins jobs
    job-info        Show detailed job information
    stop-build      Stop the current running build
    enable-job      Enable the job
    disable-job     Disable the job
    version         Show Jenkins version
    help            Show this help message

Examples:
    $0 build
    $0 status
    $0 logs

EOF
}

# Main command handler
case "${1:-help}" in
    build)
        echo "🚀 Triggering build for vibely-food..."
        jenkins_cli build vibely-food -s -v
        echo "✅ Build triggered successfully"
        ;;

    status)
        echo "📊 Fetching build status..."
        jenkins_cli get-job vibely-food | grep -A 5 "lastBuild" || echo "No builds yet"
        ;;

    logs)
        echo "📋 Fetching console output..."
        BUILD_NUMBER=$(jenkins_cli list-builds vibely-food | head -n 1 | awk '{print $1}')
        if [ -n "$BUILD_NUMBER" ]; then
            jenkins_cli console vibely-food "$BUILD_NUMBER"
        else
            echo "No builds found"
        fi
        ;;

    list-jobs)
        echo "📝 Listing all Jenkins jobs..."
        jenkins_cli list-jobs
        ;;

    job-info)
        echo "ℹ️  Job information for vibely-food:"
        jenkins_cli get-job vibely-food
        ;;

    stop-build)
        echo "🛑 Stopping current build..."
        BUILD_NUMBER=$(jenkins_cli list-builds vibely-food | head -n 1 | awk '{print $1}')
        if [ -n "$BUILD_NUMBER" ]; then
            jenkins_cli stop-build vibely-food "$BUILD_NUMBER"
            echo "✅ Build stopped"
        else
            echo "No running builds found"
        fi
        ;;

    enable-job)
        echo "✅ Enabling vibely-food job..."
        jenkins_cli enable-job vibely-food
        echo "Job enabled"
        ;;

    disable-job)
        echo "⏸️  Disabling vibely-food job..."
        jenkins_cli disable-job vibely-food
        echo "Job disabled"
        ;;

    version)
        echo "Jenkins version:"
        jenkins_cli version
        ;;

    help|--help|-h)
        show_help
        ;;

    *)
        echo "Error: Unknown command '$1'"
        echo ""
        show_help
        exit 1
        ;;
esac
