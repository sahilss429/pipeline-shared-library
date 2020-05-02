def checkFolderForDiffs() {
        def paths = []
        paths = sh(script: 'for i in `find . -type f -name vars.tfvars|awk -F\'vars.tfvars\' \'{print $1}\'|sort`; do git diff --quiet --exit-code HEAD~1..HEAD $i; if [ $? == 1 ]; then  echo $i; fi ; done', returnStdout: true).trim()
        echo "paths= ${paths}"
        return paths
}

def buildCommitedApps(paths) {
    echo "Triggering each build for which change is committed."
    for (int i = 0; i < paths.size(); i++) {
        build '${paths[i]}'
    }
}

def call(String masterBuild) {

def tokens = "${masterBuild}".tokenize('/')
def team = tokens[0]
def repo = tokens[1]
def BRANCH = tokens[2]
def REPO_URL = "git@github.com:/${team}/${repo}.git"

library identifier: 'pipeline-shared-library@master', retriever: modernSCM([$class: 'GitSCMSource', credentialsId: '', remote: 'git@github.com:sahilss429/pipeline-shared-library.git', traits: [gitBranchDiscovery()]])

properties([
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: "${REPO_URL}"],
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

node('dood') {
//    stage('Values') {
//        tokens = "${masterBuild}".tokenize('/')
//        team = tokens[0]
//        repo = tokens[1]
//        BRANCH = tokens[2]
//	REPO_URL = "git@github.com:/${team}/${repo}.git"
//    }
    stage('Checkout Code') {
        checkout scm
    }
    stage('create job scripts') {
	sh('printenv')
	sh("/bin/bash create_jobs.sh ${REPO_URL}")
    }
    stage('Creating Jobs') {
        jobDsl targets: '''jobs/*_1.groovy
 			   jobs/*_1_*.groovy
			   jobs/*_2_*.groovy
			   jobs/*_3_*.groovy
			   jobs/*_4_*.groovy
			   jobs/*_5_*.groovy'''
    }
    stage('Are we building?') {
        paths = checkFolderForDiffs()
	echo "paths= ${paths}"
        sh 'git log -1 --pretty=%B > git_message'
        if (!readFile('git_message').startsWith('[blacksmith]') && paths != "") {
            stage('Setup Gitconfig') {
                sh("git remote set-url origin ${REPO_URL}")
                sh("git config user.email blacksmith@jenkins.local")
                sh("git config user.name 'BlackSmith'")
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