Custom Jenkins Agent for ciyex

This Docker image provides a Jenkins inbound agent with the following tools preinstalled:
- Azure CLI (`az`)
- kubectl
- Docker CLI (`docker`)

Build and push

1. Build the image locally (replace `<tag>`):

```bash
docker build -t <registry>/ciyex-jenkins-agent:<tag> ./docker/jenkins-agent
```

2. Push to your container registry (example for ACR):

```bash
# Login to ACR (replace ACR_NAME and creds)
az acr login --name <ACR_NAME>

docker push <registry>/ciyex-jenkins-agent:<tag>
```

Use in Kubernetes

- If you run Jenkins in Kubernetes with the Kubernetes plugin, configure a pod template that uses this image.
- Ensure the agent has permissions to use Docker (e.g., mount Docker socket) or use remote Docker builds (recommended).

Sample Kubernetes pod template snippet for Jenkins (in Kubernetes plugin UI):

```yaml
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

Security notes

- Mounting `/var/run/docker.sock` grants the container root access on the host. Use with caution.
- Prefer using BuildKit or a remote build service instead of mounting the host docker socket when possible.

Alternative: install on the agent VM(s)

If you manage the Jenkins agent VMs, installing `az` and `kubectl` there may be simpler than running a custom image.
