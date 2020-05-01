def checkFolderForDiffs() {
    try {
        def paths = []
        paths = sh(script: "for i in `find . -type f -name vars.tfvars|awk -F'vars.tfvars' '{print \$1}'|sort`; do git diff --quiet --exit-code HEAD~1..HEAD $i; if [ \$? == 1 ]; then  echo $i; fi ; done", returnStdout: true).trim()
        return paths
    } catch (err) {
        return true
    }
}

def buildCommitedApps(paths) {
    echo "Triggering each build for which change is committed."
    for (int i = 0; i < list.size(); i++) {
        build '${list[i]}'
    }
}

def call(String masterBuild) {

properties([
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

node('dood') {
    stage('Checkout Code') {
        checkout scm
    }
    stage('Values') {
        tokens = "${env.JOB_NAME}".tokenize('/')
        repo = tokens[0]
        env = tokens[1]
        service = tokens[3]
    }
    stage('Are we building?') {
        paths = checkFolderForDiffs()
        sh 'git log -1 --pretty=%B > git_message'
        if (!readFile('git_message').startsWith('[blacksmith]') && paths != null) {
            stage('Setup Gitconfig') {
                sh("git remote set-url origin git@github.com:sahilss429/myservice-app.git")
                sh("git config user.email sahilss429@gmail.com")
                sh("git config user.name 'sahilss429'")
            }
            stage('SecondaryBuild Trigger') {
                try {
                    buildCommitedApps(paths)
                }
                catch(err) {
                    return true
                }
                echo "Triggering Secondary Builds..."
            }
        }
    }
}
}
