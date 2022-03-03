def call(String masterBuild) {
def git_app_repo = scm.userRemoteConfigs[0].url
def tokens = "${masterBuild}".tokenize('/')
def aws_Account = tokens[0]
def slackChannel = tokens[1]
def buildstatus = "STARTED"
def git_app_branch = "${env.BRANCH_NAME}"
podTemplate(
    name: "jenkins_slave",
    label: "jenkins_slave",
    containers: [
        containerTemplate(name: 'jnlp', image: "${aws_Account}.dkr.ecr.ap-south-1.amazonaws.com/jenkins-slave:latest", command: '/bin/sh -c ', args: '/usr/local/bin/jenkins-slave', ttyEnabled: true , activeDeadlineSeconds: '600')
    ],
    volumes: [
        hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
    ],
    {
node('jenkins_slave') {
properties([
  buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')),
  disableConcurrentBuilds(),
  parameters([
    string(defaultValue: 'none', description: 'Name of Chart-Name to use', name: 'CHART_NAME', trim: false),
    string(defaultValue: 'none', description: 'Name of Manifest to Build', name: 'SERVICE_NAME', trim: false),
    string(defaultValue: 'none', description: 'Which Namespace', name: 'namespace', trim: false),
    choice(choices: ['nonprod', 'prod'], description: 'Select the Environment name: prod/nonprod', name: 'ENV'),
    ])
  ])
  stage('Debug') {
    sh('whoami && pwd')
  }
  stage('Checkout Code') {
    checkout([$class: 'GitSCM', branches: [[name: "*/${git_app_branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'LocalBranch', localBranch: "${git_app_branch}"]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'test', url: "${git_app_repo}"]]])
    notifySlack(buildstatus, slackChannel)
  }
  stage('Are we building?') {
      stage('Setup Gitconfig') {
        sh("git remote set-url origin ${git_app_repo}")
        sh("git config user.email ironman.original@example.com")
        sh("git config user.name 'Iron Man'")
      }
      stage('Run MakeInstall') {
        ansiColor('xterm') {
          withCredentials([usernamePassword(credentialsId: 'bit_helm', passwordVariable: 'bitbucket_pass', usernameVariable: 'bitbucket_user'), sshUserPrivateKey(credentialsId: "test", keyFileVariable: 'private_key')]) {
            sh "ssh-agent && eval `ssh-agent -s` && ssh-add ${private_key} && make SERVICE_NAME=${SERVICE_NAME} CHART_NAME=${CHART_NAME} namespace=${namespace} ENV=${ENV} install"
          }
        }
      }
      if (ENV == 'prod') {
        stage('Run MakeProduction') {
          withCredentials([usernamePassword(credentialsId: "argosecret", passwordVariable: 'argopassword', usernameVariable: 'argouser')]) {
            sh "make SERVICE_NAME=${SERVICE_NAME} CHART_NAME=${CHART_NAME} namespace=${namespace} ENV=${ENV} production"
          }
        }
      } else {
        stage('Run MakeNonProduction') {
          withCredentials([usernamePassword(credentialsId: "argosecret", passwordVariable: 'argopassword', usernameVariable: 'argouser')]) {
            sh "make SERVICE_NAME=${SERVICE_NAME} CHART_NAME=${CHART_NAME} namespace=${namespace} ENV=${ENV} nonproduction"
          }
        }
      }
  }
}
}
)
}
