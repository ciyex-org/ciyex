--
-- PostgreSQL database dump
--

-- Dumped from database version 16.9 (Debian 16.9-1.pgdg110+1)
-- Dumped by pg_dump version 16.9 (Debian 16.9-1.pgdg110+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;

SET row_security = off;

--
-- Name: practice_1; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA practice_1;


ALTER SCHEMA practice_1 OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: allergy_details; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.allergy_details (
    id bigint NOT NULL,
    allergy_intolerance character varying(255),
    allergy_name character varying(255),
    reaction character varying(255),
    severity character varying(255),
    status character varying(255),
    allergy_intolerance_id bigint NOT NULL
);


ALTER TABLE practice_1.allergy_details OWNER TO postgres;

--
-- Name: allergy_details_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.allergy_details_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.allergy_details_id_seq OWNER TO postgres;

--
-- Name: allergy_details_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.allergy_details_id_seq OWNED BY practice_1.allergy_details.id;


--
-- Name: allergy_intolerances; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.allergy_intolerances (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    allergy_name character varying(255),
    reaction character varying(255),
    severity character varying(255),
    status character varying(255),
    start_date character varying(255),
    end_date character varying(255),
    comments character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255)
);


ALTER TABLE practice_1.allergy_intolerances OWNER TO postgres;

--
-- Name: allergy_intolerances_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.allergy_intolerances_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.allergy_intolerances_id_seq OWNER TO postgres;

--
-- Name: allergy_intolerances_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.allergy_intolerances_id_seq OWNED BY practice_1.allergy_intolerances.id;


--
-- Name: appointments; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.appointments (
    id bigint NOT NULL,
    org_id bigint,
    visit_type character varying(255),
    patient_id bigint,
    provider_id bigint,
    appointment_start_date character varying(255),
    appointment_end_date character varying(255),
    appointment_start_time character varying(255),
    appointment_end_time character varying(255),
    priority character varying(255),
    location_id bigint,
    status character varying(255),
    reason character varying(2000),
    created_date character varying(255),
    last_modified_date character varying(255)
);


ALTER TABLE practice_1.appointments OWNER TO postgres;

--
-- Name: appointments_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.appointments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.appointments_id_seq OWNER TO postgres;

--
-- Name: appointments_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.appointments_id_seq OWNED BY practice_1.appointments.id;


