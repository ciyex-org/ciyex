Deploy the Jenkins agent image to AKS (or any Kubernetes cluster)

Prerequisites
- `kubectl` configured to talk to your target cluster
- The image pushed to a registry accessible from the cluster (e.g., ACR)
- If using ACR and the cluster is in a different subscription, create an imagePullSecret or enable managed identity-based pull

Steps

1. Build and push the image (from repo root):

```bash
ACR_NAME=hinikubestageacr.azurecr.io TAG=v1.0.0 ./docker/jenkins-agent/build-and-push.sh
```

2. Update the manifest

Edit `manifests/jenkins-agent-deployment.yaml` and replace `<registry>/ciyex-jenkins-agent:<tag>` with your image name, e.g. `hinikubestageacr.azurecr.io/ciyex-jenkins-agent:v1.0.0`.

If your cluster needs imagePullSecrets, create one:

```bash
kubectl create secret docker-registry acr-pull-secret --docker-server=hinikubestageacr.azurecr.io --docker-username=<username> --docker-password=<password> --docker-email=<email>
```

Then reference the secret in the manifest under `imagePullSecrets`.

3. Deploy to Kubernetes

```bash
kubectl apply -f manifests/jenkins-agent-deployment.yaml
```

4. Verify

```bash
kubectl get pods -l app=ciyex-jenkins-agent
kubectl logs deploy/ciyex-jenkins-agent
```

Notes
- The deployment mounts the host Docker socket (`/var/run/docker.sock`). If you are running in AKS with managed nodes, ensure the nodepool allows this; otherwise consider using a remote builder (BuildKit, ACR Tasks) or running Docker-in-Docker with appropriate security configuration.
- You may want to add a readinessProbe/livenessProbe depending on how the agent process is started in your image.
