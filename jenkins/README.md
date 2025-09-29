Jenkins pipeline for Ciyex
=========================

This folder documents how to run the repository CI/CD using Jenkins. A root-level `Jenkinsfile` is provided to build the Docker image, push it to Azure Container Registry (ACR), update Kubernetes manifests, and deploy to AKS.

Credentials and Jenkins setup
-----------------------------

Create the following credentials in Jenkins (Credentials > System > Global credentials). The Jenkinsfile selects credentials based on branch:

Branch mapping:

- `main` &rarr; staging credentials (used for deploy-to-stage)
- `release` or branches starting with `release/` &rarr; production credentials (used for deploy-to-prod)
- other branches &rarr; staging credentials (default)

Credential IDs to create (exact names expected by the `Jenkinsfile`):

- Staging (for `main` and feature branches):
	- `AZURE_CLIENT_ID_STAGE` (Secret text) - Service Principal App ID for staging
	- `AZURE_CLIENT_SECRET_STAGE` (Secret text) - Service Principal password for staging
	- `AZURE_TENANT_ID_STAGE` (Secret text) - Azure tenant id for staging
	- `AZURE_SUBSCRIPTION_ID_STAGE` (Secret text) - Azure subscription id for staging
	- `ACR_CREDENTIALS_STAGE` (Username with password) - ACR username/password for staging

- Production (for `release` branches):
	- `AZURE_CLIENT_ID_PROD` (Secret text) - Service Principal App ID for production
	- `AZURE_CLIENT_SECRET_PROD` (Secret text) - Service Principal password for production
	- `AZURE_TENANT_ID_PROD` (Secret text) - Azure tenant id for production
	- `AZURE_SUBSCRIPTION_ID_PROD` (Secret text) - Azure subscription id for production
	- `ACR_CREDENTIALS_PROD` (Username with password) - ACR username/password for production

If you prefer different names, update the `Select Credentials` stage in the `Jenkinsfile` to match your Jenkins credential IDs.

Recommended Jenkins agents
-------------------------

Use an agent with the following installed:

- Docker CLI
- Azure CLI (`az`) with `aks` extension
- kubectl

How to create the pipeline
--------------------------

1. Create a new Pipeline job in Jenkins.
2. Point the job to this repository (Git). Configure credentials as needed.
3. Use the Jenkinsfile from the repository root (default).
4. Ensure the job has access to the credentials listed above.

Notes and migration guidance
---------------------------

- The repository still contains a GitHub Actions workflow at `.github/workflows/build-and-deploy-stage.yml`. If you fully migrate to Jenkins, consider disabling or removing that workflow to avoid duplicate deployments.
- The `Jenkinsfile` uses `sed` to perform a simple string replacement in `manifests/stage/ciyex-deployment-stage.yaml`. Confirm the placeholder `IMAGE_URL:IMAGE_TAG` exists in the manifest.