--
-- Name: assessment; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.assessment (
    id bigint NOT NULL,
    external_id character varying(128),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    diagnosis_code character varying(64),
    diagnosis_name character varying(512),
    status character varying(64),
    priority character varying(64),
    assessment_text character varying(255),
    notes character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.assessment OWNER TO postgres;

--
-- Name: assessment_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.assessment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.assessment_id_seq OWNER TO postgres;

--
-- Name: assessment_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.assessment_id_seq OWNED BY practice_1.assessment.id;


--
-- Name: assigned_providers; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.assigned_providers (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    provider_id bigint,
    role character varying(32),
    start_date character varying(16),
    end_date character varying(16),
    status character varying(24),
    notes character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.assigned_providers OWNER TO postgres;

--
-- Name: assigned_providers_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.assigned_providers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.assigned_providers_id_seq OWNER TO postgres;

--
-- Name: assigned_providers_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.assigned_providers_id_seq OWNED BY practice_1.assigned_providers.id;


--
-- Name: audit_log; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.audit_log (
    id bigint NOT NULL,
    event_time timestamp without time zone,
    user_id character varying(255),
    user_role character varying(255),
    session_id character varying(255),
    action_type character varying(255),
    entity_type character varying(255),
    entity_id character varying(255),
    patient_id bigint,
    description character varying(500),
    details character varying(255),
    ip_address character varying(255),
    user_agent character varying(255),
    endpoint character varying(500),
    http_method character varying(255),
    response_status integer,
    success boolean,
    error_message character varying(255),
    risk_level character varying(50),
    compliance_critical boolean,
    organization_id bigint,
    data_classification character varying(50),
    consent_reference character varying(255),
    actiontype character varying(50) NOT NULL,
    compliancecritical boolean NOT NULL,
    consentreference character varying(100),
    dataclassification character varying(255),
    entityid character varying(100),
    entitytype character varying(100) NOT NULL,
    errormessage character varying(1000),
    eventtime timestamp(6) without time zone NOT NULL,
    httpmethod character varying(10),
    ipaddress character varying(45),
    organizationid bigint,
    patientid bigint,
    responsestatus integer,
    risklevel character varying(255) NOT NULL,
    sessionid character varying(255),
    useragent character varying(500),
    userid character varying(100) NOT NULL,
    userrole character varying(50) NOT NULL,
    CONSTRAINT audit_log_dataclassification_check CHECK (((dataclassification)::text = ANY ((ARRAY['PUBLIC'::character varying, 'INTERNAL'::character varying, 'CONFIDENTIAL'::character varying, 'PHI'::character varying, 'SENSITIVE_PHI'::character varying])::text[]))),
    CONSTRAINT audit_log_risklevel_check CHECK (((risklevel)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[])))
);


ALTER TABLE practice_1.audit_log OWNER TO postgres;

--
-- Name: audit_log_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.audit_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.audit_log_id_seq OWNER TO postgres;

--
-- Name: audit_log_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.audit_log_id_seq OWNED BY practice_1.audit_log.id;


--
-- Name: billing_autopay; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.billing_autopay (
    id bigint NOT NULL,
    org_id bigint,
    user_id bigint,
    enabled boolean,
    start_date date,
    frequency character varying(255),
    max_amount character varying(255),
    card_id bigint,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    cardid bigint,
    createdat timestamp(6) without time zone,
    maxamount double precision,
    orgid bigint,
    startdate date,
    updatedat timestamp(6) without time zone,
    userid bigint
);


ALTER TABLE practice_1.billing_autopay OWNER TO postgres;

--
-- Name: billing_autopay_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.billing_autopay_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.billing_autopay_id_seq OWNER TO postgres;

--
-- Name: billing_autopay_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.billing_autopay_id_seq OWNED BY practice_1.billing_autopay.id;


--
-- Name: billing_cards; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.billing_cards (
    id bigint NOT NULL,
    org_id bigint,
    user_id bigint,
    stripe_payment_method_id character varying(64),
    stripe_customer_id character varying(64),
    brand character varying(255),
    last4 character varying(255),
    exp_month integer,
    exp_year integer,
    is_default boolean,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    createdat timestamp(6) without time zone,
    expmonth integer,
    expyear integer,
    isdefault boolean NOT NULL,
    orgid bigint NOT NULL,
    updatedat timestamp(6) without time zone,
    userid bigint NOT NULL
);


ALTER TABLE practice_1.billing_cards OWNER TO postgres;

--
-- Name: billing_cards_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.billing_cards_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.billing_cards_id_seq OWNER TO postgres;

--
-- Name: billing_cards_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.billing_cards_id_seq OWNED BY practice_1.billing_cards.id;


--
-- Name: billing_history; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.billing_history (
    id bigint NOT NULL,
    org_id bigint,
    user_id bigint,
    stripe_payment_intent_id character varying(255),
    stripe_payment_method_id character varying(255),
    amount character varying(255),
    status character varying(255),
    invoice_bill_id bigint,
    invoice_bill character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    createdat timestamp(6) without time zone,
    orgid bigint,
    stripepaymentintentid character varying(255),
    stripepaymentmethodid character varying(255) NOT NULL,
    updatedat timestamp(6) without time zone,
    userid bigint
);


ALTER TABLE practice_1.billing_history OWNER TO postgres;

--
-- Name: billing_history_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.billing_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.billing_history_id_seq OWNER TO postgres;

--
-- Name: billing_history_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.billing_history_id_seq OWNED BY practice_1.billing_history.id;


--
-- Name: chief_complaint; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.chief_complaint (
    id bigint NOT NULL,
    complaint character varying(255),
    details character varying(255),
    severity character varying(255),
    status character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.chief_complaint OWNER TO postgres;

--
-- Name: chief_complaint_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.chief_complaint_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.chief_complaint_id_seq OWNER TO postgres;

--
-- Name: chief_complaint_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.chief_complaint_id_seq OWNED BY practice_1.chief_complaint.id;


--
-- Name: code_types; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.code_types (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    code_type_key character varying(64),
    code_type_id integer,
    sequence_number integer,
    modifier integer,
    justification character varying(128),
    mask character varying(128),
    fee_applicable boolean,
    related_indicator boolean,
    number_of_services boolean,
    diagnosis_flag boolean,
    active boolean,
    label character varying(256),
    external_flag boolean,
    claim_flag boolean,
    procedure_flag boolean,
    terminology_flag boolean,
    problem_flag boolean,
    drug_flag boolean,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.code_types OWNER TO postgres;

--
-- Name: code_types_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.code_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.code_types_id_seq OWNER TO postgres;

--
-- Name: code_types_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.code_types_id_seq OWNED BY practice_1.code_types.id;


--
-- Name: codes; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.codes (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    code_type character varying(16),
    code character varying(32),
    modifier character varying(16),
    active boolean,
    description character varying(255),
    short_description character varying(256),
    category character varying(64),
    diagnosis_reporting boolean,
    service_reporting boolean,
    relate_to character varying(128),
    fee_standard numeric(18,2),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.codes OWNER TO postgres;

--
-- Name: codes_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.codes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.codes_id_seq OWNER TO postgres;

--
-- Name: codes_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.codes_id_seq OWNED BY practice_1.codes.id;


--
-- Name: communications; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.communications (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    status character varying(255),
    category character varying(255),
    sent_date character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    payload character varying(255),
    sender character varying(255),
    recipients character varying(255),
    subject character varying(255),
    in_response_to character varying(255),
    patient_id bigint,
    provider_id bigint
);


ALTER TABLE practice_1.communications OWNER TO postgres;

--
-- Name: communications_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.communications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.communications_id_seq OWNER TO postgres;

--
-- Name: communications_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.communications_id_seq OWNED BY practice_1.communications.id;


--
-- Name: coverages; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.coverages (
    id bigint NOT NULL,
    external_id character varying(255),
    coverage_type character varying(255),
    plan_name character varying(255),
    policy_number character varying(255),
    coverage_start_date character varying(255),
    coverage_end_date character varying(255),
    patient_id bigint,
    org_id bigint,
    insurance_company character varying(255),
    provider character varying(255),
    effective_date character varying(255),
    effective_date_end character varying(255),
    group_number character varying(255),
    subscriber_employer character varying(255),
    subscriber_address_line1 character varying(255),
    subscriber_address_line2 character varying(255),
    subscriber_city character varying(255),
    subscriber_state character varying(255),
    subscriber_zip_code character varying(255),
    subscriber_country character varying(255),
    subscriber_phone character varying(255),
    byholder_name character varying(255),
    byholder_relation character varying(255),
    byholder_address_line1 character varying(255),
    byholder_address_line2 character varying(255),
    byholder_city character varying(255),
    byholder_state character varying(255),
    byholder_zip_code character varying(255),
    byholder_country character varying(255),
    byholder_phone character varying(255),
    copay_amount character varying(255),
    insurance_company_id bigint
);


ALTER TABLE practice_1.coverages OWNER TO postgres;

--
-- Name: coverages_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.coverages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.coverages_id_seq OWNER TO postgres;

--
-- Name: coverages_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.coverages_id_seq OWNED BY practice_1.coverages.id;


--
-- Name: date_time_finalized; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.date_time_finalized (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    target_type character varying(32),
    target_id bigint,
    target_version character varying(64),
    finalized_at character varying(40),
    finalized_by character varying(128),
    finalizer_role character varying(64),
    method character varying(16),
    status character varying(24),
    reason character varying(256),
    comments character varying(255),
    content_hash character varying(128),
    provider_signature_id bigint,
    signoff_id bigint,
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.date_time_finalized OWNER TO postgres;

--
-- Name: date_time_finalized_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.date_time_finalized_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.date_time_finalized_id_seq OWNER TO postgres;

--
-- Name: date_time_finalized_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.date_time_finalized_id_seq OWNED BY practice_1.date_time_finalized.id;


--
-- Name: document; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.document (
    id bigint NOT NULL,
    org_id bigint,
    patient_id bigint,
    category character varying(255),
    type character varying(255),
    file_name character varying(255),
    content_type character varying(255),
    description character varying(255),
    fhir_external_id character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    s3bucket character varying(255),
    s3key character varying(255),
    encryption_key character varying(255),
    iv character varying(255),
    contenttype character varying(255),
    createddate character varying(255),
    encryptionkey character varying(512),
    fhirexternalid character varying(255),
    filename character varying(255),
    lastmodifieddate character varying(255),
    orgid bigint,
    patientid bigint
);


ALTER TABLE practice_1.document OWNER TO postgres;

--
-- Name: document_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.document_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.document_id_seq OWNER TO postgres;

--
-- Name: document_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.document_id_seq OWNED BY practice_1.document.id;


--
-- Name: document_settings; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.document_settings (
    id bigint NOT NULL,
    org_id bigint,
    max_upload_bytes bigint,
    enable_audio boolean,
    encryption_enabled boolean,
    allowed_file_types_json character varying(255),
    categories_json character varying(255),
    updated_by character varying(255),
    updated_at character varying(255),
    updatedat timestamp(6) with time zone,
    updatedby character varying(255)
);


ALTER TABLE practice_1.document_settings OWNER TO postgres;

--
-- Name: document_settings_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.document_settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.document_settings_id_seq OWNER TO postgres;

--
-- Name: document_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.document_settings_id_seq OWNED BY practice_1.document_settings.id;


--
-- Name: enc_fee_schedule_entries; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.enc_fee_schedule_entries (
    id bigint NOT NULL,
    schedule character varying(255),
    code_type character varying(16),
    code character varying(32),
    modifier character varying(16),
    description character varying(255),
    unit character varying(32),
    currency character varying(8),
    amount numeric(18,2),
    active boolean,
    notes character varying(255),
    schedule_id bigint NOT NULL
);


ALTER TABLE practice_1.enc_fee_schedule_entries OWNER TO postgres;

--
-- Name: enc_fee_schedule_entries_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.enc_fee_schedule_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.enc_fee_schedule_entries_id_seq OWNER TO postgres;

--
-- Name: enc_fee_schedule_entries_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.enc_fee_schedule_entries_id_seq OWNED BY practice_1.enc_fee_schedule_entries.id;


--
-- Name: enc_fee_schedules; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.enc_fee_schedules (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    name character varying(128),
    payer character varying(128),
    currency character varying(8),
    effective_from character varying(16),
    effective_to character varying(16),
    status character varying(24),
    notes character varying(255),
    entries character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.enc_fee_schedules OWNER TO postgres;

--
-- Name: enc_fee_schedules_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.enc_fee_schedules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.enc_fee_schedules_id_seq OWNER TO postgres;

--
-- Name: enc_fee_schedules_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.enc_fee_schedules_id_seq OWNED BY practice_1.enc_fee_schedules.id;


--
-- Name: encounter; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.encounter (
    id bigint NOT NULL,
    visit_category character varying(255),
    encounter_provider character varying(255),
    type character varying(255),
    sensitivity character varying(255),
    discharge_disposition character varying(255),
    reason_for_visit character varying(255),
    in_collection boolean,
    org_id bigint,
    status character varying(255),
    created_at bigint,
    encounter_date timestamp without time zone,
    updated_at bigint,
    patient_id bigint,
    createdat bigint NOT NULL,
    dischargedisposition character varying(255),
    encounterprovider character varying(255),
    incollection boolean,
    orgid bigint,
    patientid bigint NOT NULL,
    reasonforvisit character varying(255),
    updatedat bigint NOT NULL,
    visitcategory character varying(255)
);


ALTER TABLE practice_1.encounter OWNER TO postgres;

--
-- Name: encounter_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.encounter_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.encounter_id_seq OWNER TO postgres;

--
-- Name: encounter_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.encounter_id_seq OWNED BY practice_1.encounter.id;


--
-- Name: family_history; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.family_history (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    signed_entry_id bigint,
    entries character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.family_history OWNER TO postgres;

--
-- Name: family_history_entry; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.family_history_entry (
    id bigint NOT NULL,
    family_history character varying(255),
    relation character varying(24),
    diagnosis_code character varying(64),
    diagnosis_text character varying(255),
    notes character varying(1000),
    family_history_id bigint NOT NULL
);


ALTER TABLE practice_1.family_history_entry OWNER TO postgres;

--
-- Name: family_history_entry_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.family_history_entry_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.family_history_entry_id_seq OWNER TO postgres;

--
-- Name: family_history_entry_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.family_history_entry_id_seq OWNED BY practice_1.family_history_entry.id;


--
-- Name: family_history_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.family_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.family_history_id_seq OWNER TO postgres;

--
-- Name: family_history_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.family_history_id_seq OWNED BY practice_1.family_history.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE practice_1.flyway_schema_history OWNER TO postgres;

--
-- Name: healthcare_service; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.healthcare_service (
    id bigint NOT NULL,
    name character varying(255),
    description character varying(255),
    location character varying(255),
    type character varying(255),
    org_id bigint,
    hours_of_operation character varying(255)
);


ALTER TABLE practice_1.healthcare_service OWNER TO postgres;

--
-- Name: healthcare_service_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.healthcare_service_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.healthcare_service_id_seq OWNER TO postgres;

--
-- Name: healthcare_service_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.healthcare_service_id_seq OWNED BY practice_1.healthcare_service.id;


--
-- Name: healthcareservice; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.healthcareservice (
    id bigint NOT NULL,
    description character varying(255),
    hoursofoperation character varying(255),
    location character varying(255),
    name character varying(255),
    orgid bigint,
    type character varying(255)
);


ALTER TABLE practice_1.healthcareservice OWNER TO postgres;

--
-- Name: healthcareservice_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

ALTER TABLE practice_1.healthcareservice ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME practice_1.healthcareservice_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: history_of_present_illness; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.history_of_present_illness (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    description character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.history_of_present_illness OWNER TO postgres;

--
-- Name: history_of_present_illness_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.history_of_present_illness_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.history_of_present_illness_id_seq OWNER TO postgres;

--
-- Name: history_of_present_illness_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.history_of_present_illness_id_seq OWNED BY practice_1.history_of_present_illness.id;


--
-- Name: immunizations; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.immunizations (
    id bigint NOT NULL,
    external_id character varying(255),
    patient_id bigint,
    org_id bigint,
    cvx_code character varying(255),
    date_time_administered character varying(255),
    amount_administered character varying(255),
    expiration_date character varying(255),
    manufacturer character varying(255),
    lot_number character varying(255),
    administrator_name character varying(255),
    administrator_title character varying(255),
    date_vis_given character varying(255),
    date_vis_statement character varying(255),
    route character varying(255),
    administration_site character varying(255),
    notes character varying(255),
    information_source character varying(255),
    completion_status character varying(255),
    substance_refusal_reason character varying(255),
    reason_code character varying(255),
    ordering_provider character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    administrationsite character varying(255),
    administratorname character varying(255),
    administratortitle character varying(255),
    amountadministered character varying(255),
    completionstatus character varying(255),
    createddate character varying(255),
    cvxcode character varying(255),
    datetimeadministered character varying(255),
    datevisgiven character varying(255),
    datevisstatement character varying(255),
    expirationdate character varying(255),
    informationsource character varying(255),
    lastmodifieddate character varying(255),
    lotnumber character varying(255),
    orderingprovider character varying(255),
    reasoncode character varying(255),
    substancerefusalreason character varying(255)
);


ALTER TABLE practice_1.immunizations OWNER TO postgres;

--
-- Name: immunizations_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.immunizations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.immunizations_id_seq OWNER TO postgres;

--
-- Name: immunizations_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.immunizations_id_seq OWNED BY practice_1.immunizations.id;


--
-- Name: insurance_companies; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.insurance_companies (
    id bigint NOT NULL,
    name character varying(255),
    address character varying(255),
    city character varying(255),
    state character varying(255),
    payer_id character varying(255),
    postal_code character varying(255),
    country character varying(255),
    fhir_id character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    status character varying(255)
);


ALTER TABLE practice_1.insurance_companies OWNER TO postgres;

--
-- Name: insurance_companies_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.insurance_companies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.insurance_companies_id_seq OWNER TO postgres;

--
-- Name: insurance_companies_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.insurance_companies_id_seq OWNED BY practice_1.insurance_companies.id;


--
-- Name: inventory; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.inventory (
    id bigint NOT NULL,
    org_id bigint,
    name character varying(255),
    category character varying(255),
    lot character varying(255),
    expiry character varying(255),
    sku character varying(255),
    stock integer,
    unit character varying(255),
    min_stock integer,
    location character varying(255),
    status character varying(255),
    supplier character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    external_id character varying(255),
    orders character varying(255),
    createddate character varying(255),
    externalid character varying(255),
    lastmodifieddate character varying(255),
    minstock integer,
    orgid bigint
);


ALTER TABLE practice_1.inventory OWNER TO postgres;

--
-- Name: inventory_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.inventory_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.inventory_id_seq OWNER TO postgres;

--
-- Name: inventory_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.inventory_id_seq OWNED BY practice_1.inventory.id;


--
-- Name: inventory_settings; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.inventory_settings (
    id bigint NOT NULL,
    org_id bigint,
    low_stock_alerts boolean,
    auto_reorder_suggestions boolean,
    critical_low_percentage integer,
    created_date character varying(255),
    last_modified_date character varying(255)
);


ALTER TABLE practice_1.inventory_settings OWNER TO postgres;

--
-- Name: inventory_settings_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.inventory_settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.inventory_settings_id_seq OWNER TO postgres;

--
-- Name: inventory_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.inventory_settings_id_seq OWNED BY practice_1.inventory_settings.id;


--
-- Name: inventorysettings; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.inventorysettings (
    id bigint NOT NULL,
    autoreordersuggestions boolean NOT NULL,
    createddate character varying(255),
    criticallowpercentage integer NOT NULL,
    lastmodifieddate character varying(255),
    lowstockalerts boolean NOT NULL,
    orgid bigint
);


ALTER TABLE practice_1.inventorysettings OWNER TO postgres;

--
-- Name: inventorysettings_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

ALTER TABLE practice_1.inventorysettings ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME practice_1.inventorysettings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: invoice; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.invoice (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    invoice_number character varying(255),
    status character varying(255),
    currency character varying(255),
    issue_date character varying(255),
    due_date character varying(255),
    payer character varying(255),
    notes character varying(255),
    total_gross numeric(18,2),
    total_net numeric(18,2),
    lines character varying(255),
    payments character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.invoice OWNER TO postgres;

--
-- Name: invoice_bills; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.invoice_bills (
    id bigint NOT NULL,
    org_id bigint,
    user_id bigint,
    subscription_id bigint,
    amount character varying(255),
    status character varying(255),
    invoice_url character varying(255),
    receipt_url character varying(255),
    external_id character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    due_date timestamp without time zone,
    createdat timestamp(6) without time zone,
    duedate timestamp(6) without time zone,
    externalid character varying(255),
    invoiceurl character varying(255),
    orgid bigint,
    receipturl character varying(255),
    subscriptionid bigint,
    updatedat timestamp(6) without time zone,
    userid bigint
);


ALTER TABLE practice_1.invoice_bills OWNER TO postgres;

--
-- Name: invoice_bills_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.invoice_bills_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.invoice_bills_id_seq OWNER TO postgres;

--
-- Name: invoice_bills_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.invoice_bills_id_seq OWNED BY practice_1.invoice_bills.id;


--
-- Name: invoice_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.invoice_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.invoice_id_seq OWNER TO postgres;

--
-- Name: invoice_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.invoice_id_seq OWNED BY practice_1.invoice.id;


--
-- Name: invoice_line; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.invoice_line (
    id bigint NOT NULL,
    invoice character varying(255),
    description character varying(1024),
    code character varying(20),
    quantity integer,
    unit_price numeric(18,2),
    amount numeric(18,2),
    invoice_id bigint NOT NULL
);


ALTER TABLE practice_1.invoice_line OWNER TO postgres;

--
-- Name: invoice_line_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.invoice_line_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.invoice_line_id_seq OWNER TO postgres;

--
-- Name: invoice_line_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.invoice_line_id_seq OWNED BY practice_1.invoice_line.id;


--
-- Name: invoice_payment; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.invoice_payment (
    id bigint NOT NULL,
    invoice character varying(255),
    date character varying(255),
    amount numeric(18,2),
    method character varying(32),
    reference character varying(64),
    note character varying(255),
    pay_date character varying(255),
    invoice_id bigint NOT NULL
);


ALTER TABLE practice_1.invoice_payment OWNER TO postgres;

--
-- Name: invoice_payment_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.invoice_payment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.invoice_payment_id_seq OWNER TO postgres;

--
-- Name: invoice_payment_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.invoice_payment_id_seq OWNED BY practice_1.invoice_payment.id;


--
-- Name: lab_orders; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.lab_orders (
    id bigint NOT NULL,
    org_id bigint,
    patient_id bigint,
    patient_external_id character varying(255),
    mrn character varying(255),
    encounter_id character varying(255),
    physician_name character varying(255),
    patient_first_name character varying(255),
    patient_last_name character varying(255),
    patient_home_phone character varying(255),
    order_date_time character varying(255),
    order_name character varying(255),
    lab_name character varying(255),
    order_number character varying(255),
    test_code character varying(255),
    test_display character varying(255),
    status character varying(255),
    priority character varying(255),
    order_date character varying(255),
    specimen_id character varying(255),
    notes character varying(2048),
    ordering_provider character varying(255),
    icd_id character varying(64),
    result character varying(4096),
    created_date character varying(255),
    last_modified_date character varying(255),
    order_datetime character varying(255)
);


ALTER TABLE practice_1.lab_orders OWNER TO postgres;

--
-- Name: lab_orders_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.lab_orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.lab_orders_id_seq OWNER TO postgres;

--
-- Name: lab_orders_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.lab_orders_id_seq OWNED BY practice_1.lab_orders.id;


--
-- Name: list_options; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.list_options (
    id bigint NOT NULL,
    org_id character varying(255),
    list_id character varying(255),
    option_id character varying(255),
    title character varying(255),
    seq integer,
    is_default boolean,
    option_value character varying(255),
    notes character varying(255),
    codes character varying(255),
    activity integer,
    edit_options boolean,
    "timestamp" timestamp without time zone,
    last_updated timestamp without time zone
);


ALTER TABLE practice_1.list_options OWNER TO postgres;

--
-- Name: list_options_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.list_options_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.list_options_id_seq OWNER TO postgres;

--
-- Name: list_options_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.list_options_id_seq OWNED BY practice_1.list_options.id;


--
-- Name: locations; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.locations (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    name character varying(255),
    address character varying(255),
    city character varying(255),
    state character varying(255),
    postal_code character varying(255),
    country character varying(255),
    postalcode character varying(255)
);


ALTER TABLE practice_1.locations OWNER TO postgres;

--
-- Name: locations_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.locations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.locations_id_seq OWNER TO postgres;

--
-- Name: locations_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.locations_id_seq OWNED BY practice_1.locations.id;


--
-- Name: maintenance; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.maintenance (
    id bigint NOT NULL,
    org_id bigint,
    equipment character varying(255),
    category character varying(255),
    location character varying(255),
    due_date character varying(255),
    last_service_date character varying(255),
    assignee character varying(255),
    vendor character varying(255),
    priority character varying(255),
    status character varying(255),
    notes character varying(2000),
    created_date character varying(255),
    last_modified_date character varying(255),
    external_id character varying(255),
    createddate character varying(255),
    duedate character varying(255),
    externalid character varying(255),
    lastmodifieddate character varying(255),
    lastservicedate character varying(255),
    orgid bigint
);


ALTER TABLE practice_1.maintenance OWNER TO postgres;

--
-- Name: maintenance_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.maintenance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.maintenance_id_seq OWNER TO postgres;

--
-- Name: maintenance_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.maintenance_id_seq OWNED BY practice_1.maintenance.id;


--
-- Name: medical_problems; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.medical_problems (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    title character varying(255),
    outcome character varying(255),
    verification_status character varying(255),
    occurrence character varying(255),
    note character varying(2000),
    created_date character varying(255),
    last_modified_date character varying(255)
);


ALTER TABLE practice_1.medical_problems OWNER TO postgres;

--
-- Name: medical_problems_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.medical_problems_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.medical_problems_id_seq OWNER TO postgres;

--
-- Name: medical_problems_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.medical_problems_id_seq OWNED BY practice_1.medical_problems.id;


--
-- Name: medication_requests; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.medication_requests (
    id bigint NOT NULL,
    patient_id bigint,
    encounter_id bigint,
    medication_name character varying(255),
    dosage character varying(255),
    instructions character varying(255),
    date_issued character varying(255),
    prescribing_doctor character varying(255),
    status character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255)
);


ALTER TABLE practice_1.medication_requests OWNER TO postgres;

--
-- Name: medication_requests_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.medication_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.medication_requests_id_seq OWNER TO postgres;

--
-- Name: medication_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.medication_requests_id_seq OWNED BY practice_1.medication_requests.id;


--
-- Name: orders; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.orders (
    id bigint NOT NULL,
    org_id bigint,
    order_number character varying(255),
    supplier character varying(255),
    date character varying(255),
    status character varying(255),
    stock integer,
    item_name character varying(255),
    category character varying(255),
    amount character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    external_id character varying(255),
    inventory character varying(255),
    createddate character varying(255),
    externalid character varying(255),
    itemname character varying(255),
    lastmodifieddate character varying(255),
    ordernumber character varying(255),
    orgid bigint,
    inventory_id bigint
);


ALTER TABLE practice_1.orders OWNER TO postgres;

--
-- Name: orders_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.orders_id_seq OWNER TO postgres;

--
-- Name: orders_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.orders_id_seq OWNED BY practice_1.orders.id;


--
-- Name: org_config; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.org_config (
    id bigint NOT NULL,
    org_id bigint,
    integrations jsonb
);


ALTER TABLE practice_1.org_config OWNER TO postgres;

--
-- Name: org_config_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.org_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.org_config_id_seq OWNER TO postgres;

--
-- Name: org_config_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.org_config_id_seq OWNED BY practice_1.org_config.id;


--
-- Name: past_medical_history; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.past_medical_history (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    description character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.past_medical_history OWNER TO postgres;

--
-- Name: past_medical_history_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.past_medical_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.past_medical_history_id_seq OWNER TO postgres;

--
-- Name: past_medical_history_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.past_medical_history_id_seq OWNED BY practice_1.past_medical_history.id;


--
-- Name: patient_education; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.patient_education (
    id bigint NOT NULL,
    org_id bigint,
    title character varying(255),
    summary character varying(255),
    category character varying(255),
    language character varying(255),
    reading_level character varying(255),
    content character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    external_id character varying(255)
);


ALTER TABLE practice_1.patient_education OWNER TO postgres;

--
-- Name: patient_education_assignment; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.patient_education_assignment (
    id bigint NOT NULL,
    education character varying(255),
    patient_id bigint,
    patient_name character varying(255),
    notes character varying(255),
    delivered boolean,
    assigned_date timestamp without time zone
);


ALTER TABLE practice_1.patient_education_assignment OWNER TO postgres;

--
-- Name: patient_education_assignment_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.patient_education_assignment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.patient_education_assignment_id_seq OWNER TO postgres;

--
-- Name: patient_education_assignment_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.patient_education_assignment_id_seq OWNED BY practice_1.patient_education_assignment.id;


--
-- Name: patient_education_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.patient_education_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.patient_education_id_seq OWNER TO postgres;

--
-- Name: patient_education_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.patient_education_id_seq OWNED BY practice_1.patient_education.id;


--
-- Name: patient_medical_history; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.patient_medical_history (
    id bigint NOT NULL,
    patient_id bigint,
    encounter_id bigint,
    org_id bigint,
    external_id character varying(255),
    medical_condition character varying(255),
    condition_name character varying(255),
    status character varying(50),
    is_chronic boolean,
    diagnosis_date timestamp without time zone,
    onset_date date,
    resolved_date date,
    created_date date,
    last_modified_date date,
    treatment_details character varying(255),
    diagnosis_details character varying(255),
    notes character varying(255),
    description character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.patient_medical_history OWNER TO postgres;

--
-- Name: patient_medical_history_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.patient_medical_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.patient_medical_history_id_seq OWNER TO postgres;

--
-- Name: patient_medical_history_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.patient_medical_history_id_seq OWNED BY practice_1.patient_medical_history.id;


--
-- Name: patienteducation; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.patienteducation (
    id bigint NOT NULL,
    category character varying(255),
    content character varying(5000),
    createddate character varying(255),
    externalid character varying(255),
    language character varying(255),
    lastmodifieddate character varying(255),
    orgid bigint,
    readinglevel character varying(255),
    summary character varying(255),
    title character varying(255)
);


ALTER TABLE practice_1.patienteducation OWNER TO postgres;

--
-- Name: patienteducation_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

ALTER TABLE practice_1.patienteducation ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME practice_1.patienteducation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: patienteducationassignment; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.patienteducationassignment (
    id bigint NOT NULL,
    assigneddate timestamp(6) without time zone,
    delivered boolean NOT NULL,
    notes character varying(255),
    patientid bigint NOT NULL,
    patientname character varying(255),
    education_id bigint NOT NULL
);


ALTER TABLE practice_1.patienteducationassignment OWNER TO postgres;

--
-- Name: patienteducationassignment_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

ALTER TABLE practice_1.patienteducationassignment ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME practice_1.patienteducationassignment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: patients; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.patients (
    id bigint NOT NULL,
    external_id character varying(255),
    status character varying(255),
    org_id bigint,
    first_name character varying(255),
    last_name character varying(255),
    middle_name character varying(255),
    gender character varying(255),
    date_of_birth character varying(255),
    phone_number character varying(255),
    email character varying(255),
    address character varying(255),
    medical_record_number character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255)
);


ALTER TABLE practice_1.patients OWNER TO postgres;

--
-- Name: patients_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.patients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.patients_id_seq OWNER TO postgres;

--
-- Name: patients_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.patients_id_seq OWNED BY practice_1.patients.id;


--
-- Name: payment_orders; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.payment_orders (
    id bigint NOT NULL,
    stripe_payment_intent_id character varying(255),
    amount bigint,
    status character varying(255),
    stripepaymentintentid character varying(255)
);


ALTER TABLE practice_1.payment_orders OWNER TO postgres;

--
-- Name: payment_orders_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.payment_orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.payment_orders_id_seq OWNER TO postgres;

--
-- Name: payment_orders_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.payment_orders_id_seq OWNED BY practice_1.payment_orders.id;


--
-- Name: physical_exam; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.physical_exam (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    sections character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.physical_exam OWNER TO postgres;

--
-- Name: physical_exam_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.physical_exam_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.physical_exam_id_seq OWNER TO postgres;

--
-- Name: physical_exam_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.physical_exam_id_seq OWNED BY practice_1.physical_exam.id;


--
-- Name: physical_exam_section; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.physical_exam_section (
    id bigint NOT NULL,
    all_normal boolean,
    findings character varying(4000),
    normal_text character varying(2000),
    section_key character varying(48),
    physical_exam character varying(255),
    physical_exam_id bigint NOT NULL
);


ALTER TABLE practice_1.physical_exam_section OWNER TO postgres;

--
-- Name: physical_exam_section_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.physical_exam_section_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.physical_exam_section_id_seq OWNER TO postgres;

--
-- Name: physical_exam_section_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.physical_exam_section_id_seq OWNED BY practice_1.physical_exam_section.id;


--
-- Name: plan; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.plan (
    id bigint NOT NULL,
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    diagnostic_plan character varying(255),
    plan character varying(255),
    notes character varying(255),
    follow_up_visit character varying(255),
    return_work_school character varying(255),
    sections_json character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.plan OWNER TO postgres;

--
-- Name: plan_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.plan_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.plan_id_seq OWNER TO postgres;

--
-- Name: plan_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.plan_id_seq OWNED BY practice_1.plan.id;


--
-- Name: portal_demographics; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.portal_demographics (
    id bigint NOT NULL,
    first_name character varying(255),
    middle_name character varying(255),
    last_name character varying(255),
    dob date,
    sex character varying(255),
    marital_status character varying(255),
    address character varying(255),
    city character varying(255),
    state character varying(255),
    postal_code character varying(255),
    country character varying(255),
    phone_mobile character varying(255),
    contact_email character varying(255),
    emergency_contact_name character varying(255),
    emergency_contact_phone character varying(255),
    allow_sms boolean,
    allow_email boolean,
    allow_voice_message boolean,
    allow_mail_message boolean,
    patient character varying(255),
    allowemail boolean NOT NULL,
    allowmailmessage boolean NOT NULL,
    allowsms boolean NOT NULL,
    allowvoicemessage boolean NOT NULL,
    contactemail character varying(255),
    emergencycontactname character varying(255),
    emergencycontactphone character varying(255),
    firstname character varying(255),
    lastname character varying(255),
    maritalstatus character varying(255),
    middlename character varying(255),
    phonemobile character varying(255),
    postalcode character varying(255),
    patient_id bigint
);


ALTER TABLE practice_1.portal_demographics OWNER TO postgres;

--
-- Name: portal_demographics_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.portal_demographics_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.portal_demographics_id_seq OWNER TO postgres;

--
-- Name: portal_demographics_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.portal_demographics_id_seq OWNED BY practice_1.portal_demographics.id;


--
-- Name: portal_patients; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.portal_patients (
    id bigint NOT NULL,
    "user" character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    dob date,
    gender character varying(255),
    phone character varying(255),
    email character varying(255),
    address character varying(255),
    insurance_id character varying(255),
    firstname character varying(255),
    insuranceid character varying(255),
    lastname character varying(255),
    user_id bigint NOT NULL
);


ALTER TABLE practice_1.portal_patients OWNER TO postgres;

--
-- Name: portal_patients_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.portal_patients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.portal_patients_id_seq OWNER TO postgres;

--
-- Name: portal_patients_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.portal_patients_id_seq OWNED BY practice_1.portal_patients.id;


--
-- Name: portal_profiles; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.portal_profiles (
    id bigint NOT NULL,
    user_id bigint,
    first_name character varying(255),
    last_name character varying(255),
    phone character varying(20),
    email character varying(150),
    date_of_birth date,
    street character varying(255),
    city character varying(100),
    state character varying(100),
    postal_code character varying(255),
    country character varying(100),
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    createdat timestamp(6) without time zone,
    dateofbirth date,
    firstname character varying(100) NOT NULL,
    lastname character varying(100) NOT NULL,
    postalcode character varying(20),
    updatedat timestamp(6) without time zone
);


ALTER TABLE practice_1.portal_profiles OWNER TO postgres;

--
-- Name: portal_profiles_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.portal_profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.portal_profiles_id_seq OWNER TO postgres;

--
-- Name: portal_profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.portal_profiles_id_seq OWNED BY practice_1.portal_profiles.id;


--
-- Name: portal_users; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.portal_users (
    id bigint NOT NULL,
    email character varying(255),
    password character varying(255),
    patient character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    middle_name character varying(255),
    date_of_birth date,
    phone_number character varying(255),
    city character varying(255),
    state character varying(255),
    country character varying(255),
    street character varying(255),
    street2 character varying(255),
    postal_code character varying(255),
    profile_image character varying(255),
    security_question character varying(255),
    security_answer character varying(255),
    uuid character varying(255),
    org_id bigint,
    role character varying(50),
    dateofbirth date,
    firstname character varying(255),
    lastname character varying(255),
    middlename character varying(255),
    phonenumber character varying(255),
    postalcode character varying(255),
    profileimage character varying(255),
    securityanswer character varying(255),
    securityquestion character varying(255)
);


ALTER TABLE practice_1.portal_users OWNER TO postgres;

--
-- Name: portal_users_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.portal_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.portal_users_id_seq OWNER TO postgres;

--
-- Name: portal_users_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.portal_users_id_seq OWNED BY practice_1.portal_users.id;


--
-- Name: practitioner_role; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.practitioner_role (
    id bigint NOT NULL,
    role character varying(255),
    specialty character varying(255),
    location character varying(255),
    org_id bigint,
    provider_id bigint,
    updated_at bigint
);


ALTER TABLE practice_1.practitioner_role OWNER TO postgres;

--
-- Name: practitioner_role_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.practitioner_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.practitioner_role_id_seq OWNER TO postgres;

--
-- Name: practitioner_role_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.practitioner_role_id_seq OWNED BY practice_1.practitioner_role.id;


--
-- Name: practitionerrole; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.practitionerrole (
    id bigint NOT NULL,
    organization_id bigint NOT NULL,
    providerid bigint,
    role character varying(255),
    specialty character varying(255),
    updatedat bigint,
    location_id bigint NOT NULL
);


ALTER TABLE practice_1.practitionerrole OWNER TO postgres;

--
-- Name: practitionerrole_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

ALTER TABLE practice_1.practitionerrole ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME practice_1.practitionerrole_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: procedure_item; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.procedure_item (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    cpt4 character varying(16),
    description character varying(1024),
    units integer,
    rate character varying(64),
    related_icds character varying(512),
    hospital_billing_start character varying(255),
    hospital_billing_end character varying(255),
    modifier1 character varying(10),
    modifier2 character varying(10),
    modifier3 character varying(10),
    modifier4 character varying(10),
    note character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    hb_end character varying(32),
    hb_start character varying(32)
);


ALTER TABLE practice_1.procedure_item OWNER TO postgres;

--
-- Name: procedure_item_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.procedure_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.procedure_item_id_seq OWNER TO postgres;

--
-- Name: procedure_item_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.procedure_item_id_seq OWNED BY practice_1.procedure_item.id;


--
-- Name: provider; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.provider (
    id bigint NOT NULL,
    org_id bigint,
    npi character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    middle_name character varying(255),
    prefix character varying(255),
    suffix character varying(255),
    gender character varying(255),
    date_of_birth character varying(255),
    photo character varying(255),
    email character varying(255),
    phone_number character varying(255),
    mobile_number character varying(255),
    fax_number character varying(255),
    address character varying(255),
    specialty character varying(255),
    provider_type character varying(255),
    license_number character varying(255),
    license_state character varying(255),
    license_expiry character varying(255),
    external_id character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    status character varying(255)
);


ALTER TABLE practice_1.provider OWNER TO postgres;

--
-- Name: provider_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.provider_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.provider_id_seq OWNER TO postgres;

--
-- Name: provider_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.provider_id_seq OWNED BY practice_1.provider.id;


--
-- Name: provider_note; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.provider_note (
    id bigint NOT NULL,
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    note_title character varying(255),
    note_type_code character varying(255),
    note_status character varying(255),
    note_date_time timestamp without time zone,
    author_practitioner_id bigint,
    subjective character varying(255),
    objective character varying(255),
    assessment character varying(255),
    plan character varying(255),
    narrative character varying(255),
    external_id character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    note_datetime timestamp(6) without time zone
);


ALTER TABLE practice_1.provider_note OWNER TO postgres;

--
-- Name: provider_note_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.provider_note_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.provider_note_id_seq OWNER TO postgres;

--
-- Name: provider_note_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.provider_note_id_seq OWNED BY practice_1.provider_note.id;


--
-- Name: provider_signatures; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.provider_signatures (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    signed_at character varying(40),
    signed_by character varying(128),
    signer_role character varying(64),
    signature_type character varying(32),
    signature_format character varying(64),
    signature_data character varying(255),
    signature_hash character varying(128),
    status character varying(64),
    comments character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.provider_signatures OWNER TO postgres;

--
-- Name: provider_signatures_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.provider_signatures_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.provider_signatures_id_seq OWNER TO postgres;

--
-- Name: provider_signatures_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.provider_signatures_id_seq OWNED BY practice_1.provider_signatures.id;


--
-- Name: recall; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.recall (
    id bigint NOT NULL,
    org_id bigint,
    patient_id bigint,
    provider_id bigint,
    patient_name character varying(255),
    dob character varying(255),
    address character varying(255),
    city character varying(255),
    state character varying(255),
    zip_code character varying(255),
    phone character varying(255),
    email character varying(255),
    last_visit character varying(255),
    recall_date character varying(255),
    recall_reason character varying(255),
    sms_consent boolean,
    email_consent boolean,
    created_date character varying(255),
    last_modified_date character varying(255),
    external_id character varying(255),
    createddate character varying(255),
    emailconsent boolean NOT NULL,
    externalid character varying(255),
    lastmodifieddate character varying(255),
    lastvisit character varying(255),
    orgid bigint,
    patientid bigint,
    patientname character varying(255),
    providerid bigint,
    recalldate character varying(255),
    recallreason character varying(255),
    smsconsent boolean NOT NULL,
    zipcode character varying(255)
);


ALTER TABLE practice_1.recall OWNER TO postgres;

--
-- Name: recall_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.recall_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.recall_id_seq OWNER TO postgres;

--
-- Name: recall_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.recall_id_seq OWNED BY practice_1.recall.id;


--
-- Name: referral_practices; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.referral_practices (
    id bigint NOT NULL,
    name character varying(255),
    address character varying(255),
    city character varying(255),
    state character varying(255),
    postal_code character varying(255),
    country character varying(255),
    phone_number character varying(255),
    email character varying(255),
    fhir_id character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255)
);


ALTER TABLE practice_1.referral_practices OWNER TO postgres;

--
-- Name: referral_practices_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.referral_practices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.referral_practices_id_seq OWNER TO postgres;

--
-- Name: referral_practices_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.referral_practices_id_seq OWNED BY practice_1.referral_practices.id;


--
-- Name: referral_providers; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.referral_providers (
    id bigint NOT NULL,
    name character varying(255),
    specialty character varying(255),
    address character varying(255),
    city character varying(255),
    state character varying(255),
    postal_code character varying(255),
    country character varying(255),
    phone_number character varying(255),
    email character varying(255),
    fhir_id character varying(255),
    practice character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    practice_id bigint NOT NULL
);


ALTER TABLE practice_1.referral_providers OWNER TO postgres;

--
-- Name: referral_providers_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.referral_providers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.referral_providers_id_seq OWNER TO postgres;

--
-- Name: referral_providers_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.referral_providers_id_seq OWNED BY practice_1.referral_providers.id;


--
-- Name: review_of_system_details; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.review_of_system_details (
    ros_id bigint NOT NULL,
    detail character varying(128)
);


ALTER TABLE practice_1.review_of_system_details OWNER TO postgres;

--
-- Name: review_of_systems; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.review_of_systems (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    system_name character varying(64),
    is_negative boolean,
    notes character varying(255),
    system_details character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.review_of_systems OWNER TO postgres;

--
-- Name: review_of_systems_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.review_of_systems_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.review_of_systems_id_seq OWNER TO postgres;

--
-- Name: review_of_systems_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.review_of_systems_id_seq OWNED BY practice_1.review_of_systems.id;


--
-- Name: schedules; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.schedules (
    id bigint NOT NULL,
    provider_id bigint,
    org_id bigint,
    external_id character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    createddate character varying(255),
    lastmodifieddate character varying(255)
);


ALTER TABLE practice_1.schedules OWNER TO postgres;

--
-- Name: schedules_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.schedules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.schedules_id_seq OWNER TO postgres;

--
-- Name: schedules_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.schedules_id_seq OWNED BY practice_1.schedules.id;


--
-- Name: services; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.services (
    id bigint NOT NULL,
    name character varying(255),
    default_price character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    createddate character varying(255) NOT NULL,
    defaultprice character varying(255),
    lastmodifieddate character varying(255) NOT NULL
);


ALTER TABLE practice_1.services OWNER TO postgres;

--
-- Name: services_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.services_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.services_id_seq OWNER TO postgres;

--
-- Name: services_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.services_id_seq OWNED BY practice_1.services.id;


--
-- Name: signoff; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.signoff (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    target_type character varying(32),
    target_id bigint,
    target_version character varying(64),
    status character varying(24),
    signed_by character varying(128),
    signer_role character varying(64),
    signed_at character varying(40),
    signature_type character varying(32),
    signature_data character varying(255),
    content_hash character varying(128),
    attestation_text character varying(255),
    comments character varying(255),
    printed_at timestamp without time zone,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.signoff OWNER TO postgres;

--
-- Name: signoff_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.signoff_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.signoff_id_seq OWNER TO postgres;

--
-- Name: signoff_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.signoff_id_seq OWNED BY practice_1.signoff.id;


--
-- Name: slots; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.slots (
    id bigint NOT NULL,
    org_id bigint,
    provider_id bigint,
    external_id character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    createddate character varying(255),
    lastmodifieddate character varying(255)
);


ALTER TABLE practice_1.slots OWNER TO postgres;

--
-- Name: slots_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.slots_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.slots_id_seq OWNER TO postgres;

--
-- Name: slots_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.slots_id_seq OWNED BY practice_1.slots.id;


--
-- Name: social_history; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.social_history (
    id bigint NOT NULL,
    external_id character varying(255),
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    entries character varying(255),
    e_signed boolean,
    signed_at character varying(255),
    signed_by character varying(128),
    printed_at character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.social_history OWNER TO postgres;

--
-- Name: social_history_entry; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.social_history_entry (
    id bigint NOT NULL,
    category character varying(48),
    details character varying(2000),
    value character varying(255),
    social_history character varying(255),
    social_history_id bigint NOT NULL
);


ALTER TABLE practice_1.social_history_entry OWNER TO postgres;

--
-- Name: social_history_entry_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.social_history_entry_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.social_history_entry_id_seq OWNER TO postgres;

--
-- Name: social_history_entry_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.social_history_entry_id_seq OWNED BY practice_1.social_history_entry.id;


--
-- Name: social_history_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.social_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.social_history_id_seq OWNER TO postgres;

--
-- Name: social_history_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.social_history_id_seq OWNED BY practice_1.social_history.id;


--
-- Name: subscriptions; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.subscriptions (
    id bigint NOT NULL,
    org_id bigint,
    user_id bigint,
    service character varying(255),
    billing_cycle character varying(255),
    scope character varying(255),
    status character varying(255),
    start_date character varying(255),
    price character varying(255),
    billingcycle character varying(255),
    orgid bigint,
    startdate character varying(255),
    userid bigint
);


ALTER TABLE practice_1.subscriptions OWNER TO postgres;

--
-- Name: subscriptions_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.subscriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.subscriptions_id_seq OWNER TO postgres;

--
-- Name: subscriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.subscriptions_id_seq OWNED BY practice_1.subscriptions.id;


--
-- Name: supplier; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.supplier (
    id bigint NOT NULL,
    org_id bigint,
    name character varying(255),
    contact character varying(255),
    phone character varying(255),
    email character varying(255),
    created_date character varying(255),
    last_modified_date character varying(255),
    external_id character varying(255),
    createddate character varying(255),
    externalid character varying(255),
    lastmodifieddate character varying(255),
    orgid bigint
);


ALTER TABLE practice_1.supplier OWNER TO postgres;

--
-- Name: supplier_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.supplier_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.supplier_id_seq OWNER TO postgres;

--
-- Name: supplier_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.supplier_id_seq OWNED BY practice_1.supplier.id;


--
-- Name: templates; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.templates (
    id bigint NOT NULL,
    org_id bigint,
    external_id character varying(255),
    template_name character varying(255),
    subject character varying(255),
    body character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE practice_1.templates OWNER TO postgres;

--
-- Name: templates_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.templates_id_seq OWNER TO postgres;

--
-- Name: templates_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.templates_id_seq OWNED BY practice_1.templates.id;


--
-- Name: vitals; Type: TABLE; Schema: practice_1; Owner: postgres
--

CREATE TABLE practice_1.vitals (
    id bigint NOT NULL,
    org_id bigint,
    patient_id bigint,
    encounter_id bigint,
    weight_kg character varying(255),
    weight_lbs character varying(255),
    height_cm character varying(255),
    height_in character varying(255),
    bp_systolic character varying(255),
    bp_diastolic character varying(255),
    pulse character varying(255),
    respiration character varying(255),
    temperature_c character varying(255),
    temperature_f character varying(255),
    oxygen_saturation character varying(255),
    bmi character varying(255),
    notes character varying(255),
    signed boolean,
    recorded_at timestamp without time zone,
    created_date timestamp without time zone,
    last_modified_date timestamp without time zone,
    bpdiastolic double precision,
    bpsystolic double precision,
    createddate timestamp(6) without time zone,
    encounterid bigint,
    heightcm double precision,
    heightin double precision,
    lastmodifieddate timestamp(6) without time zone,
    orgid bigint,
    oxygensaturation double precision,
    patientid bigint,
    recordedat timestamp(6) without time zone,
    temperaturec double precision,
    temperaturef double precision,
    weightkg double precision,
    weightlbs double precision
);


ALTER TABLE practice_1.vitals OWNER TO postgres;

--
-- Name: vitals_id_seq; Type: SEQUENCE; Schema: practice_1; Owner: postgres
--

CREATE SEQUENCE practice_1.vitals_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE practice_1.vitals_id_seq OWNER TO postgres;

--
-- Name: vitals_id_seq; Type: SEQUENCE OWNED BY; Schema: practice_1; Owner: postgres
--

ALTER SEQUENCE practice_1.vitals_id_seq OWNED BY practice_1.vitals.id;


--
-- Name: allergy_details id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.allergy_details ALTER COLUMN id SET DEFAULT nextval('practice_1.allergy_details_id_seq'::regclass);


--
-- Name: allergy_intolerances id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.allergy_intolerances ALTER COLUMN id SET DEFAULT nextval('practice_1.allergy_intolerances_id_seq'::regclass);


--
-- Name: appointments id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.appointments ALTER COLUMN id SET DEFAULT nextval('practice_1.appointments_id_seq'::regclass);


--
-- Name: assessment id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.assessment ALTER COLUMN id SET DEFAULT nextval('practice_1.assessment_id_seq'::regclass);


--
-- Name: assigned_providers id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.assigned_providers ALTER COLUMN id SET DEFAULT nextval('practice_1.assigned_providers_id_seq'::regclass);


--
-- Name: audit_log id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.audit_log ALTER COLUMN id SET DEFAULT nextval('practice_1.audit_log_id_seq'::regclass);


--
-- Name: billing_autopay id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.billing_autopay ALTER COLUMN id SET DEFAULT nextval('practice_1.billing_autopay_id_seq'::regclass);


--
-- Name: billing_cards id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.billing_cards ALTER COLUMN id SET DEFAULT nextval('practice_1.billing_cards_id_seq'::regclass);


--
-- Name: billing_history id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.billing_history ALTER COLUMN id SET DEFAULT nextval('practice_1.billing_history_id_seq'::regclass);


--
-- Name: chief_complaint id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.chief_complaint ALTER COLUMN id SET DEFAULT nextval('practice_1.chief_complaint_id_seq'::regclass);


--
-- Name: code_types id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.code_types ALTER COLUMN id SET DEFAULT nextval('practice_1.code_types_id_seq'::regclass);


--
-- Name: codes id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.codes ALTER COLUMN id SET DEFAULT nextval('practice_1.codes_id_seq'::regclass);


--
-- Name: communications id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.communications ALTER COLUMN id SET DEFAULT nextval('practice_1.communications_id_seq'::regclass);


--
-- Name: coverages id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.coverages ALTER COLUMN id SET DEFAULT nextval('practice_1.coverages_id_seq'::regclass);


--
-- Name: date_time_finalized id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.date_time_finalized ALTER COLUMN id SET DEFAULT nextval('practice_1.date_time_finalized_id_seq'::regclass);


--
-- Name: document id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.document ALTER COLUMN id SET DEFAULT nextval('practice_1.document_id_seq'::regclass);


--
-- Name: document_settings id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.document_settings ALTER COLUMN id SET DEFAULT nextval('practice_1.document_settings_id_seq'::regclass);


--
-- Name: enc_fee_schedule_entries id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.enc_fee_schedule_entries ALTER COLUMN id SET DEFAULT nextval('practice_1.enc_fee_schedule_entries_id_seq'::regclass);


--
-- Name: enc_fee_schedules id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.enc_fee_schedules ALTER COLUMN id SET DEFAULT nextval('practice_1.enc_fee_schedules_id_seq'::regclass);


--
-- Name: encounter id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.encounter ALTER COLUMN id SET DEFAULT nextval('practice_1.encounter_id_seq'::regclass);


--
-- Name: family_history id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.family_history ALTER COLUMN id SET DEFAULT nextval('practice_1.family_history_id_seq'::regclass);


--
-- Name: family_history_entry id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.family_history_entry ALTER COLUMN id SET DEFAULT nextval('practice_1.family_history_entry_id_seq'::regclass);


--
-- Name: healthcare_service id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.healthcare_service ALTER COLUMN id SET DEFAULT nextval('practice_1.healthcare_service_id_seq'::regclass);


--
-- Name: history_of_present_illness id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.history_of_present_illness ALTER COLUMN id SET DEFAULT nextval('practice_1.history_of_present_illness_id_seq'::regclass);


--
-- Name: immunizations id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.immunizations ALTER COLUMN id SET DEFAULT nextval('practice_1.immunizations_id_seq'::regclass);


--
-- Name: insurance_companies id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.insurance_companies ALTER COLUMN id SET DEFAULT nextval('practice_1.insurance_companies_id_seq'::regclass);


--
-- Name: inventory id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.inventory ALTER COLUMN id SET DEFAULT nextval('practice_1.inventory_id_seq'::regclass);


--
-- Name: inventory_settings id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.inventory_settings ALTER COLUMN id SET DEFAULT nextval('practice_1.inventory_settings_id_seq'::regclass);


--
-- Name: invoice id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice ALTER COLUMN id SET DEFAULT nextval('practice_1.invoice_id_seq'::regclass);


--
-- Name: invoice_bills id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice_bills ALTER COLUMN id SET DEFAULT nextval('practice_1.invoice_bills_id_seq'::regclass);


--
-- Name: invoice_line id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice_line ALTER COLUMN id SET DEFAULT nextval('practice_1.invoice_line_id_seq'::regclass);


--
-- Name: invoice_payment id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice_payment ALTER COLUMN id SET DEFAULT nextval('practice_1.invoice_payment_id_seq'::regclass);


--
-- Name: lab_orders id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.lab_orders ALTER COLUMN id SET DEFAULT nextval('practice_1.lab_orders_id_seq'::regclass);


--
-- Name: list_options id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.list_options ALTER COLUMN id SET DEFAULT nextval('practice_1.list_options_id_seq'::regclass);


--
-- Name: locations id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.locations ALTER COLUMN id SET DEFAULT nextval('practice_1.locations_id_seq'::regclass);


--
-- Name: maintenance id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.maintenance ALTER COLUMN id SET DEFAULT nextval('practice_1.maintenance_id_seq'::regclass);


--
-- Name: medical_problems id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.medical_problems ALTER COLUMN id SET DEFAULT nextval('practice_1.medical_problems_id_seq'::regclass);


--
-- Name: medication_requests id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.medication_requests ALTER COLUMN id SET DEFAULT nextval('practice_1.medication_requests_id_seq'::regclass);


--
-- Name: orders id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.orders ALTER COLUMN id SET DEFAULT nextval('practice_1.orders_id_seq'::regclass);


--
-- Name: org_config id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.org_config ALTER COLUMN id SET DEFAULT nextval('practice_1.org_config_id_seq'::regclass);


--
-- Name: past_medical_history id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.past_medical_history ALTER COLUMN id SET DEFAULT nextval('practice_1.past_medical_history_id_seq'::regclass);


--
-- Name: patient_education id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patient_education ALTER COLUMN id SET DEFAULT nextval('practice_1.patient_education_id_seq'::regclass);


--
-- Name: patient_education_assignment id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patient_education_assignment ALTER COLUMN id SET DEFAULT nextval('practice_1.patient_education_assignment_id_seq'::regclass);


--
-- Name: patient_medical_history id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patient_medical_history ALTER COLUMN id SET DEFAULT nextval('practice_1.patient_medical_history_id_seq'::regclass);


--
-- Name: patients id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patients ALTER COLUMN id SET DEFAULT nextval('practice_1.patients_id_seq'::regclass);


--
-- Name: payment_orders id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.payment_orders ALTER COLUMN id SET DEFAULT nextval('practice_1.payment_orders_id_seq'::regclass);


--
-- Name: physical_exam id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.physical_exam ALTER COLUMN id SET DEFAULT nextval('practice_1.physical_exam_id_seq'::regclass);


--
-- Name: physical_exam_section id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.physical_exam_section ALTER COLUMN id SET DEFAULT nextval('practice_1.physical_exam_section_id_seq'::regclass);


--
-- Name: plan id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.plan ALTER COLUMN id SET DEFAULT nextval('practice_1.plan_id_seq'::regclass);


--
-- Name: portal_demographics id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_demographics ALTER COLUMN id SET DEFAULT nextval('practice_1.portal_demographics_id_seq'::regclass);


--
-- Name: portal_patients id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_patients ALTER COLUMN id SET DEFAULT nextval('practice_1.portal_patients_id_seq'::regclass);


--
-- Name: portal_profiles id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_profiles ALTER COLUMN id SET DEFAULT nextval('practice_1.portal_profiles_id_seq'::regclass);


--
-- Name: portal_users id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_users ALTER COLUMN id SET DEFAULT nextval('practice_1.portal_users_id_seq'::regclass);


--
-- Name: practitioner_role id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.practitioner_role ALTER COLUMN id SET DEFAULT nextval('practice_1.practitioner_role_id_seq'::regclass);


--
-- Name: procedure_item id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.procedure_item ALTER COLUMN id SET DEFAULT nextval('practice_1.procedure_item_id_seq'::regclass);


--
-- Name: provider id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.provider ALTER COLUMN id SET DEFAULT nextval('practice_1.provider_id_seq'::regclass);


--
-- Name: provider_note id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.provider_note ALTER COLUMN id SET DEFAULT nextval('practice_1.provider_note_id_seq'::regclass);


--
-- Name: provider_signatures id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.provider_signatures ALTER COLUMN id SET DEFAULT nextval('practice_1.provider_signatures_id_seq'::regclass);


--
-- Name: recall id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.recall ALTER COLUMN id SET DEFAULT nextval('practice_1.recall_id_seq'::regclass);


--
-- Name: referral_practices id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.referral_practices ALTER COLUMN id SET DEFAULT nextval('practice_1.referral_practices_id_seq'::regclass);


--
-- Name: referral_providers id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.referral_providers ALTER COLUMN id SET DEFAULT nextval('practice_1.referral_providers_id_seq'::regclass);


--
-- Name: review_of_systems id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.review_of_systems ALTER COLUMN id SET DEFAULT nextval('practice_1.review_of_systems_id_seq'::regclass);


--
-- Name: schedules id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.schedules ALTER COLUMN id SET DEFAULT nextval('practice_1.schedules_id_seq'::regclass);


--
-- Name: services id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.services ALTER COLUMN id SET DEFAULT nextval('practice_1.services_id_seq'::regclass);


--
-- Name: signoff id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.signoff ALTER COLUMN id SET DEFAULT nextval('practice_1.signoff_id_seq'::regclass);


--
-- Name: slots id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.slots ALTER COLUMN id SET DEFAULT nextval('practice_1.slots_id_seq'::regclass);


--
-- Name: social_history id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.social_history ALTER COLUMN id SET DEFAULT nextval('practice_1.social_history_id_seq'::regclass);


--
-- Name: social_history_entry id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.social_history_entry ALTER COLUMN id SET DEFAULT nextval('practice_1.social_history_entry_id_seq'::regclass);


--
-- Name: subscriptions id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.subscriptions ALTER COLUMN id SET DEFAULT nextval('practice_1.subscriptions_id_seq'::regclass);


--
-- Name: supplier id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.supplier ALTER COLUMN id SET DEFAULT nextval('practice_1.supplier_id_seq'::regclass);


--
-- Name: templates id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.templates ALTER COLUMN id SET DEFAULT nextval('practice_1.templates_id_seq'::regclass);


--
-- Name: vitals id; Type: DEFAULT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.vitals ALTER COLUMN id SET DEFAULT nextval('practice_1.vitals_id_seq'::regclass);


--
-- Name: allergy_details allergy_details_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.allergy_details
    ADD CONSTRAINT allergy_details_pkey PRIMARY KEY (id);


--
-- Name: allergy_intolerances allergy_intolerances_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.allergy_intolerances
    ADD CONSTRAINT allergy_intolerances_pkey PRIMARY KEY (id);


--
-- Name: appointments appointments_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.appointments
    ADD CONSTRAINT appointments_pkey PRIMARY KEY (id);


--
-- Name: assessment assessment_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.assessment
    ADD CONSTRAINT assessment_pkey PRIMARY KEY (id);


--
-- Name: assigned_providers assigned_providers_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.assigned_providers
    ADD CONSTRAINT assigned_providers_pkey PRIMARY KEY (id);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- Name: billing_autopay billing_autopay_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.billing_autopay
    ADD CONSTRAINT billing_autopay_pkey PRIMARY KEY (id);


--
-- Name: billing_cards billing_cards_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.billing_cards
    ADD CONSTRAINT billing_cards_pkey PRIMARY KEY (id);


--
-- Name: billing_history billing_history_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.billing_history
    ADD CONSTRAINT billing_history_pkey PRIMARY KEY (id);


--
-- Name: chief_complaint chief_complaint_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.chief_complaint
    ADD CONSTRAINT chief_complaint_pkey PRIMARY KEY (id);


--
-- Name: code_types code_types_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.code_types
    ADD CONSTRAINT code_types_pkey PRIMARY KEY (id);


--
-- Name: codes codes_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.codes
    ADD CONSTRAINT codes_pkey PRIMARY KEY (id);


--
-- Name: communications communications_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.communications
    ADD CONSTRAINT communications_pkey PRIMARY KEY (id);


--
-- Name: coverages coverages_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.coverages
    ADD CONSTRAINT coverages_pkey PRIMARY KEY (id);


--
-- Name: date_time_finalized date_time_finalized_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.date_time_finalized
    ADD CONSTRAINT date_time_finalized_pkey PRIMARY KEY (id);


--
-- Name: document document_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.document
    ADD CONSTRAINT document_pkey PRIMARY KEY (id);


--
-- Name: document_settings document_settings_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.document_settings
    ADD CONSTRAINT document_settings_pkey PRIMARY KEY (id);


--
-- Name: enc_fee_schedule_entries enc_fee_schedule_entries_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.enc_fee_schedule_entries
    ADD CONSTRAINT enc_fee_schedule_entries_pkey PRIMARY KEY (id);


--
-- Name: enc_fee_schedules enc_fee_schedules_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.enc_fee_schedules
    ADD CONSTRAINT enc_fee_schedules_pkey PRIMARY KEY (id);


--
-- Name: encounter encounter_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.encounter
    ADD CONSTRAINT encounter_pkey PRIMARY KEY (id);


--
-- Name: family_history_entry family_history_entry_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.family_history_entry
    ADD CONSTRAINT family_history_entry_pkey PRIMARY KEY (id);


--
-- Name: family_history family_history_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.family_history
    ADD CONSTRAINT family_history_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: healthcare_service healthcare_service_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.healthcare_service
    ADD CONSTRAINT healthcare_service_pkey PRIMARY KEY (id);


--
-- Name: healthcareservice healthcareservice_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.healthcareservice
    ADD CONSTRAINT healthcareservice_pkey PRIMARY KEY (id);


--
-- Name: history_of_present_illness history_of_present_illness_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.history_of_present_illness
    ADD CONSTRAINT history_of_present_illness_pkey PRIMARY KEY (id);


--
-- Name: immunizations immunizations_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.immunizations
    ADD CONSTRAINT immunizations_pkey PRIMARY KEY (id);


--
-- Name: insurance_companies insurance_companies_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.insurance_companies
    ADD CONSTRAINT insurance_companies_pkey PRIMARY KEY (id);


--
-- Name: inventory inventory_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.inventory
    ADD CONSTRAINT inventory_pkey PRIMARY KEY (id);


--
-- Name: inventory_settings inventory_settings_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.inventory_settings
    ADD CONSTRAINT inventory_settings_pkey PRIMARY KEY (id);


--
-- Name: inventorysettings inventorysettings_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.inventorysettings
    ADD CONSTRAINT inventorysettings_pkey PRIMARY KEY (id);


--
-- Name: invoice_bills invoice_bills_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice_bills
    ADD CONSTRAINT invoice_bills_pkey PRIMARY KEY (id);


--
-- Name: invoice_line invoice_line_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice_line
    ADD CONSTRAINT invoice_line_pkey PRIMARY KEY (id);


--
-- Name: invoice_payment invoice_payment_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice_payment
    ADD CONSTRAINT invoice_payment_pkey PRIMARY KEY (id);


--
-- Name: invoice invoice_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice
    ADD CONSTRAINT invoice_pkey PRIMARY KEY (id);


--
-- Name: lab_orders lab_orders_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.lab_orders
    ADD CONSTRAINT lab_orders_pkey PRIMARY KEY (id);


--
-- Name: list_options list_options_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.list_options
    ADD CONSTRAINT list_options_pkey PRIMARY KEY (id);


--
-- Name: locations locations_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.locations
    ADD CONSTRAINT locations_pkey PRIMARY KEY (id);


--
-- Name: maintenance maintenance_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.maintenance
    ADD CONSTRAINT maintenance_pkey PRIMARY KEY (id);


--
-- Name: medical_problems medical_problems_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.medical_problems
    ADD CONSTRAINT medical_problems_pkey PRIMARY KEY (id);


--
-- Name: medication_requests medication_requests_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.medication_requests
    ADD CONSTRAINT medication_requests_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: org_config org_config_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.org_config
    ADD CONSTRAINT org_config_pkey PRIMARY KEY (id);


--
-- Name: past_medical_history past_medical_history_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.past_medical_history
    ADD CONSTRAINT past_medical_history_pkey PRIMARY KEY (id);


--
-- Name: patient_education_assignment patient_education_assignment_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patient_education_assignment
    ADD CONSTRAINT patient_education_assignment_pkey PRIMARY KEY (id);


--
-- Name: patient_education patient_education_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patient_education
    ADD CONSTRAINT patient_education_pkey PRIMARY KEY (id);


--
-- Name: patient_medical_history patient_medical_history_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patient_medical_history
    ADD CONSTRAINT patient_medical_history_pkey PRIMARY KEY (id);


--
-- Name: patienteducation patienteducation_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patienteducation
    ADD CONSTRAINT patienteducation_pkey PRIMARY KEY (id);


--
-- Name: patienteducationassignment patienteducationassignment_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patienteducationassignment
    ADD CONSTRAINT patienteducationassignment_pkey PRIMARY KEY (id);


--
-- Name: patients patients_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patients
    ADD CONSTRAINT patients_pkey PRIMARY KEY (id);


--
-- Name: payment_orders payment_orders_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.payment_orders
    ADD CONSTRAINT payment_orders_pkey PRIMARY KEY (id);


--
-- Name: physical_exam physical_exam_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.physical_exam
    ADD CONSTRAINT physical_exam_pkey PRIMARY KEY (id);


--
-- Name: physical_exam_section physical_exam_section_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.physical_exam_section
    ADD CONSTRAINT physical_exam_section_pkey PRIMARY KEY (id);


--
-- Name: plan plan_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.plan
    ADD CONSTRAINT plan_pkey PRIMARY KEY (id);


--
-- Name: portal_demographics portal_demographics_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_demographics
    ADD CONSTRAINT portal_demographics_pkey PRIMARY KEY (id);


--
-- Name: portal_patients portal_patients_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_patients
    ADD CONSTRAINT portal_patients_pkey PRIMARY KEY (id);


--
-- Name: portal_profiles portal_profiles_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_profiles
    ADD CONSTRAINT portal_profiles_pkey PRIMARY KEY (id);


--
-- Name: portal_users portal_users_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_users
    ADD CONSTRAINT portal_users_pkey PRIMARY KEY (id);


--
-- Name: practitioner_role practitioner_role_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.practitioner_role
    ADD CONSTRAINT practitioner_role_pkey PRIMARY KEY (id);


--
-- Name: practitionerrole practitionerrole_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.practitionerrole
    ADD CONSTRAINT practitionerrole_pkey PRIMARY KEY (id);


--
-- Name: procedure_item procedure_item_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.procedure_item
    ADD CONSTRAINT procedure_item_pkey PRIMARY KEY (id);


--
-- Name: provider_note provider_note_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.provider_note
    ADD CONSTRAINT provider_note_pkey PRIMARY KEY (id);


--
-- Name: provider provider_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.provider
    ADD CONSTRAINT provider_pkey PRIMARY KEY (id);


--
-- Name: provider_signatures provider_signatures_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.provider_signatures
    ADD CONSTRAINT provider_signatures_pkey PRIMARY KEY (id);


--
-- Name: recall recall_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.recall
    ADD CONSTRAINT recall_pkey PRIMARY KEY (id);


--
-- Name: referral_practices referral_practices_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.referral_practices
    ADD CONSTRAINT referral_practices_pkey PRIMARY KEY (id);


--
-- Name: referral_providers referral_providers_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.referral_providers
    ADD CONSTRAINT referral_providers_pkey PRIMARY KEY (id);


--
-- Name: review_of_systems review_of_systems_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.review_of_systems
    ADD CONSTRAINT review_of_systems_pkey PRIMARY KEY (id);


--
-- Name: schedules schedules_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.schedules
    ADD CONSTRAINT schedules_pkey PRIMARY KEY (id);


--
-- Name: services services_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.services
    ADD CONSTRAINT services_pkey PRIMARY KEY (id);


--
-- Name: signoff signoff_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.signoff
    ADD CONSTRAINT signoff_pkey PRIMARY KEY (id);


--
-- Name: slots slots_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.slots
    ADD CONSTRAINT slots_pkey PRIMARY KEY (id);


--
-- Name: social_history_entry social_history_entry_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.social_history_entry
    ADD CONSTRAINT social_history_entry_pkey PRIMARY KEY (id);


--
-- Name: social_history social_history_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.social_history
    ADD CONSTRAINT social_history_pkey PRIMARY KEY (id);


--
-- Name: subscriptions subscriptions_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);


--
-- Name: supplier supplier_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.supplier
    ADD CONSTRAINT supplier_pkey PRIMARY KEY (id);


--
-- Name: templates templates_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.templates
    ADD CONSTRAINT templates_pkey PRIMARY KEY (id);


--
-- Name: portal_demographics ukfdxwi5j6b93i8kbgvpk6oee0n; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_demographics
    ADD CONSTRAINT ukfdxwi5j6b93i8kbgvpk6oee0n UNIQUE (patient_id);


--
-- Name: portal_patients uktl1loqspx4cgdbm35fs31q4cc; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_patients
    ADD CONSTRAINT uktl1loqspx4cgdbm35fs31q4cc UNIQUE (user_id);


--
-- Name: document_settings uq_document_settings_org; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.document_settings
    ADD CONSTRAINT uq_document_settings_org UNIQUE (org_id);


--
-- Name: vitals vitals_pkey; Type: CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.vitals
    ADD CONSTRAINT vitals_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON practice_1.flyway_schema_history USING btree (success);


--
-- Name: idx_audit_action_type; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_audit_action_type ON practice_1.audit_log USING btree (actiontype);


--
-- Name: idx_audit_entity_type; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_audit_entity_type ON practice_1.audit_log USING btree (entitytype);


--
-- Name: idx_audit_event_time; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_audit_event_time ON practice_1.audit_log USING btree (eventtime);


--
-- Name: idx_audit_ip_address; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_audit_ip_address ON practice_1.audit_log USING btree (ipaddress);


--
-- Name: idx_audit_patient_id; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_audit_patient_id ON practice_1.audit_log USING btree (patientid);


--
-- Name: idx_audit_session_id; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_audit_session_id ON practice_1.audit_log USING btree (sessionid);


--
-- Name: idx_audit_user_id; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_audit_user_id ON practice_1.audit_log USING btree (userid);


--
-- Name: idx_codetypes_key; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_codetypes_key ON practice_1.code_types USING btree (code_type_key, code_type_id);


--
-- Name: idx_codetypes_scope; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_codetypes_scope ON practice_1.code_types USING btree (org_id, patient_id, encounter_id);


--
-- Name: idx_efs_scope; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_efs_scope ON practice_1.enc_fee_schedules USING btree (org_id, patient_id, encounter_id);


--
-- Name: idx_efs_status; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_efs_status ON practice_1.enc_fee_schedules USING btree (org_id, status);


--
-- Name: idx_efse_code; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_efse_code ON practice_1.enc_fee_schedule_entries USING btree (code_type, code, modifier);


--
-- Name: idx_efse_sched; Type: INDEX; Schema: practice_1; Owner: postgres
--

CREATE INDEX idx_efse_sched ON practice_1.enc_fee_schedule_entries USING btree (schedule_id);


--
-- Name: social_history_entry fk1eijtmgnwamcwe0s3yautgldv; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.social_history_entry
    ADD CONSTRAINT fk1eijtmgnwamcwe0s3yautgldv FOREIGN KEY (social_history_id) REFERENCES practice_1.social_history(id);


--
-- Name: enc_fee_schedule_entries fk3qs7yb9qtswey5ia9yk4e6kog; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.enc_fee_schedule_entries
    ADD CONSTRAINT fk3qs7yb9qtswey5ia9yk4e6kog FOREIGN KEY (schedule_id) REFERENCES practice_1.enc_fee_schedules(id);


--
-- Name: coverages fk7ena3iyxg1qylpd0kn2e3wkrl; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.coverages
    ADD CONSTRAINT fk7ena3iyxg1qylpd0kn2e3wkrl FOREIGN KEY (insurance_company_id) REFERENCES practice_1.insurance_companies(id);


--
-- Name: portal_demographics fk9p3cf5vybdhv146un1tnm804b; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_demographics
    ADD CONSTRAINT fk9p3cf5vybdhv146un1tnm804b FOREIGN KEY (patient_id) REFERENCES practice_1.portal_patients(id);


--
-- Name: invoice_line fkfnwks1ouvwbttl0fklxsem7ik; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice_line
    ADD CONSTRAINT fkfnwks1ouvwbttl0fklxsem7ik FOREIGN KEY (invoice_id) REFERENCES practice_1.invoice(id);


--
-- Name: billing_history fkg5u7u6wme1fmtvovvp5nn6aq8; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.billing_history
    ADD CONSTRAINT fkg5u7u6wme1fmtvovvp5nn6aq8 FOREIGN KEY (invoice_bill_id) REFERENCES practice_1.invoice_bills(id);


--
-- Name: allergy_details fkh624duw8w9u0nglqxlv4w6srp; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.allergy_details
    ADD CONSTRAINT fkh624duw8w9u0nglqxlv4w6srp FOREIGN KEY (allergy_intolerance_id) REFERENCES practice_1.allergy_intolerances(id);


--
-- Name: portal_patients fkhw83cadlkqnudfpdtblopw9h; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.portal_patients
    ADD CONSTRAINT fkhw83cadlkqnudfpdtblopw9h FOREIGN KEY (user_id) REFERENCES practice_1.portal_users(id);


--
-- Name: review_of_system_details fki66q2igj3y3m8f5b873ea7fbk; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.review_of_system_details
    ADD CONSTRAINT fki66q2igj3y3m8f5b873ea7fbk FOREIGN KEY (ros_id) REFERENCES practice_1.review_of_systems(id);


--
-- Name: family_history_entry fkio46fqpdi3vnukh5jxptwxyo4; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.family_history_entry
    ADD CONSTRAINT fkio46fqpdi3vnukh5jxptwxyo4 FOREIGN KEY (family_history_id) REFERENCES practice_1.family_history(id);


--
-- Name: invoice_payment fkkopeu965ps1ljahtib8n8nub2; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.invoice_payment
    ADD CONSTRAINT fkkopeu965ps1ljahtib8n8nub2 FOREIGN KEY (invoice_id) REFERENCES practice_1.invoice(id);


--
-- Name: orders fkn6v1kjethk891xl3ow3a17qf7; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.orders
    ADD CONSTRAINT fkn6v1kjethk891xl3ow3a17qf7 FOREIGN KEY (inventory_id) REFERENCES practice_1.inventory(id);


--
-- Name: referral_providers fko01009pj9lx253p45h40627f7; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.referral_providers
    ADD CONSTRAINT fko01009pj9lx253p45h40627f7 FOREIGN KEY (practice_id) REFERENCES practice_1.referral_practices(id);


--
-- Name: patienteducationassignment fko9ltf1tbbsinvu144vfb3i43u; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.patienteducationassignment
    ADD CONSTRAINT fko9ltf1tbbsinvu144vfb3i43u FOREIGN KEY (education_id) REFERENCES practice_1.patienteducation(id);


--
-- Name: physical_exam_section fkpktdwn2xhyygcrfgmgcbfj1e1; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.physical_exam_section
    ADD CONSTRAINT fkpktdwn2xhyygcrfgmgcbfj1e1 FOREIGN KEY (physical_exam_id) REFERENCES practice_1.physical_exam(id);


--
-- Name: practitionerrole fksjq70mw4k1dbwtpljyvg4hat3; Type: FK CONSTRAINT; Schema: practice_1; Owner: postgres
--

ALTER TABLE ONLY practice_1.practitionerrole
    ADD CONSTRAINT fksjq70mw4k1dbwtpljyvg4hat3 FOREIGN KEY (location_id) REFERENCES practice_1.locations(id);


--
-- PostgreSQL database dump complete
--

