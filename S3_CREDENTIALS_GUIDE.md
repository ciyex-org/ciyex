# S3 Credentials Configuration Guide

## Overview
Message attachments are stored in AWS S3 buckets. The S3 credentials for each organization are configured in the `org_config` table in the public schema.

## Storage Location
- **Table**: `public.org_config`
- **Column**: `integrations` (JSONB)
- **Path**: `integrations -> 'document_storage' -> 's3'`

## S3 Configuration Structure
```json
{
  "document_storage": {
    "s3": {
      "bucket": "your-s3-bucket-name",
      "region": "us-east-1",
      "accessKey": "your-aws-access-key",
      "secretKey": "your-aws-secret-key"
    }
  }
}
```

## How to Update S3 Credentials

### ✅ **RECOMMENDED: Programmatic Update via API (Production)**
```bash
# PUT request to update S3 config
curl -X PUT "http://localhost:8080/api/org-configs/{orgId}/s3-config" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {your-jwt-token}" \
  -d '{
    "bucket": "new-bucket-name",
    "region": "us-east-1",
    "accessKey": "new-access-key",
    "secretKey": "new-secret-key"
  }'
```

### Alternative: Direct Database Update (Development/Testing Only)
```sql
-- Update S3 credentials for organization ID 1
UPDATE public.org_config
SET integrations = jsonb_set(
  jsonb_set(integrations, '{document_storage,s3,bucket}', '"new-bucket-name"'),
  '{document_storage,s3,accessKey}', '"new-access-key"'
)
WHERE org_id = 1;

-- Update secret key separately for security
UPDATE public.org_config
SET integrations = jsonb_set(integrations, '{document_storage,s3,secretKey}', '"new-secret-key"')
WHERE org_id = 1;
```

## Required Permissions
The S3 credentials need the following AWS permissions:
- `s3:PutObject` - To upload files
- `s3:GetObject` - To download files
- `s3:DeleteObject` - To delete files
- `s3:ListBucket` - To list bucket contents

## Environment-Specific Configurations
- **Development**: Use test buckets with limited permissions
- **Staging**: Use staging buckets
- **Production**: Use production buckets with proper security

## Security Notes
- Never commit real AWS credentials to version control
- Use IAM roles when possible instead of access keys
- Rotate credentials regularly
- Store secrets securely (consider AWS Secrets Manager for production)

## Troubleshooting
- Check application logs for S3 connection errors
- Verify bucket permissions and region settings
- Ensure the bucket exists and is accessible
- Check network connectivity to S3 endpoints

## Current Organizations with S3 Config
- Org ID 1: hinosoft-org-1 (us-east-1)
- Org ID 2: hinosoft-org-1 (us-east-1)
- Org ID 3: hinosoft-org-1 (us-east-1)