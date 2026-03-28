-- V30: Comprehensive Provider field configuration with FHIR Practitioner mappings + Availability
UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "personal-info",
      "title": "Personal Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "identification.prefix",
          "label": "Prefix",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "Dr.", "label": "Dr."},
            {"value": "Mr.", "label": "Mr."},
            {"value": "Mrs.", "label": "Mrs."},
            {"value": "Ms.", "label": "Ms."},
            {"value": "Prof.", "label": "Prof."}
          ],
          "fhirMapping": {"resource": "Practitioner", "path": "name[0].prefix[0]", "type": "string"}
        },
        {
          "key": "identification.firstName",
          "label": "First Name",
          "type": "text",
          "required": true,
          "colSpan": 1,
          "placeholder": "First name",
          "validation": {"maxLength": 100},
          "fhirMapping": {"resource": "Practitioner", "path": "name[0].given[0]", "type": "string"}
        },
        {
          "key": "identification.middleName",
          "label": "Middle Name",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "Middle name",
          "fhirMapping": {"resource": "Practitioner", "path": "name[0].given[1]", "type": "string"}
        },
        {
          "key": "identification.lastName",
          "label": "Last Name",
          "type": "text",
          "required": true,
          "colSpan": 1,
          "placeholder": "Last name",
          "validation": {"maxLength": 100},
          "fhirMapping": {"resource": "Practitioner", "path": "name[0].family", "type": "string"}
        },
        {
          "key": "identification.suffix",
          "label": "Suffix",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "MD", "label": "MD"},
            {"value": "DO", "label": "DO"},
            {"value": "PhD", "label": "PhD"},
            {"value": "DDS", "label": "DDS"},
            {"value": "DMD", "label": "DMD"},
            {"value": "OD", "label": "OD"},
            {"value": "DPM", "label": "DPM"},
            {"value": "PA", "label": "PA"},
            {"value": "NP", "label": "NP"},
            {"value": "RN", "label": "RN"},
            {"value": "Jr.", "label": "Jr."},
            {"value": "Sr.", "label": "Sr."},
            {"value": "II", "label": "II"},
            {"value": "III", "label": "III"}
          ],
          "fhirMapping": {"resource": "Practitioner", "path": "name[0].suffix[0]", "type": "string"}
        },
        {
          "key": "identification.gender",
          "label": "Gender",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "male", "label": "Male"},
            {"value": "female", "label": "Female"},
            {"value": "other", "label": "Other"},
            {"value": "unknown", "label": "Unknown"}
          ],
          "fhirMapping": {"resource": "Practitioner", "path": "gender", "type": "code"}
        },
        {
          "key": "identification.dateOfBirth",
          "label": "Date of Birth",
          "type": "date",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Practitioner", "path": "birthDate", "type": "date"}
        },
        {
          "key": "identification.photo",
          "label": "Photo",
          "type": "file",
          "required": false,
          "colSpan": 1,
          "fileConfig": {"accept": "image/*", "maxSizeMB": 5},
          "fhirMapping": {"resource": "Practitioner", "path": "photo[0].url", "type": "string"}
        },
        {
          "key": "npi",
          "label": "NPI Number",
          "type": "text",
          "required": true,
          "colSpan": 1,
          "placeholder": "10-digit NPI",
          "helpText": "National Provider Identifier — required for all providers",
          "validation": {"pattern": "^[0-9]{10}$", "maxLength": 10},
          "fhirMapping": {"resource": "Practitioner", "path": "identifier[0].value", "type": "string", "system": "http://hl7.org/fhir/sid/us-npi"}
        }
      ]
    },
    {
      "key": "contact-info",
      "title": "Contact Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "contact.email",
          "label": "Email",
          "type": "email",
          "required": false,
          "colSpan": 1,
          "placeholder": "provider@example.com",
          "fhirMapping": {"resource": "Practitioner", "path": "telecom[3].value", "type": "string", "system": "email"}
        },
        {
          "key": "contact.phoneNumber",
          "label": "Work Phone",
          "type": "phone",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4567",
          "fhirMapping": {"resource": "Practitioner", "path": "telecom[0].value", "type": "string", "system": "phone"}
        },
        {
          "key": "contact.mobileNumber",
          "label": "Mobile Phone",
          "type": "phone",
          "required": true,
          "colSpan": 1,
          "placeholder": "(555) 987-6543",
          "fhirMapping": {"resource": "Practitioner", "path": "telecom[1].value", "type": "string", "system": "phone"}
        },
        {
          "key": "contact.faxNumber",
          "label": "Fax Number",
          "type": "phone",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 111-2222",
          "fhirMapping": {"resource": "Practitioner", "path": "telecom[2].value", "type": "string", "system": "fax"}
        },
        {
          "key": "contact.address.street",
          "label": "Street Address",
          "type": "text",
          "required": false,
          "colSpan": 2,
          "placeholder": "123 Main St, Suite 100",
          "fhirMapping": {"resource": "Practitioner", "path": "address[0].line[0]", "type": "string"}
        },
        {
          "key": "contact.address.city",
          "label": "City",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "City",
          "fhirMapping": {"resource": "Practitioner", "path": "address[0].city", "type": "string"}
        },
        {
          "key": "contact.address.state",
          "label": "State",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "AL", "label": "Alabama"}, {"value": "AK", "label": "Alaska"},
            {"value": "AZ", "label": "Arizona"}, {"value": "AR", "label": "Arkansas"},
            {"value": "CA", "label": "California"}, {"value": "CO", "label": "Colorado"},
            {"value": "CT", "label": "Connecticut"}, {"value": "DE", "label": "Delaware"},
            {"value": "FL", "label": "Florida"}, {"value": "GA", "label": "Georgia"},
            {"value": "HI", "label": "Hawaii"}, {"value": "ID", "label": "Idaho"},
            {"value": "IL", "label": "Illinois"}, {"value": "IN", "label": "Indiana"},
            {"value": "IA", "label": "Iowa"}, {"value": "KS", "label": "Kansas"},
            {"value": "KY", "label": "Kentucky"}, {"value": "LA", "label": "Louisiana"},
            {"value": "ME", "label": "Maine"}, {"value": "MD", "label": "Maryland"},
            {"value": "MA", "label": "Massachusetts"}, {"value": "MI", "label": "Michigan"},
            {"value": "MN", "label": "Minnesota"}, {"value": "MS", "label": "Mississippi"},
            {"value": "MO", "label": "Missouri"}, {"value": "MT", "label": "Montana"},
            {"value": "NE", "label": "Nebraska"}, {"value": "NV", "label": "Nevada"},
            {"value": "NH", "label": "New Hampshire"}, {"value": "NJ", "label": "New Jersey"},
            {"value": "NM", "label": "New Mexico"}, {"value": "NY", "label": "New York"},
            {"value": "NC", "label": "North Carolina"}, {"value": "ND", "label": "North Dakota"},
            {"value": "OH", "label": "Ohio"}, {"value": "OK", "label": "Oklahoma"},
            {"value": "OR", "label": "Oregon"}, {"value": "PA", "label": "Pennsylvania"},
            {"value": "RI", "label": "Rhode Island"}, {"value": "SC", "label": "South Carolina"},
            {"value": "SD", "label": "South Dakota"}, {"value": "TN", "label": "Tennessee"},
            {"value": "TX", "label": "Texas"}, {"value": "UT", "label": "Utah"},
            {"value": "VT", "label": "Vermont"}, {"value": "VA", "label": "Virginia"},
            {"value": "WA", "label": "Washington"}, {"value": "WV", "label": "West Virginia"},
            {"value": "WI", "label": "Wisconsin"}, {"value": "WY", "label": "Wyoming"},
            {"value": "DC", "label": "District of Columbia"}
          ],
          "fhirMapping": {"resource": "Practitioner", "path": "address[0].state", "type": "string"}
        },
        {
          "key": "contact.address.postalCode",
          "label": "Zip Code",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "12345",
          "validation": {"pattern": "^[0-9]{5}(-[0-9]{4})?$", "maxLength": 10},
          "fhirMapping": {"resource": "Practitioner", "path": "address[0].postalCode", "type": "string"}
        }
      ]
    },
    {
      "key": "professional-details",
      "title": "Professional Details",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "professionalDetails.providerType",
          "label": "Provider Type",
          "type": "select",
          "required": true,
          "colSpan": 1,
          "options": [
            {"value": "physician", "label": "Physician (MD/DO)"},
            {"value": "nurse_practitioner", "label": "Nurse Practitioner (NP)"},
            {"value": "physician_assistant", "label": "Physician Assistant (PA)"},
            {"value": "dentist", "label": "Dentist (DDS/DMD)"},
            {"value": "optometrist", "label": "Optometrist (OD)"},
            {"value": "podiatrist", "label": "Podiatrist (DPM)"},
            {"value": "psychologist", "label": "Psychologist (PhD/PsyD)"},
            {"value": "therapist", "label": "Therapist (LCSW/LPC)"},
            {"value": "chiropractor", "label": "Chiropractor (DC)"},
            {"value": "pharmacist", "label": "Pharmacist (PharmD)"},
            {"value": "registered_nurse", "label": "Registered Nurse (RN)"},
            {"value": "other", "label": "Other"}
          ],
          "fhirMapping": {"resource": "Practitioner", "path": "qualification[0].code.coding[0].code", "type": "code"}
        },
        {
          "key": "professionalDetails.specialty",
          "label": "Primary Specialty",
          "type": "select",
          "required": true,
          "colSpan": 1,
          "options": [
            {"value": "general_practice", "label": "General Practice"},
            {"value": "family_medicine", "label": "Family Medicine"},
            {"value": "internal_medicine", "label": "Internal Medicine"},
            {"value": "pediatrics", "label": "Pediatrics"},
            {"value": "cardiology", "label": "Cardiology"},
            {"value": "dermatology", "label": "Dermatology"},
            {"value": "endocrinology", "label": "Endocrinology"},
            {"value": "gastroenterology", "label": "Gastroenterology"},
            {"value": "neurology", "label": "Neurology"},
            {"value": "obstetrics_gynecology", "label": "Obstetrics & Gynecology"},
            {"value": "oncology", "label": "Oncology"},
            {"value": "ophthalmology", "label": "Ophthalmology"},
            {"value": "orthopedics", "label": "Orthopedics"},
            {"value": "otolaryngology", "label": "Otolaryngology (ENT)"},
            {"value": "psychiatry", "label": "Psychiatry"},
            {"value": "pulmonology", "label": "Pulmonology"},
            {"value": "radiology", "label": "Radiology"},
            {"value": "rheumatology", "label": "Rheumatology"},
            {"value": "surgery_general", "label": "General Surgery"},
            {"value": "urology", "label": "Urology"},
            {"value": "emergency_medicine", "label": "Emergency Medicine"},
            {"value": "anesthesiology", "label": "Anesthesiology"},
            {"value": "pathology", "label": "Pathology"},
            {"value": "physical_medicine", "label": "Physical Medicine & Rehab"},
            {"value": "dental_general", "label": "General Dentistry"},
            {"value": "orthodontics", "label": "Orthodontics"},
            {"value": "oral_surgery", "label": "Oral & Maxillofacial Surgery"},
            {"value": "other", "label": "Other"}
          ],
          "fhirMapping": {"resource": "Practitioner", "path": "qualification[0].code.text", "type": "string"}
        },
        {
          "key": "professionalDetails.taxonomyCode",
          "label": "Taxonomy Code",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. 207Q00000X",
          "helpText": "Healthcare Provider Taxonomy Code (NUCC)",
          "validation": {"maxLength": 20},
          "fhirMapping": {"resource": "Practitioner", "path": "qualification[0].code.coding[0].code", "type": "code", "system": "http://nucc.org/provider-taxonomy"}
        },
        {
          "key": "professionalDetails.licenseNumber",
          "label": "License Number",
          "type": "text",
          "required": true,
          "colSpan": 1,
          "placeholder": "State license #",
          "fhirMapping": {"resource": "Practitioner", "path": "identifier[1].value", "type": "string", "system": "urn:ciyex:provider:license"}
        },
        {
          "key": "professionalDetails.licenseState",
          "label": "License State",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "AL", "label": "Alabama"}, {"value": "AK", "label": "Alaska"},
            {"value": "AZ", "label": "Arizona"}, {"value": "AR", "label": "Arkansas"},
            {"value": "CA", "label": "California"}, {"value": "CO", "label": "Colorado"},
            {"value": "CT", "label": "Connecticut"}, {"value": "DE", "label": "Delaware"},
            {"value": "FL", "label": "Florida"}, {"value": "GA", "label": "Georgia"},
            {"value": "HI", "label": "Hawaii"}, {"value": "ID", "label": "Idaho"},
            {"value": "IL", "label": "Illinois"}, {"value": "IN", "label": "Indiana"},
            {"value": "IA", "label": "Iowa"}, {"value": "KS", "label": "Kansas"},
            {"value": "KY", "label": "Kentucky"}, {"value": "LA", "label": "Louisiana"},
            {"value": "ME", "label": "Maine"}, {"value": "MD", "label": "Maryland"},
            {"value": "MA", "label": "Massachusetts"}, {"value": "MI", "label": "Michigan"},
            {"value": "MN", "label": "Minnesota"}, {"value": "MS", "label": "Mississippi"},
            {"value": "MO", "label": "Missouri"}, {"value": "MT", "label": "Montana"},
            {"value": "NE", "label": "Nebraska"}, {"value": "NV", "label": "Nevada"},
            {"value": "NH", "label": "New Hampshire"}, {"value": "NJ", "label": "New Jersey"},
            {"value": "NM", "label": "New Mexico"}, {"value": "NY", "label": "New York"},
            {"value": "NC", "label": "North Carolina"}, {"value": "ND", "label": "North Dakota"},
            {"value": "OH", "label": "Ohio"}, {"value": "OK", "label": "Oklahoma"},
            {"value": "OR", "label": "Oregon"}, {"value": "PA", "label": "Pennsylvania"},
            {"value": "RI", "label": "Rhode Island"}, {"value": "SC", "label": "South Carolina"},
            {"value": "SD", "label": "South Dakota"}, {"value": "TN", "label": "Tennessee"},
            {"value": "TX", "label": "Texas"}, {"value": "UT", "label": "Utah"},
            {"value": "VT", "label": "Vermont"}, {"value": "VA", "label": "Virginia"},
            {"value": "WA", "label": "Washington"}, {"value": "WV", "label": "West Virginia"},
            {"value": "WI", "label": "Wisconsin"}, {"value": "WY", "label": "Wyoming"},
            {"value": "DC", "label": "District of Columbia"}
          ],
          "fhirMapping": {"resource": "Practitioner", "path": "qualification[0].issuer.display", "type": "string"}
        },
        {
          "key": "professionalDetails.licenseExpiry",
          "label": "License Expiry Date",
          "type": "date",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Practitioner", "path": "qualification[0].period.end", "type": "date"}
        },
        {
          "key": "professionalDetails.deaNumber",
          "label": "DEA Number",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "DEA registration #",
          "helpText": "Drug Enforcement Administration registration number",
          "validation": {"pattern": "^[A-Z]{2}[0-9]{7}$", "maxLength": 9},
          "fhirMapping": {"resource": "Practitioner", "path": "identifier[2].value", "type": "string", "system": "urn:oid:2.16.840.1.113883.4.814"}
        },
        {
          "key": "professionalDetails.upin",
          "label": "UPIN",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "Unique Physician ID",
          "helpText": "Unique Physician Identification Number (legacy)",
          "validation": {"maxLength": 10},
          "fhirMapping": {"resource": "Practitioner", "path": "identifier[3].value", "type": "string", "system": "http://hl7.org/fhir/sid/us-upin"}
        }
      ]
    },
    {
      "key": "credentials",
      "title": "Credentials & Education",
      "columns": 2,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "credentials.educationDegrees",
          "label": "Education & Degrees",
          "type": "textarea",
          "required": false,
          "colSpan": 2,
          "placeholder": "e.g. MD — Harvard Medical School 2010, BS Biology — MIT 2006",
          "helpText": "List degrees, institutions, and years",
          "fhirMapping": {"resource": "Practitioner", "path": "qualification[1].code.text", "type": "string"}
        },
        {
          "key": "credentials.boardCertifications",
          "label": "Board Certifications",
          "type": "textarea",
          "required": false,
          "colSpan": 2,
          "placeholder": "e.g. ABIM — Internal Medicine (expires 2027)",
          "helpText": "List certifying board, specialty, and expiration",
          "fhirMapping": {"resource": "Practitioner", "path": "qualification[2].code.text", "type": "string"}
        },
        {
          "key": "credentials.affiliatedOrganizations",
          "label": "Hospital / Organization Affiliations",
          "type": "textarea",
          "required": false,
          "colSpan": 2,
          "placeholder": "e.g. City General Hospital — Active Staff",
          "fhirMapping": {"resource": "Practitioner", "path": "extension[0].valueString", "type": "string"}
        },
        {
          "key": "credentials.employmentStatus",
          "label": "Employment Status",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "full_time", "label": "Full Time"},
            {"value": "part_time", "label": "Part Time"},
            {"value": "contract", "label": "Contract / Locum"},
            {"value": "per_diem", "label": "Per Diem"},
            {"value": "retired", "label": "Retired"}
          ],
          "fhirMapping": {"resource": "Practitioner", "path": "extension[1].valueCode", "type": "code"}
        }
      ]
    },
    {
      "key": "billing-info",
      "title": "Billing & Insurance",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "billing.billingNpi",
          "label": "Billing NPI",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "Billing NPI (if different)",
          "helpText": "Use if billing NPI differs from individual NPI (e.g. group NPI)",
          "validation": {"pattern": "^[0-9]{10}$", "maxLength": 10},
          "fhirMapping": {"resource": "Practitioner", "path": "identifier[4].value", "type": "string", "system": "http://hl7.org/fhir/sid/us-npi"}
        },
        {
          "key": "billing.taxId",
          "label": "Tax ID / EIN",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "XX-XXXXXXX",
          "validation": {"maxLength": 15},
          "fhirMapping": {"resource": "Practitioner", "path": "identifier[5].value", "type": "string", "system": "urn:oid:2.16.840.1.113883.4.4"}
        },
        {
          "key": "billing.medicareMedicaidId",
          "label": "Medicare / Medicaid ID",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "Medicare or Medicaid provider #",
          "fhirMapping": {"resource": "Practitioner", "path": "identifier[6].value", "type": "string", "system": "http://hl7.org/fhir/sid/us-medicare"}
        },
        {
          "key": "billing.credentialedPayers",
          "label": "Credentialed Payers",
          "type": "textarea",
          "required": false,
          "colSpan": 3,
          "placeholder": "List insurance payers this provider is credentialed with",
          "helpText": "e.g. Aetna, BCBS, Cigna, UnitedHealthcare, Medicare, Medicaid"
        }
      ]
    },
    {
      "key": "system-access",
      "title": "System Access & Status",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "systemAccess.status",
          "label": "Provider Status",
          "type": "select",
          "required": true,
          "colSpan": 1,
          "options": [
            {"value": "ACTIVE", "label": "Active"},
            {"value": "ARCHIVED", "label": "Archived / Inactive"}
          ],
          "badgeColors": {"ACTIVE": "bg-green-100 text-green-800", "ARCHIVED": "bg-gray-100 text-gray-600"},
          "fhirMapping": {"resource": "Practitioner", "path": "active", "type": "boolean"}
        },
        {
          "key": "systemAccess.username",
          "label": "Username / Login",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "System login username"
        },
        {
          "key": "systemAccess.rolesPermissions",
          "label": "Roles & Permissions",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "provider", "label": "Provider (Clinical)"},
            {"value": "admin_provider", "label": "Provider + Admin"},
            {"value": "billing_provider", "label": "Provider + Billing"},
            {"value": "supervising", "label": "Supervising Provider"},
            {"value": "referring", "label": "Referring Only"}
          ]
        }
      ]
    },
    {
      "key": "availability",
      "title": "Availability & Scheduling",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "scheduling.practiceLocations",
          "label": "Practice Locations",
          "type": "textarea",
          "required": false,
          "colSpan": 3,
          "placeholder": "List locations where this provider sees patients",
          "helpText": "e.g. Main Office — 123 Main St; Satellite — 456 Oak Ave",
          "fhirMapping": {"resource": "PractitionerRole", "path": "location", "type": "reference"}
        },
        {
          "key": "scheduling.mondayHours",
          "label": "Monday",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. 8:00 AM - 5:00 PM",
          "fhirMapping": {"resource": "PractitionerRole", "path": "availableTime[0].availableStartTime", "type": "string"}
        },
        {
          "key": "scheduling.tuesdayHours",
          "label": "Tuesday",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. 8:00 AM - 5:00 PM",
          "fhirMapping": {"resource": "PractitionerRole", "path": "availableTime[1].availableStartTime", "type": "string"}
        },
        {
          "key": "scheduling.wednesdayHours",
          "label": "Wednesday",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. 8:00 AM - 5:00 PM",
          "fhirMapping": {"resource": "PractitionerRole", "path": "availableTime[2].availableStartTime", "type": "string"}
        },
        {
          "key": "scheduling.thursdayHours",
          "label": "Thursday",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. 8:00 AM - 5:00 PM",
          "fhirMapping": {"resource": "PractitionerRole", "path": "availableTime[3].availableStartTime", "type": "string"}
        },
        {
          "key": "scheduling.fridayHours",
          "label": "Friday",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. 8:00 AM - 3:00 PM",
          "fhirMapping": {"resource": "PractitionerRole", "path": "availableTime[4].availableStartTime", "type": "string"}
        },
        {
          "key": "scheduling.saturdayHours",
          "label": "Saturday",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. Closed or 9:00 AM - 12:00 PM",
          "fhirMapping": {"resource": "PractitionerRole", "path": "availableTime[5].availableStartTime", "type": "string"}
        },
        {
          "key": "scheduling.sundayHours",
          "label": "Sunday",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. Closed",
          "fhirMapping": {"resource": "PractitionerRole", "path": "availableTime[6].availableStartTime", "type": "string"}
        },
        {
          "key": "scheduling.appointmentDuration",
          "label": "Default Appt Duration (min)",
          "type": "number",
          "required": false,
          "colSpan": 1,
          "placeholder": "30",
          "helpText": "Default appointment slot duration in minutes",
          "validation": {"min": 5, "max": 480},
          "fhirMapping": {"resource": "Schedule", "path": "extension[0].valueInteger", "type": "string"}
        },
        {
          "key": "scheduling.onCallStatus",
          "label": "On-Call Status",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "available", "label": "Available"},
            {"value": "on_call", "label": "On Call"},
            {"value": "unavailable", "label": "Unavailable"},
            {"value": "vacation", "label": "Vacation / Leave"}
          ],
          "badgeColors": {
            "available": "bg-green-100 text-green-800",
            "on_call": "bg-yellow-100 text-yellow-800",
            "unavailable": "bg-red-100 text-red-800",
            "vacation": "bg-blue-100 text-blue-800"
          },
          "fhirMapping": {"resource": "PractitionerRole", "path": "availabilityExceptions", "type": "string"}
        },
        {
          "key": "scheduling.acceptingNewPatients",
          "label": "Accepting New Patients",
          "type": "boolean",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "PractitionerRole", "path": "extension[0].valueBoolean", "type": "boolean"}
        },
        {
          "key": "scheduling.telehealth",
          "label": "Telehealth Available",
          "type": "boolean",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "PractitionerRole", "path": "extension[1].valueBoolean", "type": "boolean"}
        },
        {
          "key": "scheduling.maxDailyPatients",
          "label": "Max Patients Per Day",
          "type": "number",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. 25",
          "helpText": "Maximum patient appointments per day",
          "validation": {"min": 1, "max": 100}
        }
      ]
    }
  ]
}'::jsonb,
    fhir_resources = '[{"type": "Practitioner"}, {"type": "PractitionerRole"}, {"type": "Schedule"}]'::jsonb,
    updated_at = now()
WHERE tab_key = 'providers'
  AND practice_type_code = '*'
  AND org_id = '*';
