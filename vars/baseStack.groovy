def checkFolderForDiffs() {
        def paths = []
        paths = sh(script: '''for i in `find . -type f -name vars.tfvars|awk -F\'vars.tfvars\' \'{print $1}\'|sort`; do git diff --quiet --exit-code HEAD~1..HEAD $i; if [ $? == 1 ]; then  echo $i; fi ; done''', returnStdout: true).trim()
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
        jobDsl targets: '''jobs/*_1.groovy
 			   jobs/*_1_*.groovy
			   jobs/*_2_*.groovy
			   jobs/*_3_*.groovy
			   jobs/*_4_*.groovy
			   jobs/*_5_*.groovy'''
    }
    stage('Are we building?') {
	def changeLogSets = currentBuild.rawBuild.changeSets
	for (int i = 0; i < changeLogSets.size(); i++) {
    	   def entries = changeLogSets[i].items
           for (int j = 0; j < entries.length; j++) {
        	def entry = entries[j]
        	echo "${entry.commitId} by ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}"
        	def files = new ArrayList(entry.affectedFiles)
        	for (int k = 0; k < files.size(); k++) {
            	   def file = files[k]
            	   echo "  ${file.editType.name} ${file.path}"
        	}
    	   }
	}
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
