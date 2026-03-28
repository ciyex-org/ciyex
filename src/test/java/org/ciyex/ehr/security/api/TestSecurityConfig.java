package org.ciyex.ehr.security.api;

import org.ciyex.ehr.audit.controller.AuditLogController;
import org.ciyex.ehr.audit.service.AuditLogService;
import org.ciyex.ehr.careplan.controller.CarePlanController;
import org.ciyex.ehr.careplan.service.CarePlanService;
import org.ciyex.ehr.cds.controller.CdsAlertController;
import org.ciyex.ehr.cds.service.CdsService;
import org.ciyex.ehr.consent.controller.PatientConsentController;
import org.ciyex.ehr.consent.service.PatientConsentService;
import org.ciyex.ehr.controller.AllClaimsController;
import org.ciyex.ehr.controller.DocumentController;
import org.ciyex.ehr.controller.EncounterSummaryController;
import org.ciyex.ehr.controller.InsuranceCompanyController;
import org.ciyex.ehr.controller.InvoiceController;
import org.ciyex.ehr.controller.OrgConfigController;
import org.ciyex.ehr.controller.PatientBillingController;
import org.ciyex.ehr.controller.UserController;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.fhir.GenericFhirResourceController;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
import org.ciyex.ehr.immunization.controller.ImmunizationController;
import org.ciyex.ehr.immunization.service.ImmunizationService;
import org.ciyex.ehr.lab.controller.LabOrderController;
import org.ciyex.ehr.lab.controller.LabResultController;
import org.ciyex.ehr.lab.service.LabOrderService;
import org.ciyex.ehr.lab.service.LabResultService;
import org.ciyex.ehr.marketplace.controller.AppInstallationController;
import org.ciyex.ehr.marketplace.service.AppInstallationService;
import org.ciyex.ehr.messaging.controller.MessagingController;
import org.ciyex.ehr.messaging.service.MessagingService;
import org.ciyex.ehr.notification.controller.NotificationController;
import org.ciyex.ehr.notification.service.NotificationService;
import org.ciyex.ehr.payment.controller.PaymentController;
import org.ciyex.ehr.payment.service.PaymentService;
import org.ciyex.ehr.prescription.controller.PrescriptionController;
import org.ciyex.ehr.prescription.service.PrescriptionService;
import org.ciyex.ehr.recall.controller.RecallController;
import org.ciyex.ehr.recall.service.RecallService;
import org.ciyex.ehr.referral.controller.ReferralController;
import org.ciyex.ehr.referral.service.ReferralService;
import org.ciyex.ehr.service.DocumentService;
import org.ciyex.ehr.service.EncounterSummaryService;
import org.ciyex.ehr.service.InsuranceCompanyService;
import org.ciyex.ehr.service.InvoiceService;
import org.ciyex.ehr.service.KeycloakUserService;
import org.ciyex.ehr.service.OrgConfigService;
import org.ciyex.ehr.service.PatientBillingNoteService;
import org.ciyex.ehr.service.PatientBillingPrintService;
import org.ciyex.ehr.service.PatientClaimService;
import org.ciyex.ehr.service.PatientCreditService;
import org.ciyex.ehr.service.PatientDepositService;
import org.ciyex.ehr.service.PatientInsurancePaymentService;
import org.ciyex.ehr.service.PatientInvoiceService;
import org.ciyex.ehr.service.PatientPaymentService;
import org.ciyex.ehr.task.controller.ClinicalTaskController;
import org.ciyex.ehr.task.service.ClinicalTaskService;
import org.ciyex.ehr.usermgmt.controller.RolePermissionController;
import org.ciyex.ehr.usermgmt.controller.UserManagementController;
import org.ciyex.ehr.usermgmt.service.EmailService;
import org.ciyex.ehr.usermgmt.service.RolePermissionService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Minimal Spring configuration for API security tests.
 * Loads the security layer + ALL production controllers + mocked services.
 * No database, no Vault, no FHIR server required.
 *
 * <p>Covers all 18 FHIR resource scope domains:
 * Patient, Encounter, Appointment, Observation, MedicationRequest,
 * DiagnosticReport, ServiceRequest, CarePlan, Immunization, Consent,
 * Task, Claim, Coverage, DocumentReference, Organization, Practitioner,
 * Communication, (system)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableWebMvc   // registers Jackson HttpMessageConverters so controllers can serialize/deserialize JSON
