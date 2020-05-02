def call(String masterBuild) {

properties([
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

def tokens = "${masterBuild}".tokenize('/')
def team = tokens[0]
def repo = tokens[1]
def BRANCH = tokens[2]
def REPO_URL = "git@github.com:${team}/${repo}.git"
def app = "${env.JOB_NAME}".tokenize(repo"/")

def PATH = app[1]

node('dood') {
    stage('variables') {
        echo "Terraform Version = $TERRAFORM_VERSION"
        echo "Ruby Version = $RUBY_VERSION"
        echo "Container ID = $DOCKER_CONTAINER_ID"
        echo "WORKSPACE = $WORKSPACE"
	echo "Terraform PATH = ${PATH}"
    }

    stage('Checkout Code') {
        checkout scm
    }
    stage('Resolve Dependencies') {
        sh('ls -l')
        sh("cp ${PATH}/dependency.json .")
        sh("cp ${PATH}/vars.tfvars .")
        module_name = sh(script: 'jq --raw-output .dependencies[0].name dependency.json', returnStdout: true).trim()
        moduleURL = sh(script: 'jq --raw-output .dependencies[0].url dependency.json', returnStdout: true).trim()
        submodulePath = sh(script: 'jq --raw-output .dependencies[0].modulepath dependency.json', returnStdout: true).trim()
        moduleVersion = sh(script: 'jq --raw-output .dependencies[0].version_requirement dependency.json', returnStdout: true).trim()
        if (moduleURL != null) {
            stage('Download Dependencies') {
                sh("printenv | sort")
                sh("git clone ${moduleURL}")
                sh("cd ${module_name} && git checkout tags/v${moduleVersion}")
                sh("cp -r ${module_name}/${submodulePath}/* .")
                sh('ls -l')
            }
        }
    }
        stage('Are we building?') {
	   sh 'git log -1 --pretty=%B > git_message'
           if (!readFile('git_message').startsWith('[blacksmith]')) {
		stage('Setup Gitconfig') {
		    sh("git remote set-url origin ${REPO_URL}")
		    sh("git config user.email blacksmith@jenkins.local")
		    sh("git config user.name 'BlackSmith'")
		}
                stage('Validate Terraform') {
                    sh 'terraform init'
                    sh 'terraform validate'
                }
                stage('Terraform Plan') {
		          echo "terraform plan -lock=true -var-file=vars.tf -input=false -out=plan.tf"
                }
                stage('Apply Changes') {
                    input 'Apply Changes?'
                }
                stage('Terraform Apply') {
                    echo "Not running apply this time command could be terraform apply -lock=true -var-file=vars.tf -input=false plan.tf"
                }
                stage('Update Tag') {
                    sh 'rake tag'
                }
                stage('Publish Tag') {
                    sh 'git checkout -b publish_tmp && git checkout -B master publish_tmp && rake publish && git branch -d publish_tmp && git push --tags origin master'
                }
	   }
        }
}

}
