# Remaining Compilation Fixes Needed

## Summary: 15 errors remaining (down from 80!)

### Quick Fixes Needed:

1. **PatientBillingController.java** - `listAllClaims()` needs patientId parameter
2. **PortalReportsController.java** - Remove orgId from method calls
3. **TemplateDocumentController.java** - Remove orgId parameter from getAll() calls
4. **PatientBillingService.java** - Fix PatientBillingNoteDto::from method reference
5. **PortalVitalsController.java** - Service methods expect Long but receiving String
6. **PortalBillingController.java** - `userId` variable undefined
7. **MessageAttachmentService.java** - `findByIdAndOrgId()` should be `findById()`
8. **PortalCommunicationController.java** - Remove orgId parameters and setOrgId() call

### Files Status:
✅ MedicationsController - FIXED
✅ CoverageService - FIXED  
✅ MedicationsService - FIXED
✅ LabOrderController - FIXED
✅ DocumentService - FIXED
✅ PatientBillingService - 98% FIXED (1 minor error)

⚠️ Need fixing: Portal controllers, MessageAttachmentService, TemplateDocumentController
