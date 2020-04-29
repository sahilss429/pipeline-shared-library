def call(String masterBuild) {

properties([
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

node('dood') {
    stage('variables') {
        echo "Terraform Version = $TERRAFORM_VERSION"
        echo "Ruby Version = $RUBY_VERSION"
        echo "Container ID = $DOCKER_CONTAINER_ID"
        echo "WORKSPACE = $WORKSPACE"
        tokens = "${env.JOB_NAME}".tokenize('/')
        repo = tokens[0]
        env = tokens[1]
        service = tokens[3]
        echo "$repo/$env/apps/$service/"
    }

    stage('Checkout Code') {
        checkout scm
    }
    dir("$env/apps/$service/"){
        sh('pwd')
        stage('Resolve Dependencies') {
            sh('ls -l')
            module_name = sh(script: 'jq --raw-output .dependencies[0].name metadata.json', returnStdout: true).trim()
            moduleURL = sh(script: 'jq --raw-output .dependencies[0].url metadata.json', returnStdout: true).trim()
            submodulePath = sh(script: 'jq --raw-output .dependencies[0].modulepath metadata.json', returnStdout: true).trim()
            moduleVersion = sh(script: 'jq --raw-output .dependencies[0].version_requirement metadata.json', returnStdout: true).trim()
            if (moduleURL != null) {
                stage('Download Dependencies') {
                    sh("printenv | sort")
                    workdir = "$WORKSPACE"
                    git branch: '*/master'
                        url: "${moduleURL}"
                    sh("ls -l")
                }
            }
        }
        stage('Are we building?') {
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
        }
    }
}

}
