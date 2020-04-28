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
            stage('Resolve Dependencies') {
		module_name = sh(script: 'jq --raw-output .dependencies[0].name metadata.json', returnStdout: true).trim()
                moduleURL = sh(script: 'jq --raw-output .dependencies[0].url metadata.json', returnStdout: true).trim()
                submodulePath = sh(script: 'jq --raw-output .dependencies[0].modulepath metadata.json', returnStdout: true).trim()
                moduleVersion = sh(script: 'jq --raw-output .dependencies[0].version_requirement metadata.json', returnStdout: true).trim()
                if (moduleURL != null) {
                    stage('Download Dependencies') {
                        sh("git clone ${moduleURL}")
                        sh("cd ${module_name} && git checkout tags/v${moduleVersion} && cd -")
                        sh("cp -r ${module_name}/${submodulePath}/* ${env.PWD}")
			sh("printenv | sort")
                    }
                }
            }
            stage('Are we building?') {
                sh 'git log -1 --pretty=%B > git_message'
                if (!readFile('git_message').startsWith('[blacksmith]')) {
                    stage('Setup Gitconfig') {
                        tokens = "${env.JOB_NAME}".tokenize('/')
                        org = tokens[0]
                        repo = tokens[1]
                        branch = tokens[2]
                        sh("git remote set-url origin git@github.com:sahilss429/${repo}.git")
                        sh("git config user.email sahilss429@gmail.com")
                        sh("git config user.name 'sahilss429'")
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
