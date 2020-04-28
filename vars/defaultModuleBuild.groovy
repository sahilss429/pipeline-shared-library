def call(String masterBuild) {

properties([
    buildDiscarder(logRotator(numToKeepStr: '5')), 
    disableConcurrentBuilds()
])

node('dood') {
            stage('Delete workspace'){
                sh 'ls -alh'
                echo "should be boring"
                sh 'rm -rf ..?* .[!.]* *'
                sh 'ls -alh'
                echo "should really be boring now"
            }
            stage('Checkout Code') {
                checkout scm
            }
            stage('Are we building?') {
                sh 'git log -1 --pretty=%B > git_message'
                if (!readFile('git_message').startsWith('[blacksmith]')) {
                    stage('Setup Gitconfig') {
                        tokens = "${env.JOB_NAME}".tokenize('/')
                        org = tokens[0]
                        repo = tokens[1]
                        branch = tokens[2]
                        sh("git remote set-url origin git@github.com:sahilss429/terraform-vpc.git")
                        sh("git config user.email sahilss429@gmail.com")
                        sh("git config user.name 'sahilss429'")
                    }
                    stage('ValidateTF') {
                        sh 'terraform init'
			sh 'terraform validate'
                    }
                    stage('ModuleTag') {
                        sh 'rake tag'
                    }
                     stage('PublishTag') {
                         sh 'git checkout -b publish_tmp && git checkout -B master publish_tmp && rake publish && git branch -d publish_tmp && git push --tags origin master'
                     }
                }
            }
}

}