public class TestSecurityConfig {

    // ── Security filter chain ──────────────────────────────────────────────────

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/api/auth/**", "/api/internal/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(mockJwtDecoder()))
                )
                .build();
    }

    @Bean
    JwtDecoder mockJwtDecoder() {
        return Mockito.mock(JwtDecoder.class);
    }

    // ── Controllers under test ─────────────────────────────────────────────────
    // Organized by SMART on FHIR scope domain

    // --- Patient scope ---
    @Bean
    GenericFhirResourceController genericFhirResourceController() {
        return new GenericFhirResourceController(mockGenericFhirResourceService());
    }

    // --- Encounter scope ---
    @Bean
    EncounterSummaryController encounterSummaryController() {
        return new EncounterSummaryController(mockEncounterSummaryService());
    }

    // --- Appointment scope ---
    @Bean
    RecallController recallController() {
        return new RecallController(mockRecallService());
    }

    // --- Observation scope ---
    @Bean
    CdsAlertController cdsAlertController() {
        return new CdsAlertController(mockCdsService());
    }

    // --- MedicationRequest scope ---
    @Bean
    PrescriptionController prescriptionController() {
        return new PrescriptionController(mockPrescriptionService());
    }

    // --- DiagnosticReport scope ---
    @Bean
    LabResultController labResultController() {
        return new LabResultController(mockLabResultService());
    }

    @Bean
    LabOrderController labOrderController() {
        return new LabOrderController(mockLabOrderService());
    }

    // --- ServiceRequest scope ---
    @Bean
    ReferralController referralController() {
        return new ReferralController(mockReferralService());
    }

    // --- CarePlan scope ---
    @Bean
    CarePlanController carePlanController() {
        return new CarePlanController(mockCarePlanService());
    }

    // --- Immunization scope ---
    @Bean
    ImmunizationController immunizationController() {
        return new ImmunizationController(mockImmunizationService());
    }

    // --- Consent scope ---
    @Bean
    PatientConsentController patientConsentController() {
        return new PatientConsentController(mockPatientConsentService());
    }

    // --- Task scope ---
    @Bean
    ClinicalTaskController clinicalTaskController() {
        return new ClinicalTaskController(mockClinicalTaskService());
    }

    // --- Claim scope ---
    @Bean
    AllClaimsController allClaimsController() {
        return new AllClaimsController(mockPatientClaimService(), mockGenericFhirResourceService());
    }

    @Bean
    InvoiceController invoiceController() {
        return new InvoiceController(mockInvoiceService());
    }

    @Bean
    PaymentController paymentController() {
        return new PaymentController(mockPaymentService());
    }

    @Bean
    PatientBillingController patientBillingController() {
        return new PatientBillingController(
                mockPatientInvoiceService(),
                mockPatientClaimService(),
                mockPatientInsurancePaymentService(),
                mockPatientPaymentService(),
                mockPatientCreditService(),
                mockPatientBillingNoteService(),
                mockPatientDepositService(),
                mockPatientBillingPrintService()
        );
    }

    // --- Coverage scope ---
    @Bean
    InsuranceCompanyController insuranceCompanyController() {
        return new InsuranceCompanyController(mockInsuranceCompanyService());
    }

    // --- DocumentReference scope ---
    @Bean
    DocumentController documentController() {
        return new DocumentController(mockDocumentService());
    }

    // --- Organization scope ---
    @Bean
    OrgConfigController orgConfigController() {
        return new OrgConfigController(mockOrgConfigService());
    }

    @Bean
    AuditLogController auditLogController() {
        return new AuditLogController(mockAuditLogService());
    }

    @Bean
    AppInstallationController appInstallationController() {
        return new AppInstallationController(mockAppInstallationService());
    }

    @Bean
    RolePermissionController rolePermissionController() {
        return new RolePermissionController(mockRolePermissionService());
    }

    @Bean
    UserManagementController userManagementController() {
        return new UserManagementController(
                mockKeycloakUserService(),
                mockEmailService(),
                mockFhirClientService()
        );
    }

    // --- Practitioner scope ---
    @Bean
    UserController userController() {
        return new UserController(mockKeycloakUserService());
    }

    // --- Communication scope ---
    @Bean
    NotificationController notificationController() {
        return new NotificationController(mockNotificationService());
    }

    @Bean
    MessagingController messagingController() {
        return new MessagingController(mockMessagingService());
    }

    // ── Mocked service dependencies ────────────────────────────────────────────

    // Patient / FHIR generic
    @Bean GenericFhirResourceService mockGenericFhirResourceService() { return Mockito.mock(GenericFhirResourceService.class); }
    @Bean FhirClientService mockFhirClientService() { return Mockito.mock(FhirClientService.class); }

    // Encounter
    @Bean EncounterSummaryService mockEncounterSummaryService() { return Mockito.mock(EncounterSummaryService.class); }

    // Appointment / Recall
    @Bean RecallService mockRecallService() { return Mockito.mock(RecallService.class); }

    // Observation / CDS
    @Bean CdsService mockCdsService() { return Mockito.mock(CdsService.class); }

    // MedicationRequest / Prescription
    @Bean PrescriptionService mockPrescriptionService() { return Mockito.mock(PrescriptionService.class); }

    // DiagnosticReport / Lab
    @Bean LabResultService mockLabResultService() { return Mockito.mock(LabResultService.class); }
    @Bean LabOrderService mockLabOrderService() { return Mockito.mock(LabOrderService.class); }

    // ServiceRequest / Referral
    @Bean ReferralService mockReferralService() { return Mockito.mock(ReferralService.class); }

    // CarePlan
    @Bean CarePlanService mockCarePlanService() { return Mockito.mock(CarePlanService.class); }

    // Immunization
    @Bean ImmunizationService mockImmunizationService() { return Mockito.mock(ImmunizationService.class); }

    // Consent
    @Bean PatientConsentService mockPatientConsentService() { return Mockito.mock(PatientConsentService.class); }

    // Task
    @Bean ClinicalTaskService mockClinicalTaskService() { return Mockito.mock(ClinicalTaskService.class); }

    // Claim / Billing
    @Bean PatientClaimService mockPatientClaimService() { return Mockito.mock(PatientClaimService.class); }
    @Bean InvoiceService mockInvoiceService() { return Mockito.mock(InvoiceService.class); }
    @Bean PaymentService mockPaymentService() { return Mockito.mock(PaymentService.class); }
    @Bean PatientInvoiceService mockPatientInvoiceService() { return Mockito.mock(PatientInvoiceService.class); }
    @Bean PatientInsurancePaymentService mockPatientInsurancePaymentService() { return Mockito.mock(PatientInsurancePaymentService.class); }
    @Bean PatientPaymentService mockPatientPaymentService() { return Mockito.mock(PatientPaymentService.class); }
    @Bean PatientCreditService mockPatientCreditService() { return Mockito.mock(PatientCreditService.class); }
    @Bean PatientBillingNoteService mockPatientBillingNoteService() { return Mockito.mock(PatientBillingNoteService.class); }
    @Bean PatientDepositService mockPatientDepositService() { return Mockito.mock(PatientDepositService.class); }
    @Bean PatientBillingPrintService mockPatientBillingPrintService() { return Mockito.mock(PatientBillingPrintService.class); }

    // Coverage / Insurance
    @Bean InsuranceCompanyService mockInsuranceCompanyService() { return Mockito.mock(InsuranceCompanyService.class); }

    // DocumentReference
    @Bean DocumentService mockDocumentService() { return Mockito.mock(DocumentService.class); }

    // Organization
    @Bean OrgConfigService mockOrgConfigService() { return Mockito.mock(OrgConfigService.class); }
    @Bean AuditLogService mockAuditLogService() { return Mockito.mock(AuditLogService.class); }
    @Bean AppInstallationService mockAppInstallationService() { return Mockito.mock(AppInstallationService.class); }
    @Bean RolePermissionService mockRolePermissionService() { return Mockito.mock(RolePermissionService.class); }

    // Practitioner (KeycloakUserService shared with UserManagementController)
    @Bean KeycloakUserService mockKeycloakUserService() { return Mockito.mock(KeycloakUserService.class); }
    @Bean EmailService mockEmailService() { return Mockito.mock(EmailService.class); }

    // Communication
    @Bean NotificationService mockNotificationService() { return Mockito.mock(NotificationService.class); }
    @Bean MessagingService mockMessagingService() { return Mockito.mock(MessagingService.class); }
}
