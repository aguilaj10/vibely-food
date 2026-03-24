#!/bin/bash

# Jenkins Helper Script for Vibely Food
# Provides convenient commands to interact with Jenkins

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '#' | xargs)
else
    echo "Error: .env file not found"
    echo "Please create a .env file with Jenkins credentials:"
    echo ""
    echo "JENKINS_URL=http://localhost:8081"
    echo "JENKINS_USER=your-username"
    echo "JENKINS_TOKEN=your-token"
    echo "JENKINS_CLI_JAR=./jenkins-cli.jar"
    exit 1
fi

# Jenkins CLI wrapper function
jenkins_cli() {
    java -jar "${JENKINS_CLI_JAR}" -s "${JENKINS_URL}" -auth "${JENKINS_USER}:${JENKINS_TOKEN}" "$@"
}

# Display help
show_help() {
    cat << EOF
${BLUE}Jenkins Helper Script for Vibely Food${NC}

${YELLOW}Usage:${NC} $0 [COMMAND]

${YELLOW}Build Commands:${NC}
    build           Trigger a build for vibely-food job
    status          Show the status of the last build
    logs            Show console output of the last build
    stop-build      Stop the current running build

${YELLOW}Job Management:${NC}
    list-jobs       List all Jenkins jobs
    job-info        Show detailed job information
    enable-job      Enable the job
    disable-job     Disable the job

${YELLOW}System:${NC}
    version         Show Jenkins version
    help            Show this help message

${YELLOW}Examples:${NC}
    $0 build
    $0 logs
    $0 status

${YELLOW}Jenkins Dashboard:${NC}
    Web UI:     ${JENKINS_URL}
    Blue Ocean: ${JENKINS_URL}/blue/organizations/jenkins/vibely-food/

EOF
}

# Main command handler
case "${1:-help}" in
    build)
        echo -e "${BLUE}🚀 Triggering build for vibely-food...${NC}"
        jenkins_cli build vibely-food -s -v
        echo -e "${GREEN}✅ Build triggered successfully${NC}"
        ;;

    status)
        echo -e "${BLUE}📊 Fetching build status...${NC}"
        jenkins_cli get-job vibely-food | grep -A 5 "lastBuild" || echo "No builds yet"
        ;;

    logs)
        echo -e "${BLUE}📋 Fetching console output...${NC}"
        BUILD_NUMBER=$(jenkins_cli list-builds vibely-food 2>/dev/null | head -n 1 | awk '{print $1}')
        if [ -n "$BUILD_NUMBER" ]; then
            jenkins_cli console vibely-food "$BUILD_NUMBER"
        else
            echo "No builds found"
        fi
        ;;

    list-jobs)
        echo -e "${BLUE}📝 Listing all Jenkins jobs...${NC}"
        jenkins_cli list-jobs
        ;;

    job-info)
        echo -e "${BLUE}ℹ️  Job information for vibely-food:${NC}"
        jenkins_cli get-job vibely-food
        ;;

    stop-build)
        echo -e "${YELLOW}🛑 Stopping current build...${NC}"
        BUILD_NUMBER=$(jenkins_cli list-builds vibely-food 2>/dev/null | head -n 1 | awk '{print $1}')
        if [ -n "$BUILD_NUMBER" ]; then
            jenkins_cli stop-build vibely-food "$BUILD_NUMBER"
            echo -e "${GREEN}✅ Build stopped${NC}"
        else
            echo "No running builds found"
        fi
        ;;

    enable-job)
        echo -e "${GREEN}✅ Enabling vibely-food job...${NC}"
        jenkins_cli enable-job vibely-food
        echo "Job enabled"
        ;;

    disable-job)
        echo -e "${YELLOW}⏸️  Disabling vibely-food job...${NC}"
        jenkins_cli disable-job vibely-food
        echo "Job disabled"
        ;;

    version)
        echo -e "${BLUE}Jenkins version:${NC}"
        jenkins_cli version
        ;;

    help|--help|-h)
        show_help
        ;;

    *)
        echo -e "${YELLOW}Error: Unknown command '$1'${NC}"
        echo ""
        show_help
        exit 1
        ;;
esac
