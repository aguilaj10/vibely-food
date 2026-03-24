# Jenkins CI/CD Setup Guide for Vibely Food

This guide will help you configure Jenkins for the Vibely Food Kotlin Multiplatform project.

## Prerequisites

### 1. Jenkins Plugins Required

Install the following plugins in Jenkins (Manage Jenkins → Manage Plugins):

#### Essential Plugins:
- **Git Plugin** - For Git repository integration
- **Pipeline Plugin** - For pipeline support
- **Pipeline: Stage View Plugin** - For visual pipeline stages
- **Gradle Plugin** - For Gradle build support
- **JUnit Plugin** - For test result publishing
- **HTML Publisher Plugin** - For code quality reports
- **Workspace Cleanup Plugin** - For workspace management

#### Optional but Recommended:
- **Blue Ocean** - Modern UI for pipelines
- **Email Extension Plugin** - For email notifications
- **Slack Notification Plugin** - For Slack notifications
- **JaCoCo Plugin** - For code coverage reports
- **Android Lint Plugin** - For Android-specific linting
- **Pipeline Utility Steps** - Additional pipeline utilities

### 2. Jenkins Global Tool Configuration

Go to **Manage Jenkins → Global Tool Configuration** and configure:

#### JDK Installation
1. Add JDK → Name: `JDK17`
2. Choose one of:
   - **Install automatically** from adoptium.net (recommended)
   - **JAVA_HOME**: Point to existing JDK 17+ installation

#### Gradle (Optional - project uses wrapper)
1. Add Gradle → Name: `Gradle 9`
2. Install automatically or point to existing installation

### 3. Jenkins Credentials Setup

Go to **Manage Jenkins → Manage Credentials** and add:

#### GitHub SSH Key
1. Kind: `SSH Username with private key`
2. ID: `github-ssh-key`
3. Username: `git`
4. Private Key: Add your SSH private key that has access to the repository
5. Passphrase: Enter if your key has one

#### Or GitHub Personal Access Token (Alternative)
1. Kind: `Username with password`
2. ID: `github-pat`
3. Username: Your GitHub username
4. Password: Your GitHub Personal Access Token

## Creating the Jenkins Pipeline Job

### Option 1: Using Jenkins UI

1. **Create New Item**
   - Go to Jenkins Dashboard → New Item
   - Enter name: `vibely-food`
   - Select: **Pipeline**
   - Click OK

2. **Configure General Settings**
   - Description: `Vibely Food - Kotlin Multiplatform Project`
   - ✅ Discard old builds (keep last 10)
   - ✅ GitHub project: `https://github.com/aguilaj10/vibely-food`

3. **Configure Build Triggers**
   - ✅ Poll SCM: `H/5 * * * *` (check every 5 minutes)
   - Or ✅ GitHub hook trigger for GITScm polling (if webhook configured)

4. **Configure Pipeline**
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: `git@github.com:aguilaj10/vibely-food.git`
   - Credentials: Select your GitHub SSH key
   - Branch Specifier: `*/main` (or `*/develop` for dev branch)
   - Script Path: `Jenkinsfile`

5. **Save**

### Option 2: Using Jenkins Configuration as Code (JCasC)

Create a job configuration file and import it via Jenkins CLI or JCasC plugin.

## Jenkins Pipeline Features

The provided Jenkinsfile includes:

### 1. **Checkout Stage**
- Clones the repository
- Displays Git commit info

### 2. **Setup Stage**
- Configures Gradle wrapper
- Displays environment versions

### 3. **Install Git Hooks Stage**
- Runs `installGitHooks` task

### 4. **Lint Stage** (Parallel)
- **KtLint Check**: Kotlin code formatting
- **Detekt**: Static code analysis

### 5. **Build Stage**
- Builds all modules
- Creates artifacts (JARs, APKs)

### 6. **Test Stage**
- Runs all unit tests
- Publishes JUnit test results
- Publishes JaCoCo coverage reports (if configured)

### 7. **Code Quality Reports Stage** (Parallel)
- Publishes Detekt HTML reports
- Publishes KtLint HTML reports

### 8. **Archive Artifacts Stage** (Conditional)
- Archives build outputs on main/develop branches
- Archives JARs, APKs, and distribution files

### 9. **Deploy Stages** (Conditional)
- **Deploy to Staging**: Runs on `develop` branch
- **Deploy to Production**: Runs on `main` branch

## Environment Variables

The pipeline uses these environment variables (customize as needed):

