package org.ciyex.ehr.security;

/**
 * SMART on FHIR permission scope constants for the Ciyex EHR API.
 *
 * Format follows the SMART on FHIR clinical scope specification:
 * <pre>
 *   {context}/{ResourceType}.{action}
 * </pre>
 * <ul>
 *   <li>{@code user/}   — practitioner/staff acting on behalf of the organisation</li>
 *   <li>{@code patient/}— patient acting on their own records (patient portal)</li>
 *   <li>{@code system/} — machine/system-level cross-org access</li>
 *   <li>action: {@code read} | {@code write} | {@code *}</li>
 * </ul>
 *
 * <h3>Usage in Spring Security</h3>
 * These constants are used as {@code SCOPE_*} GrantedAuthority strings:
 * <pre>
 *   @PreAuthorize("hasAuthority('" + Permission.PATIENT_READ + "')")
 * </pre>
 *
 * <h3>Keycloak SMART scopes (optional)</h3>
 * If Keycloak is configured to issue SMART client scopes directly in the JWT
 * {@code scope} claim, those are honoured in addition to the role-expanded scopes.
 */
public final class Permission {

    private Permission() {}

    // ─── Patient (FHIR Patient resource) ──────────────────────────────────────

    /** Read any patient record within the practice. */
    public static final String PATIENT_READ  = "SCOPE_user/Patient.read";
    /** Create or update patient records. */
    public static final String PATIENT_WRITE = "SCOPE_user/Patient.write";

    // ─── Appointments / Scheduling ────────────────────────────────────────────

    /** Read appointments and schedules. */
    public static final String APPOINTMENT_READ  = "SCOPE_user/Appointment.read";
    /** Create, reschedule, or cancel appointments. */
    public static final String APPOINTMENT_WRITE = "SCOPE_user/Appointment.write";

    // ─── Encounters ───────────────────────────────────────────────────────────

    /** Read clinical encounters. */
    public static final String ENCOUNTER_READ  = "SCOPE_user/Encounter.read";
    /** Create or update clinical encounters. */
    public static final String ENCOUNTER_WRITE = "SCOPE_user/Encounter.write";

    // ─── Observations (vitals, lab values, measurements) ─────────────────────

    /** Read clinical observations (vitals, growth, perio, etc.). */
    public static final String OBSERVATION_READ  = "SCOPE_user/Observation.read";
    /** Record or update clinical observations. */
    public static final String OBSERVATION_WRITE = "SCOPE_user/Observation.write";

    // ─── Procedures / Treatment ───────────────────────────────────────────────

    /** Read procedure records. */
    public static final String PROCEDURE_READ  = "SCOPE_user/Procedure.read";
    /** Create or update procedures. */
    public static final String PROCEDURE_WRITE = "SCOPE_user/Procedure.write";

    // ─── Medication Requests / Prescriptions ──────────────────────────────────

    /** Read prescriptions and medication orders. */
    public static final String MEDICATION_REQUEST_READ  = "SCOPE_user/MedicationRequest.read";
    /** Create or update prescriptions. */
    public static final String MEDICATION_REQUEST_WRITE = "SCOPE_user/MedicationRequest.write";

    // ─── Diagnostic Reports (Lab results, Imaging) ───────────────────────────

    /** Read lab orders, results, and diagnostic reports. */
    public static final String DIAGNOSTIC_REPORT_READ  = "SCOPE_user/DiagnosticReport.read";
    /** Submit or update lab orders and diagnostic reports. */
    public static final String DIAGNOSTIC_REPORT_WRITE = "SCOPE_user/DiagnosticReport.write";

    // ─── Service Requests (Referrals, Orders) ────────────────────────────────

    /** Read referrals and service/lab orders. */
    public static final String SERVICE_REQUEST_READ  = "SCOPE_user/ServiceRequest.read";
    /** Create or update referrals and orders. */
    public static final String SERVICE_REQUEST_WRITE = "SCOPE_user/ServiceRequest.write";

    // ─── Care Plans ───────────────────────────────────────────────────────────

    /** Read care plans. */
    public static final String CARE_PLAN_READ  = "SCOPE_user/CarePlan.read";
    /** Create or update care plans. */
    public static final String CARE_PLAN_WRITE = "SCOPE_user/CarePlan.write";

    // ─── Immunizations ────────────────────────────────────────────────────────

