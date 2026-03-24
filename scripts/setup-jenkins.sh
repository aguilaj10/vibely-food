#!/bin/bash

# Jenkins Job Setup Script for Vibely Food
# This script helps you configure Jenkins for the vibely-food repository

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Jenkins Setup for Vibely Food${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check Docker permissions first
echo -e "${YELLOW}Checking Docker permissions...${NC}"
if ! docker ps &> /dev/null; then
    echo -e "${RED}✗ Cannot access Docker${NC}"
    echo -e ""
    echo -e "${YELLOW}Your user is in the docker group, but the session needs to be refreshed.${NC}"
    echo -e ""
    echo -e "Quick fix (choose one):"
    echo -e ""
    echo -e "1. Run this command to refresh group membership:"
    echo -e "   ${BLUE}newgrp docker${NC}"
    echo -e "   Then run this script again"
    echo -e ""
    echo -e "2. Or run in one command:"
    echo -e "   ${BLUE}newgrp docker << 'SCRIPT'"
    echo -e "   cd $(pwd)"
    echo -e "   ./scripts/setup-jenkins.sh"
    echo -e "   SCRIPT${NC}"
    echo -e ""
    echo -e "3. Or log out and log back in (most reliable)"
    echo -e ""
    echo -e "See ${BLUE}FIX_DOCKER_PERMISSION.md${NC} for more details."
    exit 1
fi
echo -e "${GREEN}✓ Docker access confirmed${NC}"

# Check if Jenkins is running
echo -e "\n${YELLOW}Checking Jenkins status...${NC}"
if docker ps | grep -q jenkins; then
    echo -e "${GREEN}✓ Jenkins container is running${NC}"
    JENKINS_CONTAINER=$(docker ps --filter "name=jenkins" --format "{{.Names}}" | head -1)
    echo -e "  Container name: ${JENKINS_CONTAINER}"
else
    echo -e "${RED}✗ Jenkins container is not running${NC}"
    echo -e "  Start Jenkins with: ${BLUE}docker-compose up -d${NC}"
    exit 1
fi

# Get Jenkins URL
JENKINS_URL="http://localhost:8081"
echo -e "\n${YELLOW}Jenkins URL:${NC} ${JENKINS_URL}"

# Check if Jenkins is accessible
echo -e "\n${YELLOW}Checking Jenkins accessibility...${NC}"
if curl -s -o /dev/null -w "%{http_code}" "${JENKINS_URL}" | grep -q "200\|403"; then
    echo -e "${GREEN}✓ Jenkins is accessible${NC}"
else
    echo -e "${RED}✗ Jenkins is not accessible${NC}"
    echo -e "  Please wait for Jenkins to fully start"
    exit 1
fi

# Display initial admin password if exists
echo -e "\n${YELLOW}Checking for initial admin password...${NC}"
ADMIN_PASSWORD=$(docker exec "${JENKINS_CONTAINER}" cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null || echo "")
if [ -n "${ADMIN_PASSWORD}" ]; then
    echo -e "${GREEN}Initial Admin Password:${NC} ${ADMIN_PASSWORD}"
    echo -e "${YELLOW}Note:${NC} Use this password for first-time Jenkins setup"
else
    echo -e "${GREEN}✓ Jenkins already configured${NC}"
fi

echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}  Required Jenkins Plugins${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Install these plugins via:"
echo "  ${JENKINS_URL}/pluginManager/available"
echo ""
echo "Essential plugins:"
echo "  ✓ Git Plugin"
echo "  ✓ Pipeline Plugin"
echo "  ✓ Pipeline: Stage View Plugin"
echo "  ✓ Gradle Plugin"
echo "  ✓ JUnit Plugin"
echo "  ✓ HTML Publisher Plugin"
echo "  ✓ Workspace Cleanup Plugin"
echo ""
echo "Recommended plugins:"
echo "  ✓ Blue Ocean"
echo "  ✓ Email Extension Plugin"
echo "  ✓ JaCoCo Plugin"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Jenkins Configuration Steps${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "1. Configure JDK 17:"
echo "   ${JENKINS_URL}/configureTools/"
echo "   → Add JDK → Name: 'JDK17'"
echo "   → Install automatically (recommended)"
echo ""
echo "2. Add GitHub Credentials:"
echo "   ${JENKINS_URL}/credentials/store/system/domain/_/newCredentials"
echo "   → Kind: 'SSH Username with private key'"
echo "   → ID: 'github-ssh-key'"
echo "   → Username: 'git'"
echo "   → Private Key: Add your SSH key"
echo ""
echo "3. Create Pipeline Job:"
echo "   ${JENKINS_URL}/view/all/newJob"
echo "   → Name: 'vibely-food'"
echo "   → Type: 'Pipeline'"
echo "   → Pipeline from SCM:"
echo "     • SCM: Git"
echo "     • Repository URL: git@github.com:aguilaj10/vibely-food.git"
echo "     • Credentials: github-ssh-key"
echo "     • Branch: */main"
echo "     • Script Path: Jenkinsfile"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Quick Links${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Jenkins Dashboard:     ${JENKINS_URL}/"
echo "Plugin Manager:        ${JENKINS_URL}/pluginManager/"
echo "Global Configuration:  ${JENKINS_URL}/configure"
echo "Tool Configuration:    ${JENKINS_URL}/configureTools/"
echo "Credentials:           ${JENKINS_URL}/credentials/"
echo "Create New Job:        ${JENKINS_URL}/view/all/newJob"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  SSH Key Setup for GitHub${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "If you don't have an SSH key yet:"
echo ""
echo "1. Generate SSH key:"
echo "   ${BLUE}ssh-keygen -t ed25519 -C \"jenkins@vibely-food\"${NC}"
echo ""
echo "2. Add public key to GitHub:"
echo "   • Copy: ${BLUE}cat ~/.ssh/id_ed25519.pub${NC}"
echo "   • Go to: https://github.com/settings/keys"
echo "   • Add new SSH key"
echo ""
echo "3. Test connection:"
echo "   ${BLUE}ssh -T git@github.com${NC}"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Docker-based Build Agent (Optional)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "For isolated builds, you can use Docker-based agents."
echo "See docs/JENKINS_SETUP.md for Dockerfile configuration."
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Webhook Setup (Optional)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "For automatic builds on push:"
echo ""
echo "1. In GitHub repository settings:"
echo "   https://github.com/aguilaj10/vibely-food/settings/hooks"
echo ""
echo "2. Add webhook:"
echo "   • Payload URL: ${JENKINS_URL}/github-webhook/"
echo "   • Content type: application/json"
echo "   • Events: Just the push event"
echo ""
echo "3. Enable in Jenkins job:"
echo "   ✓ GitHub hook trigger for GITScm polling"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Setup script completed!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Next steps:"
echo -e "  1. Open Jenkins: ${BLUE}${JENKINS_URL}${NC}"
echo -e "  2. Install required plugins"
echo -e "  3. Configure JDK 17"
echo -e "  4. Add GitHub credentials"
echo -e "  5. Create pipeline job for vibely-food"
echo -e "  6. Run your first build!"
echo ""
echo -e "For detailed instructions, see: ${BLUE}docs/JENKINS_SETUP.md${NC}"
echo ""
