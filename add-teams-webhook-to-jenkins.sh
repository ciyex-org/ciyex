#!/bin/bash
# Add Teams Webhook URL to Jenkins credentials

read -p "Enter Jenkins URL (e.g., http://localhost:8080): " JENKINS_URL
read -p "Enter Jenkins username: " JENKINS_USER
read -sp "Enter Jenkins password/token: " JENKINS_TOKEN
echo
read -p "Enter Teams Webhook URL: " TEAMS_WEBHOOK_URL

# Create credential XML
cat > /tmp/teams-webhook-cred.xml <<EOF
<org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl>
  <scope>GLOBAL</scope>
  <id>TEAMS_WEBHOOK_URL</id>
  <description>Microsoft Teams Webhook URL for deployment notifications</description>
  <secret>${TEAMS_WEBHOOK_URL}</secret>
</org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl>
EOF

# Add to Jenkins
curl -X POST "${JENKINS_URL}/credentials/store/system/domain/_/createCredentials" \
  --user "${JENKINS_USER}:${JENKINS_TOKEN}" \
  --data-urlencode 'json={
    "": "0",
    "credentials": {
      "scope": "GLOBAL",
      "id": "TEAMS_WEBHOOK_URL",
      "secret": "'"${TEAMS_WEBHOOK_URL}"'",
      "description": "Microsoft Teams Webhook URL",
      "$class": "org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl"
    }
  }'

echo -e "\n✅ Teams webhook credential added to Jenkins"
rm /tmp/teams-webhook-cred.xml
