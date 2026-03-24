# Fix Docker Permission Issue

## Problem
```
permission denied while trying to connect to the docker API at unix:///var/run/docker.sock
```

## You're already in the docker group!
Your user is already a member of the `docker` group, but the session hasn't refreshed yet.

## Solutions (Choose One)

### Solution 1: Use Auto-Fix Wrapper (Easiest) ⭐
```bash
cd /home/jonathan/desarrollo/vibely-food
./scripts/run-jenkins-setup.sh
```
This wrapper script automatically handles the group refresh for you!

### Solution 2: Refresh Group Membership Manually
```bash
newgrp docker
```
Then run the script again:
```bash
cd /home/jonathan/desarrollo/vibely-food
./scripts/setup-jenkins.sh
```

### Solution 3: Run in One Command
```bash
newgrp docker << 'SCRIPT'
cd /home/jonathan/desarrollo/vibely-food
./scripts/setup-jenkins.sh
SCRIPT
```

### Solution 4: Log Out and Log Back In (Most Reliable)
1. Log out of your current session
2. Log back in
3. Run the script again

## Verify It's Fixed
After applying any solution, verify with:
```bash
docker ps
```

If you see the Jenkins container (or any docker output without errors), it's fixed!

## Why This Happens
When a user is added to the `docker` group, the change doesn't take effect until:
- The user logs out and back in, OR
- The user starts a new shell session with the updated groups

Your current shell session still has the old group membership cached.

## Updated Scripts
The setup scripts have been improved to:
- ✅ Detect the permission issue automatically
- ✅ Provide clear instructions on how to fix it
- ✅ Include an auto-fix wrapper script (`run-jenkins-setup.sh`)
