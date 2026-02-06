pipeline {
  agent any

  environment {
    IMAGE_NAME = 'ciyex-app-stage'
    ACR_NAME = 'hinikubestageacr.azurecr.io'
    CLUSTER_NAME = 'hiniKubeStage'
    RESOURCE_GROUP = 'hiniKubeStage-rg'
    VERSION = "v1.0.${env.BUILD_NUMBER}"
  }

  stages {
    stage('Select Credentials') {
      steps {
        script {
          // Determine branch name in multibranch pipeline.
          // If this build is a pull request Jenkins exposes the target branch in CHANGE_TARGET.
          // Use the PR target branch (if present) so credential selection follows where the PR will be merged.
          def branch = env.CHANGE_TARGET ?: env.BRANCH_NAME ?: (env.GIT_BRANCH ? env.GIT_BRANCH.replaceAll('refs/heads/', '') : 'main')
          echo "Detected branch: ${branch}"

          if (branch == 'main') {
            // main -> staging
            env.AZURE_CLIENT_ID_CRED = 'AZURE_CLIENT_ID_STAGE'
            env.AZURE_CLIENT_SECRET_CRED = 'AZURE_CLIENT_SECRET_STAGE'
            env.AZURE_TENANT_ID_CRED = 'AZURE_TENANT_ID_STAGE'
            env.AZURE_SUBSCRIPTION_ID_CRED = 'AZURE_SUBSCRIPTION_ID_STAGE'
            env.ACR_CREDENTIALS_ID = 'ACR_CREDENTIALS_STAGE'
            env.TARGET_ENV = 'stage'
          } else if (branch == 'release' || branch.startsWith('release/')) {
            // release branches -> production
            env.AZURE_CLIENT_ID_CRED = 'AZURE_CLIENT_ID_PROD'
            env.AZURE_CLIENT_SECRET_CRED = 'AZURE_CLIENT_SECRET_PROD'
            env.AZURE_TENANT_ID_CRED = 'AZURE_TENANT_ID_PROD'
            env.AZURE_SUBSCRIPTION_ID_CRED = 'AZURE_SUBSCRIPTION_ID_PROD'
            env.ACR_CREDENTIALS_ID = 'ACR_CREDENTIALS_PROD'
            env.TARGET_ENV = 'prod'
          } else {
            // default: use staging credentials for feature branches
            env.AZURE_CLIENT_ID_CRED = 'AZURE_CLIENT_ID_STAGE'
            env.AZURE_CLIENT_SECRET_CRED = 'AZURE_CLIENT_SECRET_STAGE'
            env.AZURE_TENANT_ID_CRED = 'AZURE_TENANT_ID_STAGE'
            env.AZURE_SUBSCRIPTION_ID_CRED = 'AZURE_SUBSCRIPTION_ID_STAGE'
            env.ACR_CREDENTIALS_ID = 'ACR_CREDENTIALS_STAGE'
            env.TARGET_ENV = 'stage'
          }

          echo "Using credentials set: ${env.AZURE_CLIENT_ID_CRED} / ${env.ACR_CREDENTIALS_ID} for ${env.TARGET_ENV}"
          // Record whether this build is a Pull Request and the effective target branch
          env.IS_PR = env.CHANGE_ID ? 'true' : 'false'
          env.TARGET_BRANCH = branch
          echo "IS_PR=${env.IS_PR} TARGET_BRANCH=${env.TARGET_BRANCH}"
        }
      }
    }
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('PR Build & Test') {
      when {
        expression {
          // Run this stage only for pull requests targeting main or release/*
          return (env.IS_PR == 'true') && (env.TARGET_BRANCH == 'main' || env.TARGET_BRANCH.startsWith('release/'))
        }
      }
      steps {
        echo "Pull Request detected targeting ${env.TARGET_BRANCH}. Running build and tests only."
        // Run the project's build and tests. Adjust gradle task if you want only tests or a different task.
  sh '''#!/usr/bin/env bash
set -euo pipefail
./gradlew clean build
'''
      }
    }

   // Azure login stage — robust fallback: local az -> kubernetes pod az -> helpful error
stage('Azure Login') {
  steps {
    script {
      try {
        // 1) If az exists on the agent, use it
        if (sh(script: 'command -v az >/dev/null 2>&1', returnStatus: true) == 0) {
          echo "Using local az on the agent"
          withCredentials([
            string(credentialsId: 'AZURE_CLIENT_ID_STAGE', variable: 'AZURE_CLIENT_ID'),
            string(credentialsId: 'AZURE_CLIENT_SECRET_STAGE', variable: 'AZURE_CLIENT_SECRET'),
            string(credentialsId: 'AZURE_TENANT_ID_STAGE', variable: 'AZURE_TENANT_ID'),
            string(credentialsId: 'AZURE_SUBSCRIPTION_ID_STAGE', variable: 'AZURE_SUBSCRIPTION_ID')
          ]) {
            sh '''
              az --version
              az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_CLIENT_SECRET" --tenant "$AZURE_TENANT_ID"
              az account set --subscription "$AZURE_SUBSCRIPTION_ID"
            '''
          }
        } else {
          // 2) Fallback: try to run az inside an ephemeral Kubernetes pod (Jenkins Kubernetes plugin)
          echo "'az' not found on agent — attempting containerized az in a Kubernetes pod"
          def podYaml = """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: azure
    image: mcr.microsoft.com/azure-cli:2.49.0
    command:
      - cat
    tty: true
"""
          // Use a fixed label so we can target the node created by podTemplate
          def podLabel = "azure-agent-${env.BUILD_ID}"
          podTemplate(label: podLabel, yaml: podYaml) {
            node(podLabel) {
              container('azure') {
                withCredentials([
                  string(credentialsId: 'AZURE_CLIENT_ID_STAGE', variable: 'AZURE_CLIENT_ID'),
                  string(credentialsId: 'AZURE_CLIENT_SECRET_STAGE', variable: 'AZURE_CLIENT_SECRET'),
                  string(credentialsId: 'AZURE_TENANT_ID_STAGE', variable: 'AZURE_TENANT_ID'),
                  string(credentialsId: 'AZURE_SUBSCRIPTION_ID_STAGE', variable: 'AZURE_SUBSCRIPTION_ID')
                ]) {
                  sh '''
                    echo "Running az inside Kubernetes pod"
                    az --version
                    az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_CLIENT_SECRET" --tenant "$AZURE_TENANT_ID"
                    az account set --subscription "$AZURE_SUBSCRIPTION_ID"
                  '''
                }
              }
            }
          }
        }
      } catch (err) {
        // 3) Clear actionable error message
        echo "ERROR: Azure login step failed: ${err}"
        echo '''
Possible fixes:
  - Use an agent image that has the Azure CLI installed (recommended).
  - Install Docker on the agent if you intend to run containerized az via docker.
  - Configure Jenkins with the Kubernetes plugin and a cloud so podTemplate can create pods (used by fallback).
  - Or perform az login outside the pipeline and provide kubeconfig/service principal to the job.
'''
        error("Azure login could not be performed. See the message above for remediation steps.")
      }
    } // end script
  } // end steps
} // end stage

    stage('Build Docker Image') {
      when {
        expression { return env.IS_PR != 'true' }
      }
      steps {
  sh '''#!/usr/bin/env bash
set -euo pipefail
echo "Building Docker image: ${ACR_NAME}/${IMAGE_NAME}:${VERSION}"
docker build --build-arg ENVIRONMENT=stage -t ${ACR_NAME}/${IMAGE_NAME}:${VERSION} .
'''
      }
    }

    stage('Push to ACR') {
      when {
        expression { return env.IS_PR != 'true' }
      }
      steps {
        // Use the ACR credential ID selected earlier (branch-specific)
        withCredentials([usernamePassword(credentialsId: env.ACR_CREDENTIALS_ID, usernameVariable: 'ACR_USERNAME', passwordVariable: 'ACR_PASSWORD')]) {
          sh '''#!/usr/bin/env bash
# Derive short registry name (before first dot)
ACR_REGISTRY="$(echo ${ACR_NAME} | cut -d'.' -f1)"
echo "Trying az acr login for registry: ${ACR_REGISTRY}"

# Use same az wrapper as in Azure Login: prefer local az, otherwise run az in container with mounts
AZ_CLI_CMD="az"
if ! command -v az >/dev/null 2>&1; then
  echo "'az' not found on agent; will use containerized Azure CLI for ACR login"
  mkdir -p "$HOME/.azure" "$HOME/.kube"
  if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: 'az' is not installed and 'docker' is not available to run a containerized Azure CLI."
    echo "You can either use an agent image that contains 'az' or install docker on the agent."
    exit 127
  fi
  AZ_CLI_CMD="docker run --rm -v $HOME/.azure:/root/.azure -v $HOME/.kube:/root/.kube -e AZURE_CLIENT_ID -e AZURE_CLIENT_SECRET -e AZURE_TENANT_ID -e AZURE_SUBSCRIPTION_ID mcr.microsoft.com/azure-cli az"
fi

if ${AZ_CLI_CMD} acr login --name "${ACR_REGISTRY}" 2>/dev/null; then
  echo "az acr login succeeded"
else
  echo "az acr login failed or not available, falling back to docker login"
  docker login ${ACR_NAME} -u "$ACR_USERNAME" -p "$ACR_PASSWORD"
fi

echo "Pushing image to ACR"
docker push ${ACR_NAME}/${IMAGE_NAME}:${VERSION}
'''
        }
      }
    }

    stage('Set AKS context') {
      when {
        expression { return env.IS_PR != 'true' }
      }
      steps {
  sh '''#!/usr/bin/env bash
set -euo pipefail
echo "Getting AKS credentials for cluster ${CLUSTER_NAME} in ${RESOURCE_GROUP}"

# Prefer local az; if not available, run the Azure CLI container and mount kube and azure dirs so credentials persist
AZ_CLI_CMD="az"
if ! command -v az >/dev/null 2>&1; then
  echo "'az' not found on agent; will use containerized Azure CLI to fetch AKS credentials"
  mkdir -p "$HOME/.azure" "$HOME/.kube"
  if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: 'az' is not installed on this agent and 'docker' is not available to run containerized Azure CLI."
    echo "Please use an agent image that has the Azure CLI installed or install Docker on the agent nodes."
    exit 127
  fi
  AZ_CLI_CMD="docker run --rm -v $HOME/.azure:/root/.azure -v $HOME/.kube:/root/.kube -e AZURE_CLIENT_ID -e AZURE_CLIENT_SECRET -e AZURE_TENANT_ID -e AZURE_SUBSCRIPTION_ID mcr.microsoft.com/azure-cli az"
fi

${AZ_CLI_CMD} aks get-credentials --resource-group "${RESOURCE_GROUP}" --name "${CLUSTER_NAME}" --overwrite-existing
'''
      }
    }

    stage('Update manifests') {
      when {
        expression { return env.IS_PR != 'true' }
      }
      steps {
  sh '''#!/usr/bin/env bash
set -euo pipefail
echo "Updating image tag in manifests/stage/ciyex-deployment-stage.yaml"
sed -i "s|IMAGE_URL:IMAGE_TAG|${ACR_NAME}/${IMAGE_NAME}:${VERSION}|g" manifests/stage/ciyex-deployment-stage.yaml
echo "Updated manifest content:" && grep -n "image:" manifests/stage/ciyex-deployment-stage.yaml || true
'''
      }
    }

    stage('Deploy to AKS') {
      when {
        expression { return env.IS_PR != 'true' }
      }
      steps {
  sh '''#!/usr/bin/env bash
set -euo pipefail
echo "Applying Kubernetes manifests in manifests/stage/"
kubectl apply -f manifests/stage/
'''
      }
    }
  }

  post {
    success {
      script {
        if (env.IS_PR != 'true') {
          sendTeamsNotification('success')
        }
      }
      echo "Pipeline succeeded: ${ACR_NAME}/${IMAGE_NAME}:${VERSION}"
    }
    failure {
      script {
        if (env.IS_PR != 'true') {
          sendTeamsNotification('failure')
        }
      }
      echo "Pipeline failed"
    }
  }
}

