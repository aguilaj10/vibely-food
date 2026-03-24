#!/bin/bash

# Wrapper script to run Jenkins setup with proper Docker permissions
# This script handles the docker group membership refresh automatically

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_DIR="$(dirname "$SCRIPT_DIR")"

# Check if we can access Docker
if docker ps &> /dev/null; then
    # Docker access works, run the setup script directly
    exec "${REPO_DIR}/scripts/setup-jenkins.sh"
else
    # Docker access doesn't work, need to refresh group membership
    echo "🔧 Docker group membership needs to be refreshed..."
    echo "   Starting a new shell session with updated permissions..."
    echo ""

    # Use newgrp to start a new shell with docker group active
    exec newgrp docker << EOF
cd "${REPO_DIR}"
./scripts/setup-jenkins.sh
EOF
fi
