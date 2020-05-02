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
//library identifier: '', retriever: legacySCM([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:sahilss429/pipeline-shared-library.git']]])

properties([
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'git@github.com:sahilss429/stacks-vertical.git'],
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

node('dood') {
    stage('Checkout Code') {
        checkout scm
    }
    stage('create job scripts') {
	sh('printenv')
	sh("/bin/bash create_jobs.sh git@github.com:sahilss429/stacks-vertical.git")
    }
    stage('Creating Jobs') {
        jobDsl targets: '''jobs/*_1.groovy
 			   jobs/*_1_*.groovy
			   jobs/*_2_*.groovy
			   jobs/*_3_*.groovy
			   jobs/*_4_*.groovy
			   jobs/*_5_*.groovy'''
//        def causes = currentBuild.rawBuild.getCauses()
//        for(cause in causes) {
//           if (cause.class.toString().contains("UpstreamCause")) {
//              println "This job was caused by job " + cause.upstreamProject
//           } else {
//              println "Root cause : " + cause.toString()
//           }
//        }
//	build job: '../seed', parameters: [string(name: 'REPO_URL', value: 'git@github.com:sahilss429/stacks-vertical.git')]
    }
    stage('Values') {
        tokens = "${env.JOB_NAME}".tokenize('/')
        repo = tokens[0]
        env = tokens[1]
        service = tokens[3]
    }
    stage('Are we building?') {
        paths = checkFolderForDiffs()
	echo "paths= ${paths}"
        sh 'git log -1 --pretty=%B > git_message'
        if (!readFile('git_message').startsWith('[blacksmith]') && paths != "") {
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
