def call(String masterBuild) {
def git_app_repo = scm.userRemoteConfigs[0].url
def SERVICE_NAME = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
def git_app_branch = "${env.BRANCH_NAME}"
def buildstatus = "STARTED"
node('master') {
properties([
  buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')),
  disableConcurrentBuilds(),
  ])
  stage('CleanWorkspace') {
    cleanWs()
    sh('whoami && pwd')
  }
  stage('Checkout Code') {
    checkout([$class: 'GitSCM', branches: [[name: "*/${git_app_branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'LocalBranch', localBranch: "${git_app_branch}"]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'test', url: "${git_app_repo}"]]])
  }
  stage('Are we building?') {
    if (git_app_branch == "staging" || git_app_branch == "preprod" || git_app_branch == "production" || git_app_branch == "master") {
      stage('Build Stage Production') {
        ansiColor('xterm') {
          try {
              sh("docker build -t ${SERVICE_NAME} .")
          } catch(Exception e) {
              currentBuild.result = 'FAILURE'
              throw e
          } finally {
              //notifySlack(currentBuild.result, slackChannel)
          }
        }
      }
    }else {
      stage('Build Stage Non Production') {
        ansiColor('xterm') {
          try {
              sh("docker build -t ${SERVICE_NAME} .")
          } catch(Exception e) {
              currentBuild.result = 'FAILURE'
              throw e
          } finally {
              //notifySlack(currentBuild.result, slackChannel)
          }
        }
      }
    }
  }
}
}