    /** Read immunization records. */
    public static final String IMMUNIZATION_READ  = "SCOPE_user/Immunization.read";
    /** Create or update immunization records. */
    public static final String IMMUNIZATION_WRITE = "SCOPE_user/Immunization.write";

    // ─── Document References (uploaded documents, notes) ──────────────────────

    /** Read documents and clinical notes. */
    public static final String DOCUMENT_REFERENCE_READ  = "SCOPE_user/DocumentReference.read";
    /** Upload or update documents. */
    public static final String DOCUMENT_REFERENCE_WRITE = "SCOPE_user/DocumentReference.write";

    // ─── Consent ──────────────────────────────────────────────────────────────

    /** Read patient consent records. */
    public static final String CONSENT_READ  = "SCOPE_user/Consent.read";
    /** Create or update consent records. */
    public static final String CONSENT_WRITE = "SCOPE_user/Consent.write";

    // ─── Tasks (Clinical Tasks) ───────────────────────────────────────────────

    /** Read clinical tasks. */
    public static final String TASK_READ  = "SCOPE_user/Task.read";
    /** Create or update clinical tasks. */
    public static final String TASK_WRITE = "SCOPE_user/Task.write";

    // ─── Claims / Billing ─────────────────────────────────────────────────────

    /** Read claims, invoices, and billing records. */
    public static final String CLAIM_READ  = "SCOPE_user/Claim.read";
    /** Create or update claims and billing records. */
    public static final String CLAIM_WRITE = "SCOPE_user/Claim.write";

    // ─── Coverage / Insurance ─────────────────────────────────────────────────

    /** Read insurance coverage and eligibility records. */
    public static final String COVERAGE_READ  = "SCOPE_user/Coverage.read";
    /** Create or update insurance coverage records. */
    public static final String COVERAGE_WRITE = "SCOPE_user/Coverage.write";

    // ─── Practitioners / Staff ────────────────────────────────────────────────

    /** Read provider/staff profiles and credentials. */
    public static final String PRACTITIONER_READ  = "SCOPE_user/Practitioner.read";
    /** Create or manage provider/staff profiles (admin only). */
    public static final String PRACTITIONER_WRITE = "SCOPE_user/Practitioner.write";

    // ─── Organization / Practice Settings ────────────────────────────────────

    /** Read practice settings, locations, and configuration. */
    public static final String ORGANIZATION_READ  = "SCOPE_user/Organization.read";
    /** Modify practice settings, locations, and configuration. */
    public static final String ORGANIZATION_WRITE = "SCOPE_user/Organization.write";

    // ─── Communication (Notifications, Messaging, Fax, Telehealth) ───────────

    /** Read communications, notifications, and messaging history. */
    public static final String COMMUNICATION_READ  = "SCOPE_user/Communication.read";
    /** Send communications or manage messaging and notifications. */
    public static final String COMMUNICATION_WRITE = "SCOPE_user/Communication.write";

    // ─── Allergy Intolerance ──────────────────────────────────────────────────

    /** Read allergy and intolerance records. */
    public static final String ALLERGY_INTOLERANCE_READ  = "SCOPE_user/AllergyIntolerance.read";
    /** Create or update allergy and intolerance records. */
    public static final String ALLERGY_INTOLERANCE_WRITE = "SCOPE_user/AllergyIntolerance.write";

    // ─── Condition (Medical Problems, Issues) ────────────────────────────────

    /** Read condition/problem list records. */
    public static final String CONDITION_READ  = "SCOPE_user/Condition.read";
    /** Create or update condition/problem list records. */
    public static final String CONDITION_WRITE = "SCOPE_user/Condition.write";

    // ─── RelatedPerson (Relationships) ─────────────────────────────────────

    /** Read related person / relationship records. */
    public static final String RELATED_PERSON_READ  = "SCOPE_user/RelatedPerson.read";
    /** Create or update related person records. */
    public static final String RELATED_PERSON_WRITE = "SCOPE_user/RelatedPerson.write";

    // ─── Flag (Clinical Alerts) ────────────────────────────────────────────

    /** Read clinical alert / flag records. */
    public static final String FLAG_READ  = "SCOPE_user/Flag.read";
    /** Create or update clinical alert / flag records. */
    public static final String FLAG_WRITE = "SCOPE_user/Flag.write";

    // ─── QuestionnaireResponse (Clinical History) ──────────────────────────

    /** Read questionnaire response / clinical history records. */
    public static final String QUESTIONNAIRE_RESPONSE_READ  = "SCOPE_user/QuestionnaireResponse.read";
    /** Create or update questionnaire response records. */
    public static final String QUESTIONNAIRE_RESPONSE_WRITE = "SCOPE_user/QuestionnaireResponse.write";

