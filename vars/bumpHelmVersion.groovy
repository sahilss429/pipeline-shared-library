def call(String masterBuild) {
def git_app_repo = scm.userRemoteConfigs[0].url
def SERVICE_NAME = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
def tokens = "${masterBuild}".tokenize('/')
def jenkins_slave = tokens[0]
def slackChannel = tokens[1]
def awsAccount = tokens[2]
def git_app_branch = "${env.BRANCH_NAME}"
def buildstatus = "STARTED"
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
node(jenkins_slave) {
properties([
  buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')),
  disableConcurrentBuilds(),
  parameters([
    string(defaultValue: 'none', description: 'Name of chart to bump', name: 'ChartName', trim: false),
    choice(choices: ['major', 'minor', 'patch'], description: 'Select a Bump version type', name: 'VerType'),
    ])
  ])
  stage('CleanWorkspace') {
    sh('whoami && pwd')
  }
  stage('Checkout Code') {
    checkout([$class: 'GitSCM', branches: [[name: "*/${git_app_branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'LocalBranch', localBranch: "${git_app_branch}"]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'test', url: "${git_app_repo}"]]])
    notifySlack(buildstatus, slackChannel)
  }
  stage('Are we building?') {
    sh 'git log -1 --pretty=%B > git_message'
    if (!readFile('git_message').startsWith('[Iron_man]')) {
      stage('Setup Gitconfig') {
        sh("git remote set-url origin ${git_app_repo}")
        sh("git config user.email ironman.original@example.com")
        sh("git config user.name 'Iron Man'")
      }
      stage('Run tests') {
        if ("${params.ChartName}" == "application") {    
	        sh "helm plugin install https://github.com/quintush/helm-unittest || echo 0"
	        sh "cd helm-charts/${params.ChartName}/ && helm unittest ."
        }
      }
      stage('Bump Charts.yaml') {
        ansiColor('xterm') {
          sh "cd helm-charts/${params.ChartName}/ && helm dependency update"
          sh "sudo pybump bump --file helm-charts/${params.ChartName}/Chart.yaml --level ${params.VerType}"
        }
      }
      stage('Helm Package') {
        sh "helm package helm-charts/${params.ChartName} --destination ./charts"
      }
      stage('Helm Index') {
        sh 'helm repo index .'
      }
      stage('PublishTag') {
        sh """git add . && git commit -m "[Iron_man] updated commit hash" && git push origin ${git_app_branch} || echo 'no changes'"""
      }
    }
  }
}
}
}
