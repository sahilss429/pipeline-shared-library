def checkFolderForDiffs() {
        def paths = []
        paths = sh(script: '/bin/bash change.sh', returnStdout: true).trim()
        return paths
}

import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

def call(String masterBuild) {
def tokens = "${masterBuild}".tokenize('/')
def team = tokens[0]
def repo = tokens[1]
def BRANCH = tokens[2]
def REPO_URL = "git@github.com:${team}/${repo}.git"

properties([
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: "${REPO_URL}"],
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

node('dood') {
    stage('Checkout Code') {
        checkout scm
    }
    stage('create job scripts') {
	echo "${REPO_URL}"
	sh("/bin/bash create_jobs.sh ${REPO_URL}")
    }
    stage('Creating Jobs') {
	def ScriptApproval scriptApproval = ScriptApproval.get()
	scriptApproval.pendingScripts.each {
    	    scriptApproval.approveScript(it.hash)
	}

        jobDsl targets: '''jobs/*_1.groovy
 			   jobs/*_1_*.groovy
			   jobs/*_2_*.groovy
			   jobs/*_3_*.groovy
			   jobs/*_4_*.groovy
			   jobs/*_5_*.groovy'''
    }
    stage('Are we building?') {
	def job_paths = checkFolderForDiffs()
       String[] dir_paths = job_paths.tokenize()
	echo "$dir_paths"
        sh 'git log -1 --pretty=%B > git_message'
        if (!readFile('git_message').startsWith('[blacksmith]') && dir_paths != "") {
            stage('Setup Gitconfig') {
                sh("git remote set-url origin ${REPO_URL}")
                sh("git config user.email blacksmith@jenkins.local")
                sh("git config user.name 'BlackSmith'")
            }
            stage('SecondaryBuild Trigger') {
                try {
		    def arrayLength = dir_paths.size()
		    echo "$arrayLength"
                    for (int i = 0; i < arrayLength; i++) {
			echo "${repo}/${dir_paths[i]}"
        		build "${dir_paths[i]}"
		    }
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
