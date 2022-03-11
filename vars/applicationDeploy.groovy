def call(String masterBuild) {
def git_app_repo = scm.userRemoteConfigs[0].url
def SERVICE_NAME = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
def tokens = "${masterBuild}".tokenize('/')
def infra_repo = tokens[0]
def slackChannel = tokens[1]
def awsAccount = tokens[2]
def git_app_branch = "${env.BRANCH_NAME}"
def buildstatus = "STARTED"
def git_infra_repo = "git@github.com:sahilss429/${infra_repo}.git"
podTemplate(
    name: "jenkins_slave",
    label: "jenkins_slave",
    containers: [
      containerTemplate(name: 'jnlp', image: '${awsAccount}.dkr.ecr.ap-south-1.amazonaws.com/jenkins-slave:latest', alwaysPullImage: true, command: '/bin/sh -c ', args: '/usr/local/bin/jenkins-slave', ttyEnabled: true, resourceRequestCpu: '400m', resourceLimitCpu: '800m', resourceRequestMemory: '1200Mi', resourceLimitMemory: '2000Mi')
    ],
    volumes: [
        hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
    ],
    serviceAccount: 'jenkins',
    {
node('jenkins_slave') {
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
    checkout([$class: 'GitSCM', branches: [[name: "*/kubernetes"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "./${infra_repo}"]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'test', url: "${git_infra_repo}"]]])
    //notifySlack(buildstatus, slackChannel)
  }
  stage('Are we building?') {
    if (git_app_branch == "staging" || git_app_branch == "preprod" || git_app_branch == "production" || git_app_branch == "master") {
      stage('Production Equivalent Environment') {
        ansiColor('xterm') {
          try {
            withCredentials([usernamePassword(credentialsId: "argosecret", passwordVariable: 'argopassword', usernameVariable: 'argouser'),sshUserPrivateKey(credentialsId: "test", keyFileVariable: 'private_key')]) {
              sh("make INFRA_REPO=${infra_repo} SERVICE_NAME=${SERVICE_NAME} install")
              sh("make INFRA_REPO=${infra_repo} SERVICE_NAME=${SERVICE_NAME} production")
            }
          } catch(Exception e) {
              currentBuild.result = 'FAILURE'
              throw e
          } finally {
              //notifySlack(currentBuild.result, slackChannel)
          }
        }
      }
    }else {
      stage('Non Production Environment') {
        ansiColor('xterm') {
          try {
            withCredentials([usernamePassword(credentialsId: "argosecret", passwordVariable: 'argopassword', usernameVariable: 'argouser'),sshUserPrivateKey(credentialsId: "test", keyFileVariable: 'private_key')]) {
              sh("make INFRA_REPO=${infra_repo} SERVICE_NAME=${SERVICE_NAME} install")
              sh("make INFRA_REPO=${infra_repo} SERVICE_NAME=${SERVICE_NAME} nonprod")
            }
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
)
}
