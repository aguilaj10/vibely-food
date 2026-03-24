# Jenkins CI/CD Quick Start for Vibely Food

## What's Been Set Up

This repository now includes complete Jenkins CI/CD configuration:

1. **Jenkinsfile** - The main pipeline definition
2. **docs/JENKINS_SETUP.md** - Comprehensive setup guide
3. **scripts/setup-jenkins.sh** - Interactive setup helper script
4. **jenkins-job-config.xml** - Importable job configuration

## Quick Start (3 Steps)

### Step 1: Run the Setup Helper

```bash
cd /home/jonathan/desarrollo/vibely-food
./scripts/setup-jenkins.sh
```

This will:
- Check if Jenkins is running
- Display the admin password (if first time)
- Show quick links to Jenkins configuration pages
- Guide you through the setup process

### Step 2: Configure Jenkins

1. **Open Jenkins**: http://localhost:8081

2. **Install Required Plugins**:
   - Go to: Manage Jenkins → Manage Plugins → Available
   - Search and install:
     - Git Plugin
     - Pipeline Plugin
     - Gradle Plugin
     - JUnit Plugin
     - HTML Publisher Plugin
   - Restart Jenkins after installation

3. **Configure JDK 17**:
   - Go to: Manage Jenkins → Global Tool Configuration
   - Add JDK → Name: `JDK17`
   - Check "Install automatically"
   - Save

4. **Add GitHub Credentials**:
   - Go to: Manage Jenkins → Manage Credentials
   - Click: (global) → Add Credentials
   - Kind: `SSH Username with private key`
   - ID: `github-ssh-key`
   - Username: `git`
   - Private Key: Paste your SSH private key
   - Save

### Step 3: Create the Pipeline Job

#### Option A: Using Web UI

1. Go to Jenkins Dashboard → New Item
2. Name: `vibely-food`
3. Type: **Pipeline**
4. Configure:
   - Description: `Vibely Food - Kotlin Multiplatform Project`
   - Build Triggers: ✅ Poll SCM: `H/5 * * * *`
   - Pipeline:
     - Definition: **Pipeline script from SCM**
     - SCM: **Git**
     - Repository URL: `git@github.com:aguilaj10/vibely-food.git`
     - Credentials: `github-ssh-key`
     - Branch: `*/main`
     - Script Path: `Jenkinsfile`
5. Save and click "Build Now"

#### Option B: Import XML Configuration

```bash
# Using Jenkins CLI (if installed)
java -jar jenkins-cli.jar -s http://localhost:8081/ create-job vibely-food < jenkins-job-config.xml

# Or copy directly to Jenkins
docker cp jenkins-job-config.xml jenkins:/var/jenkins_home/jobs/vibely-food/config.xml
docker exec jenkins chown -R jenkins:jenkins /var/jenkins_home/jobs/vibely-food
docker restart jenkins
```

## Pipeline Features

The Jenkins pipeline automatically:

### Build Stages:
1. ✅ **Checkout** - Clone repository
2. ✅ **Setup** - Configure environment
3. ✅ **Install Git Hooks** - Set up pre-commit hooks
4. ✅ **Lint** (Parallel)
   - KtLint formatting checks
   - Detekt static analysis
5. ✅ **Build** - Build all modules
6. ✅ **Test** - Run unit tests and publish results
7. ✅ **Code Quality Reports** - Publish HTML reports
8. ✅ **Archive Artifacts** - Save JARs, APKs (on main/develop)
9. ✅ **Deploy to Staging** - Deploy develop branch (customize)
10. ✅ **Deploy to Production** - Deploy main branch (customize)

### Automatic Features:
- Test result publishing (JUnit format)
- Code coverage reports (JaCoCo)
- Detekt and KtLint HTML reports
- Artifact archiving
- Build notifications (configure email/Slack)
- Workspace cleanup after builds

## Customization

### Environment Variables

Edit `Jenkinsfile` to customize:

```groovy
environment {
    JAVA_HOME = tool name: 'JDK17', type: 'jdk'
    GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.caching=true'
    ANDROID_HOME = '/opt/android-sdk'  // Update this
}
```

### Deployment Stages

Update the deployment stages in `Jenkinsfile`:

```groovy
stage('Deploy to Staging') {
    when { branch 'develop' }
    steps {
        sh './gradlew :server:deployStaging'
        // Or use Docker, Kubernetes, etc.
    }
}
```

### Notifications

Uncomment and configure in `Jenkinsfile`:

```groovy
post {
    success {
        emailext subject: "Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build successful!",
                 to: "team@example.com"
    }
}
```

## Webhook Setup (Recommended)

For instant builds on push:

1. **In GitHub**:
   - Go to: https://github.com/aguilaj10/vibely-food/settings/hooks
   - Add webhook:
     - Payload URL: `http://your-public-ip:8081/github-webhook/`
     - Content type: `application/json`
     - Events: Just the push event
   - Save

2. **In Jenkins Job**:
   - Build Triggers: ✅ GitHub hook trigger for GITScm polling

**Note**: For local Jenkins, you'll need a public URL (use ngrok or similar for testing).

## Troubleshooting

### Jenkins not accessible?
```bash
docker ps | grep jenkins
docker logs jenkins
```

### Need admin password?
```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### SSH key not working?
```bash
# Test SSH connection
ssh -T git@github.com

# Check key is added to GitHub
# https://github.com/settings/keys
```

### Build fails with "Permission denied: gradlew"?
The Jenkinsfile already handles this, but if needed:
```bash
git update-index --chmod=+x gradlew
git commit -m "Make gradlew executable"
```

### Out of memory during build?
Increase in `Jenkinsfile`:
```groovy
GRADLE_OPTS = '-Dorg.gradle.daemon=false -Xmx4g'
```

## Monitoring

### View Build Status:
- Dashboard: http://localhost:8081
- Blue Ocean: http://localhost:8081/blue/organizations/jenkins/vibely-food/activity

### Check Console Output:
- Click build number → Console Output
- Debug with `--stacktrace` flag in Gradle commands

### Build History:
- View trends in dashboard
- Test results over time
- Build duration analysis

## Next Steps

1. ✅ Run `./scripts/setup-jenkins.sh`
2. ✅ Configure Jenkins plugins and tools
3. ✅ Create pipeline job
4. ✅ Trigger first build
5. ⚙️ Set up webhooks
6. ⚙️ Configure notifications
7. ⚙️ Customize deployment stages

## Resources

- **Detailed Setup**: `docs/JENKINS_SETUP.md`
- **Jenkins Documentation**: https://www.jenkins.io/doc/
- **Pipeline Syntax**: https://www.jenkins.io/doc/book/pipeline/syntax/
- **Gradle Plugin**: https://plugins.jenkins.io/gradle/

## Support

For issues:
1. Check Jenkins console output
2. Review `docs/JENKINS_SETUP.md`
3. Check Jenkins system logs: Manage Jenkins → System Log
4. Verify plugin installations
5. Test Git/SSH connectivity

---

**Ready to build? Run the setup script and start your first build!** 🚀
