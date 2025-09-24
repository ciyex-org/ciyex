Custom Jenkins Agent for ciyex

This Docker image provides a Jenkins inbound agent with the following tools preinstalled:
```yaml
# Pod template example for Kubernetes plugin
apiVersion: v1
kind: Pod
metadata:
  name: jenkins-agent
spec:
  containers:
  - name: jnlp
    image: <registry>/ciyex-jenkins-agent:<tag>
    args:
      - ${computer.jnlpmac}
      - ${computer.name}
    volumeMounts:
      - name: dockersock
        mountPath: /var/run/docker.sock
  volumes:
  - name: dockersock
    hostPath:
      path: /var/run/docker.sock
```

Kubernetes deployment example (run an agent as a Deployment):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ciyex-jenkins-agent
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ciyex-jenkins-agent
  template:
    metadata:
      labels:
        app: ciyex-jenkins-agent
    spec:
      containers:
        - name: jenkins-agent
          image: <registry>/ciyex-jenkins-agent:<tag>
          # adjust args/entrypoint according to your base image and Jenkins setup
          volumeMounts:
            - name: dockersock
              mountPath: /var/run/docker.sock
      volumes:
        - name: dockersock
          hostPath:
            path: /var/run/docker.sock
```

Build-and-push helper script

Use the included `build-and-push.sh` script to build and push the image to ACR. Example:

```bash
ACR_NAME=hinikubestageacr.azurecr.io TAG=v1.0.0 ./docker/jenkins-agent/build-and-push.sh
```

The script prefers `az acr login` when the Azure CLI is available and falls back to `docker login` using `ACR_USERNAME`/`ACR_PASSWORD` environment variables.

- Mounting `/var/run/docker.sock` grants the container root access on the host. Use with caution.
- Prefer using BuildKit or a remote build service instead of mounting the host docker socket when possible.

Alternative: install on the agent VM(s)

If you manage the Jenkins agent VMs, installing `az` and `kubectl` there may be simpler than running a custom image.