    // ─── Invoice (Payments, Statements) ────────────────────────────────────

    /** Read invoice / payment records. */
    public static final String INVOICE_READ  = "SCOPE_user/Invoice.read";
    /** Create or update invoice / payment records. */
    public static final String INVOICE_WRITE = "SCOPE_user/Invoice.write";

    // ─── MeasureReport (Reports) ───────────────────────────────────────────

    /** Read measure report / analytics records. */
    public static final String MEASURE_REPORT_READ  = "SCOPE_user/MeasureReport.read";
    /** Create or update measure report records. */
    public static final String MEASURE_REPORT_WRITE = "SCOPE_user/MeasureReport.write";

    // ─── Location (Facilities) ───────────────────────────────────────────────

    /** Read facility / location records. */
    public static final String LOCATION_READ  = "SCOPE_user/Location.read";
    /** Create or update facility / location records. */
    public static final String LOCATION_WRITE = "SCOPE_user/Location.write";

    // ─── ClaimResponse (Denials) ───────────────────────────────────────────

    /** Read claim response / denial records. */
    public static final String CLAIM_RESPONSE_READ  = "SCOPE_user/ClaimResponse.read";
    /** Create or update claim response records. */
    public static final String CLAIM_RESPONSE_WRITE = "SCOPE_user/ClaimResponse.write";

    // ─── ExplanationOfBenefit (ERA/Remittance) ─────────────────────────────

    /** Read explanation of benefit / ERA records. */
    public static final String EXPLANATION_OF_BENEFIT_READ  = "SCOPE_user/ExplanationOfBenefit.read";
    /** Create or update explanation of benefit records. */
    public static final String EXPLANATION_OF_BENEFIT_WRITE = "SCOPE_user/ExplanationOfBenefit.write";

    // ─── Composition (Encounter Forms / Clinical Notes) ──────────────────────

    /** Read composition / encounter form records. */
    public static final String COMPOSITION_READ  = "SCOPE_user/Composition.read";
    /** Create or update composition / encounter form records. */
    public static final String COMPOSITION_WRITE = "SCOPE_user/Composition.write";

    // ─── Patient self-access (patient/ SMART context) ────────────────────────

    /** Patient reads their own demographic record. */
    public static final String PATIENT_SELF_READ           = "SCOPE_patient/Patient.read";
    /** Patient reads their own appointments. */
    public static final String PATIENT_APPOINTMENT_READ    = "SCOPE_patient/Appointment.read";
    /** Patient reads their own clinical observations. */
    public static final String PATIENT_OBSERVATION_READ    = "SCOPE_patient/Observation.read";
    /** Patient reads their own medications. */
    public static final String PATIENT_MEDICATION_READ     = "SCOPE_patient/MedicationRequest.read";
    /** Patient reads their own diagnostic reports and lab results. */
    public static final String PATIENT_DIAGNOSTIC_READ     = "SCOPE_patient/DiagnosticReport.read";
    /** Patient reads their own documents. */
    public static final String PATIENT_DOCUMENT_READ       = "SCOPE_patient/DocumentReference.read";
    /** Patient reads their own care plan. */
    public static final String PATIENT_CARE_PLAN_READ      = "SCOPE_patient/CarePlan.read";
    /** Patient reads their own allergy/intolerance records. */
    public static final String PATIENT_ALLERGY_READ        = "SCOPE_patient/AllergyIntolerance.read";
    /** Patient reads their own communications and messages. */
    public static final String PATIENT_COMMUNICATION_READ  = "SCOPE_patient/Communication.read";
    /** Patient sends messages and communications. */
    public static final String PATIENT_COMMUNICATION_WRITE = "SCOPE_patient/Communication.write";
    /** Patient reads their own insurance coverage. */
    public static final String PATIENT_COVERAGE_READ       = "SCOPE_patient/Coverage.read";
    /** Patient reads their own billing claims. */
    public static final String PATIENT_CLAIM_READ          = "SCOPE_patient/Claim.read";
    /** Patient reads their own history (questionnaire responses). */
    public static final String PATIENT_QUESTIONNAIRE_READ  = "SCOPE_patient/QuestionnaireResponse.read";
    /** Patient reads their own conditions. */
    public static final String PATIENT_CONDITION_READ      = "SCOPE_patient/Condition.read";

}
