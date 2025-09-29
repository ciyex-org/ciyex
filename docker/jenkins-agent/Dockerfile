# Custom Jenkins inbound agent image with Azure CLI, kubectl and Docker
# Based on the official Jenkins inbound agent image
FROM jenkins/inbound-agent:latest

USER root

# Install prereqs, Azure CLI, kubectl and docker
RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates curl apt-transport-https gnupg lsb-release \
  && curl -sL https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor > /usr/share/keyrings/microsoft.gpg \
  && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/microsoft.gpg] https://packages.microsoft.com/repos/azure-cli/ $(lsb_release -cs) main" > /etc/apt/sources.list.d/azure-cli.list \
  && apt-get update \
  && apt-get install -y --no-install-recommends azure-cli \
  && curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" \
  && install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl \
  && rm kubectl \
  && apt-get install -y --no-install-recommends docker.io \
  && rm -rf /var/lib/apt/lists/*

# Ensure jenkins user owns its home
RUN chown -R jenkins:jenkins /home/jenkins

USER jenkins

# Default entrypoint/command inherited from base image

LABEL maintainer="your-team@example.com"
