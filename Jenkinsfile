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
        sh '''
          set -euo pipefail
          ./gradlew clean build
        '''
      }
    }

    stage('Azure Login') {
      when {
        expression { return env.IS_PR != 'true' }
      }
      steps {
        // Use branch-specific credential IDs selected earlier
        withCredentials([
          string(credentialsId: env.AZURE_CLIENT_ID_CRED, variable: 'AZURE_CLIENT_ID'),
          string(credentialsId: env.AZURE_CLIENT_SECRET_CRED, variable: 'AZURE_CLIENT_SECRET'),
          string(credentialsId: env.AZURE_TENANT_ID_CRED, variable: 'AZURE_TENANT_ID'),
          string(credentialsId: env.AZURE_SUBSCRIPTION_ID_CRED, variable: 'AZURE_SUBSCRIPTION_ID')
        ]) {
          sh '''
            set -euo pipefail
            echo "Logging into Azure..."
            az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_CLIENT_SECRET" --tenant "$AZURE_TENANT_ID"
            az account set --subscription "$AZURE_SUBSCRIPTION_ID"
          '''
        }
      }
    }

    stage('Build Docker Image') {
      when {
        expression { return env.IS_PR != 'true' }
      }
      steps {
        sh '''
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
          sh '''
            set -euo pipefail
            # Derive short registry name (before first dot)
            ACR_REGISTRY="$(echo ${ACR_NAME} | cut -d'.' -f1)"
            echo "Trying az acr login for registry: ${ACR_REGISTRY}"
            if az acr login --name "${ACR_REGISTRY}" 2>/dev/null; then
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
        sh '''
          set -euo pipefail
          echo "Getting AKS credentials for cluster ${CLUSTER_NAME} in ${RESOURCE_GROUP}"
          az aks get-credentials --resource-group "${RESOURCE_GROUP}" --name "${CLUSTER_NAME}" --overwrite-existing
        '''
      }
    }

    stage('Update manifests') {
      when {
        expression { return env.IS_PR != 'true' }
      }
      steps {
        sh '''
          set -euo pipefail
          echo "Updating image tag in manifests/stage/ciyex-deployment-stage.yaml"
          sed -i "s|IMAGE_URL:IMAGE_TAG|${ACR_NAME}/${IMAGE_NAME}:${VERSION}|g" manifests/stage/ciyex-deployment-stage.yaml
          echo "Updated manifest content:" && grep -n "image:" -n manifests/stage/ciyex-deployment-stage.yaml || true
        '''
      }
    }

    stage('Deploy to AKS') {
      when {
        expression { return env.IS_PR != 'true' }
      }
      steps {
        sh '''
          set -euo pipefail
          echo "Applying Kubernetes manifests in manifests/stage/"
          kubectl apply -f manifests/stage/
        '''
      }
    }
  }

  post {
    success {
      echo "Pipeline succeeded: ${ACR_NAME}/${IMAGE_NAME}:${VERSION}"
    }
    failure {
      echo "Pipeline failed"
    }
  }
}