def sendTeamsNotification(String status) {
  def color = status == 'success' ? '00FF00' : 'FF0000'
  def emoji = status == 'success' ? '✅' : '❌'
  def statusText = status == 'success' ? 'Deployment Successful' : 'Deployment Failed'
  
  def payload = """
  {
    "type": "message",
    "attachments": [{
      "contentType": "application/vnd.microsoft.card.adaptive",
      "content": {
        "type": "AdaptiveCard",
        "version": "1.4",
        "body": [
          {"type": "TextBlock", "size": "Large", "weight": "Bolder", "text": "${emoji} Ciyex - ${env.TARGET_ENV}"},
          {"type": "FactSet", "facts": [
            {"title": "Version", "value": "${env.VERSION}"},
            {"title": "Status", "value": "${statusText}"},
            {"title": "Branch", "value": "${env.TARGET_BRANCH}"},
            {"title": "Build", "value": "#${env.BUILD_NUMBER}"}
          ]},
          {"type": "TextBlock", "text": "[View Build](${env.BUILD_URL})", "wrap": true}
        ]
      }
    }]
  }
  """
  
  withCredentials([string(credentialsId: 'TEAMS_WEBHOOK_URL', variable: 'WEBHOOK_URL')]) {
    sh """curl -H 'Content-Type: application/json' -d '${payload}' \"\${WEBHOOK_URL}\""""
  }
}
}
