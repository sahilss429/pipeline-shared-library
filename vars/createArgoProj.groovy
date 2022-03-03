def call(String masterBuild) {
def git_infra_repo = scm.userRemoteConfigs[0].url
def infra_repo = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
def tokens = "${masterBuild}".tokenize('/')
def aws_Account = tokens[0]
def slackChannel = tokens[1]
def buildstatus = "STARTED"
podTemplate(
    name: "jenkins_slave",
    label: "jenkins_slave",
    containers: [
        containerTemplate(name: 'jnlp', image: "${aws_Account}.dkr.ecr.ap-south-1.amazonaws.com/jenkins-slave:latest", alwaysPullImage: true, command: '/bin/sh -c ', args: '/usr/local/bin/jenkins-slave', ttyEnabled: true, resourceRequestCpu: '100m', resourceLimitCpu: '200m', resourceRequestMemory: '800Mi', resourceLimitMemory: '1200Mi')
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
  parameters([
    string(defaultValue: 'none', description: 'Name of Service: Exactly same as in your app git repo', name: 'SERVICE_NAME', trim: false),
    ])
  ])
  stage('CleanWorkspace') {
    sh('whoami && pwd')
  }
  stage('Checkout Code') {
    checkout([$class: 'GitSCM', branches: [[name: "*/kubernetes"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'LocalBranch', localBranch: "kubernetes"]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'test', url: "${git_infra_repo}"]]])
    notifySlack(buildstatus, slackChannel)
  }
  stage('Are we building?') {
    if ( params.SERVICE_NAME != "none") {
      stage('Project Create Production') {
        ansiColor('xterm') {
          try {
            withCredentials([usernamePassword(credentialsId: "argosecret", passwordVariable: 'argopassword', usernameVariable: 'argouser')]) {
              sh("cd templates && make INFRA_REPO=${infra_repo} SERVICE_NAME=${SERVICE_NAME} proj-create")
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
