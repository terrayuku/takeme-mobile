# takeme-mobile
This is the mobile app for TakeMe

### Getting Started with dev

* Cloning the project (getting the project to your local machine)
    `git clone https://github.com/terrayuku/takeme-mobile.git`

* Changing branch to the develop branch (one must make sure they work from this branch)
    `git checkout develop`
    
* Pulling new code with rebase 
    `git pull origin develop`

* After making some changes on the code base one need to 
    * Check if the files have been recognized by git (Red: Not Recognized, Lemon/Yellow: Recognized)
        `git status`
    * Add all files modified for git
        `git add .`
    * Commit to the branch
        `git commit -m "the change made"`
    * Push the change to the git repo
        `git push origin develop`
        
        
* Kick starting the build in travis
    * Visit the travis project at
        `https://travis-ci.org/terrayuku/takeme-mobile/branches`
    * Under More Options
        * Click Trigger build
 