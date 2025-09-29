# EHR Audit Logging System for ONC Certification

## Overview

This comprehensive audit logging system has been implemented to meet the requirements for ONC Health IT Certification, specifically:

- **ONC § 170.315(d)(1)** - Authentication, access control, and authorization
- **ONC § 170.315(d)(2)** - Auditable events and tamper-resistance
- **ONC § 170.315(d)(3)** - Audit report(s)
- **ONC § 170.315(d)(9)** - Trusted connection

The system also supports **HIPAA Security Rule § 164.312(b)** audit controls for comprehensive healthcare data protection.

## Key Features

### 🔐 Comprehensive Audit Coverage
- **Authentication Events**: Login, logout, failed authentication attempts
- **Patient Data Access**: All PHI access, modifications, and viewing
- **Clinical Operations**: Lab orders, medications, immunizations, vitals
- **Administrative Actions**: System configuration, user management, privilege changes
- **Security Events**: Break-glass access, bulk data exports, suspicious activities

### 🛡️ Security Monitoring
- **Real-time Alerting**: Automatic detection of security threats
- **Risk Classification**: Four-level risk assessment (LOW, MEDIUM, HIGH, CRITICAL)
- **Failed Login Detection**: Configurable thresholds and alerting
- **Suspicious Pattern Detection**: Unusual access patterns, bulk operations

### 📊 Compliance Reporting
- **ONC Compliance Reports**: Pre-built reports for certification requirements
- **HIPAA Reports**: Patient data access summaries and breach investigation
- **Statistical Analysis**: Trend analysis, user activity patterns
- **Audit Trail Integrity**: Tamper-resistant logging with digital signatures

## Architecture Components

### Core Classes

#### 1. AuditLog Entity
- Comprehensive audit event storage
- 20+ fields covering all ONC requirements
- Optimized indexes for performance
- Support for multi-tenant environments

#### 2. AuditLogService
- Central service for all audit operations
- Specialized methods for different event types
- Automatic risk assessment and classification
- Patient-specific audit logging

#### 3. AuditLogAspect (AOP)
- Automatic interception of all controller and service methods
- Transparent audit logging without code changes
- Exception handling and failed operation logging
- Configurable pointcuts for targeted auditing

#### 4. AuditLogController
- RESTful API for audit report generation
- Role-based access control (ADMIN, AUDITOR)
- Comprehensive query capabilities
- Export functionality for compliance reports

#### 5. AuditEventListener
- Real-time security monitoring
- Automatic threat detection
- Configurable alerting system
- Pattern analysis for anomaly detection

### Supporting Components

#### Enums
- **AuditRiskLevel**: LOW, MEDIUM, HIGH, CRITICAL
- **DataClassification**: PUBLIC, INTERNAL, CONFIDENTIAL, PHI, SENSITIVE_PHI

#### Annotations
- **@AuditableOperation**: Fine-grained audit control
- **@PatientDataAccess**: Patient-specific audit marking

#### Configuration
- **AuditConfiguration**: Comprehensive system configuration
- **application.yml**: Production-ready settings

## Database Schema

### audit_log Table
```sql
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_time TIMESTAMP NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    session_id VARCHAR(255),
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    patient_id BIGINT,
    description VARCHAR(500) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    endpoint VARCHAR(500),
    http_method VARCHAR(10),
    response_status INTEGER,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message VARCHAR(1000),
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    compliance_critical BOOLEAN NOT NULL DEFAULT FALSE,
    organization_id BIGINT,
    data_classification VARCHAR(20),
    consent_reference VARCHAR(100)
);
```

### Performance Indexes
- User-based queries: `idx_audit_user_id`, `idx_audit_user_date`
- Patient data access: `idx_audit_patient_id`, `idx_audit_patient_date`
- Security monitoring: `idx_audit_ip_address`, `idx_audit_session_id`
- Compliance queries: `idx_audit_compliance_critical`, `idx_audit_risk_level`

## API Endpoints

### Basic Audit Retrieval
```
GET /api/audit                     # Get paginated audit logs
GET /api/audit/date-range          # Get logs by date range
GET /api/audit/user/{userId}       # Get logs for specific user
GET /api/audit/patient/{patientId} # Get patient access logs
```

### Security Monitoring
```
GET /api/audit/failed-operations   # Get all failed operations
GET /api/audit/high-risk-events    # Get high-risk security events
GET /api/audit/failed-logins       # Get failed login attempts
GET /api/audit/ip-address/{ip}     # Get logs by IP address
```

### Compliance Reporting
```
GET /api/audit/compliance-report   # ONC compliance report
GET /api/audit/hipaa-report        # HIPAA compliance report
GET /api/audit/phi-access          # PHI access events
GET /api/audit/statistics/*        # Various statistical reports
```

## Configuration

### application.yml
```yaml
ciyex:
  audit:
    enabled: true
    log-successful-operations: true
    log-failed-operations: true
    retention-period-days: 2555  # 7 years for healthcare
    enable-alerts: true
    failed-login-alert-threshold: 5
    enable-compliance-monitoring: true
    enable-tamper-detection: true
```

### Key Configuration Options
- **Retention Period**: 7 years default for healthcare compliance
- **Alert Thresholds**: Configurable for failed logins, bulk operations
- **Excluded Endpoints**: Health checks, metrics excluded from auditing
- **Risk Assessment**: Automatic risk level assignment
- **Real-time Monitoring**: Configurable alert system