```groovy
JAVA_HOME = tool name: 'JDK17', type: 'jdk'
GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.caching=true'
ANDROID_HOME = '/opt/android-sdk'  // Update this path
```

## Android SDK Setup (Optional)

If building Android targets:

1. Install Android SDK on Jenkins agent:
   ```bash
   sudo apt-get update
   sudo apt-get install -y android-sdk
   ```

2. Update `ANDROID_HOME` in Jenkinsfile
3. Accept Android SDK licenses:
   ```bash
   yes | sdkmanager --licenses
   ```

## Docker-based Jenkins Agent (Recommended)

For better isolation and reproducibility, you can run builds in Docker containers.

### Dockerfile for Build Agent

```dockerfile
FROM gradle:8.5-jdk17

# Install Android SDK (if needed)
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set up Android SDK
ENV ANDROID_SDK_ROOT=/opt/android-sdk
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip commandlinetools-linux-9477386_latest.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm commandlinetools-linux-9477386_latest.zip

ENV PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${PATH}"

# Accept licenses
RUN yes | sdkmanager --licenses || true

WORKDIR /workspace
```

## Webhook Setup (Recommended)

For instant builds on push:

1. **GitHub Settings**
   - Go to repository → Settings → Webhooks
   - Add webhook
   - Payload URL: `http://your-jenkins-url:8081/github-webhook/`
   - Content type: `application/json`
   - Select: "Just the push event"
   - Active: ✅

2. **Jenkins Configuration**
   - In job settings, enable: "GitHub hook trigger for GITScm polling"

## Notifications Setup

### Email Notifications

1. Configure SMTP in **Manage Jenkins → Configure System**
2. Uncomment email sections in Jenkinsfile `post` blocks
3. Update email addresses

### Slack Notifications

1. Install Slack Notification Plugin
2. Configure Slack in **Manage Jenkins → Configure System**
3. Add Slack notification steps to Jenkinsfile

## Troubleshooting

### Issue: Permission Denied on gradlew

**Solution**: Jenkinsfile already includes `chmod +x ./gradlew`

### Issue: Out of Memory

**Solution**: Increase heap size in `GRADLE_OPTS`:
```groovy
GRADLE_OPTS = '-Dorg.gradle.daemon=false -Xmx2g'
```

### Issue: Android SDK Not Found

**Solution**:
1. Install Android SDK on Jenkins agent
2. Update `ANDROID_HOME` path in Jenkinsfile
3. Accept SDK licenses

### Issue: Git Clone Fails

**Solution**:
1. Verify SSH key is correctly added to Jenkins credentials
2. Test SSH connection from Jenkins agent:
   ```bash
   ssh -T git@github.com
   ```

### Issue: Tests Fail in CI but Pass Locally

**Solution**:
1. Check environment differences
2. Ensure all dependencies are available
3. Review test output in Jenkins console

## Monitoring and Maintenance

### Build Health
- Monitor build success rate in Jenkins dashboard
- Review trends in Blue Ocean interface

### Performance
- Check build duration trends
- Optimize Gradle caching if builds are slow
- Consider using Gradle Build Cache

### Artifacts
- Regularly clean old artifacts to save disk space
- Archive only necessary files

## Advanced Configuration

### Multi-branch Pipeline

For better branch management:

1. Create **Multibranch Pipeline** instead of regular Pipeline
2. Jenkins will automatically discover branches with Jenkinsfile
3. Each branch gets its own build pipeline

### Parallel Builds

The Jenkinsfile already includes parallel stages for:
- Lint checks (KtLint + Detekt)
- Code quality reports

### Build Parameters

Add build parameters for flexibility:

```groovy
parameters {
    choice(name: 'BUILD_TYPE', choices: ['debug', 'release'], description: 'Build type')
    booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run tests')
    booleanParam(name: 'DEPLOY', defaultValue: false, description: 'Deploy after build')
}
```

## Next Steps

1. ✅ Install required Jenkins plugins
2. ✅ Configure JDK 17 in Global Tool Configuration
3. ✅ Add GitHub credentials
4. ✅ Create Jenkins pipeline job
5. ✅ Trigger first build
6. ⚙️ Configure webhooks (optional)
7. ⚙️ Set up notifications (optional)
8. ⚙️ Configure deployment stages (optional)

## Support

For issues or questions:
- Check Jenkins console output
- Review Jenkinsfile syntax
- Verify plugin installations
- Check Jenkins system logs: `Manage Jenkins → System Log`