## Usage Examples

### Manual Audit Logging
```java
@Autowired
private AuditLogService auditLogService;

// Log patient data access
auditLogService.logPatientAccess(patientId, patientName, userId, userRole);

// Log medication prescription
auditLogService.logMedicationPrescription(medicationId, "Aspirin", 
    patientId, patientName, userId, userRole);

// Log break-glass access
auditLogService.logBreakGlassAccess(patientId, patientName, 
    "Emergency cardiac event", userId, userRole);
```

### Automatic Audit Logging
```java
@RestController
public class PatientController {
    
    @GetMapping("/patients/{id}")
    @PatientDataAccess(accessType = "VIEW", description = "Patient record view")
    public Patient getPatient(@PathVariable Long id) {
        // Automatically audited by AOP aspect
        return patientService.findById(id);
    }
}
```

### Audit Report Generation
```java
// Generate ONC compliance report
ResponseEntity<Map<String, Object>> report = auditController
    .generateComplianceReport(startDate, endDate);

// Get patient access summary
List<Object[]> accessSummary = auditController
    .getPatientAccessSummary(patientId, startDate, endDate);
```

## Security Features

### Tamper Resistance
- **Immutable Logs**: Audit logs cannot be modified after creation
- **Digital Signatures**: Optional cryptographic integrity verification
- **Access Control**: Strict role-based access to audit functions
- **Audit the Auditor**: All audit log access is itself audited

### Real-time Monitoring
- **Failed Login Detection**: Automatic threshold-based alerting
- **Bulk Operation Monitoring**: Detection of unusual data export patterns
- **IP Address Tracking**: Geographic and behavioral analysis
- **Session Monitoring**: Concurrent session detection

### Privacy Protection
- **Data Classification**: Automatic PHI classification and handling
- **Consent Tracking**: Integration with patient consent management
- **Anonymization**: Optional user information anonymization
- **Retention Management**: Automatic archival and deletion

## Compliance Benefits

### ONC Certification Support
✅ **Authentication Auditing** - Complete login/logout tracking  
✅ **Access Control Logging** - All authorization events captured  
✅ **Audit Reports** - Comprehensive reporting capabilities  
✅ **Tamper Resistance** - Immutable audit trail with integrity checks  
✅ **Trusted Connections** - IP address and session tracking  

### HIPAA Compliance Support
✅ **PHI Access Logging** - All patient data access tracked  
✅ **Minimum Necessary** - Access pattern analysis  
✅ **Breach Detection** - Automatic anomaly detection  
✅ **Business Associate** - Multi-tenant audit isolation  
✅ **Administrative Safeguards** - User activity monitoring  

## Performance Considerations

### Database Optimization
- **Strategic Indexing**: 15+ indexes for common query patterns
- **Partitioning Ready**: Large table support with date-based partitioning
- **Archival Strategy**: Automatic old log archival and compression
- **Query Optimization**: Efficient pagination and filtering

### Scalability Features
- **Asynchronous Logging**: Optional async processing for high-volume systems
- **Batch Processing**: Configurable batch sizes for bulk operations
- **Connection Pooling**: Optimized database connection management
- **Caching Support**: Strategic caching for frequently accessed data

## Monitoring and Alerting

### Built-in Alerts
- **Failed Authentication**: Multiple failed login attempts
- **Break-glass Access**: Emergency patient data access
- **Bulk Operations**: Large data exports or unusual patterns
- **Configuration Changes**: System security setting modifications
- **High-risk Events**: Automatically classified security events

### Integration Points
- **Email Notifications**: Configurable alert destinations
- **SIEM Integration**: Structured log format for security tools
- **Dashboard Integration**: REST API for real-time monitoring
- **Reporting Tools**: Export capabilities for BI systems

## Deployment Notes

### Database Migration
1. Enable Flyway in application.yml
2. Run application - tables will be created automatically
3. Verify indexes and constraints are properly applied
4. Configure retention and archival policies

### Configuration Checklist
- [ ] Set appropriate retention period
- [ ] Configure alert thresholds
- [ ] Set up notification endpoints
- [ ] Test audit log generation
- [ ] Verify compliance reports
- [ ] Enable security monitoring

### Production Recommendations
- Monitor audit log growth and implement archival
- Set up automated compliance reporting
- Configure security alert notifications
- Implement log backup and disaster recovery
- Regular audit of audit system access
- Performance monitoring of audit queries

## Support and Maintenance

### Log Retention Management
```sql
-- Archive old audit logs (example)
CREATE TABLE audit_log_archive AS 
SELECT * FROM audit_log 
WHERE event_time < NOW() - INTERVAL '7 years';

-- Clean up after archival
DELETE FROM audit_log 
WHERE event_time < NOW() - INTERVAL '7 years';
```

### Performance Monitoring
```sql
-- Monitor audit log growth
SELECT DATE(event_time) as log_date, COUNT(*) as events_count
FROM audit_log 
WHERE event_time > NOW() - INTERVAL '30 days'
GROUP BY DATE(event_time)
ORDER BY log_date;

-- Monitor high-risk events
SELECT COUNT(*) as high_risk_events
FROM audit_log 
WHERE risk_level IN ('HIGH', 'CRITICAL')
AND event_time > NOW() - INTERVAL '24 hours';
```

This audit logging system provides comprehensive ONC certification support while maintaining high performance and scalability for enterprise EHR deployments.