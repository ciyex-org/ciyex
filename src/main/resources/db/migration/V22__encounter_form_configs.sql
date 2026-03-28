-- V22: Dynamic encounter form configs
-- Replaces hardcoded encounter section components with config-driven rendering.
-- Uses Composition FHIR resource to store clinical note content per encounter.

-- =====================================================
-- 1. Universal encounter form (all specialties)
-- =====================================================
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, label, icon, category, category_position, position, visible, version)
VALUES (
  'encounter-form', '*', '*',
  '[{"type":"Composition","patientSearchParam":"subject"}]',
  '{
    "features": {
      "encounterForm": {
        "autoSave": { "enabled": true, "debounceMs": 2000 },
        "signLock": { "enabled": true }
      }
    },
    "sections": [
      {
        "key": "chief-complaint",
        "title": "Chief Complaint",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"cc_text","label":"Chief Complaint","type":"textarea","required":true,"colSpan":1,"placeholder":"Why is the patient being seen today?"}
        ]
      },
      {
        "key": "hpi",
        "title": "History of Present Illness",
        "columns": 2,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"hpi_onset","label":"Onset","type":"text","colSpan":1,"placeholder":"When did it start?"},
          {"key":"hpi_location","label":"Location","type":"text","colSpan":1,"placeholder":"Where is it?"},
          {"key":"hpi_duration","label":"Duration","type":"text","colSpan":1,"placeholder":"How long?"},
          {"key":"hpi_character","label":"Character","type":"text","colSpan":1,"placeholder":"What does it feel like?"},
          {"key":"hpi_severity","label":"Severity","type":"select","colSpan":1,"options":[{"value":"mild","label":"Mild"},{"value":"moderate","label":"Moderate"},{"value":"severe","label":"Severe"}]},
          {"key":"hpi_timing","label":"Timing","type":"text","colSpan":1,"placeholder":"Constant, intermittent?"},
          {"key":"hpi_context","label":"Context","type":"text","colSpan":1,"placeholder":"What were you doing?"},
          {"key":"hpi_modifying","label":"Modifying Factors","type":"text","colSpan":1,"placeholder":"What makes it better/worse?"},
          {"key":"hpi_associated","label":"Associated Signs/Symptoms","type":"text","colSpan":2,"placeholder":"Any other symptoms?"},
          {"key":"hpi_narrative","label":"HPI Narrative","type":"textarea","colSpan":2,"placeholder":"Free-text narrative..."}
        ]
      },
      {
        "key": "ros",
        "title": "Review of Systems",
        "columns": 1,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"ros_data","label":"Review of Systems","type":"ros-grid","colSpan":1,
           "rosConfig":{"systems":[
             {"key":"constitutional","label":"Constitutional","findings":["fever","chills","nightSweats","fatigue","weightLoss","weightGain"]},
             {"key":"eyes","label":"Eyes","findings":["visionLoss","blurredVision","eyePain","eyeRedness"]},
             {"key":"ent","label":"ENT","findings":["earache","ringing","hearingLoss","nasalCongestion","soreThroat","difficultySwallowing"]},
             {"key":"cardiovascular","label":"Cardiovascular","findings":["chestPain","palpitations","shortnessOfBreath","edema","syncope"]},
             {"key":"respiratory","label":"Respiratory","findings":["cough","wheezing","dyspnea","hemoptysis"]},
             {"key":"gastrointestinal","label":"Gastrointestinal","findings":["nausea","vomiting","diarrhea","constipation","abdominalPain"]},
             {"key":"genitourinary","label":"Genitourinary","findings":["frequency","urgency","dysuria","hematuria"]},
             {"key":"musculoskeletal","label":"Musculoskeletal","findings":["jointPain","stiffness","swelling","backPain","muscleWeakness"]},
             {"key":"integumentary","label":"Integumentary/Skin","findings":["rash","itching","lesions","dryness"]},
             {"key":"neurological","label":"Neurological","findings":["headaches","dizziness","numbness","tingling","seizures","tremor"]},
             {"key":"psychiatric","label":"Psychiatric","findings":["anxiety","depression","insomnia","suicidalIdeation"]},
             {"key":"endocrine","label":"Endocrine","findings":["polyuria","polydipsia","heatIntolerance","coldIntolerance"]},
             {"key":"hematologic","label":"Hematologic/Lymphatic","findings":["easyBruising","easyBleeding","lymphadenopathy"]},
             {"key":"allergic","label":"Allergic/Immunologic","findings":["seasonalAllergies","hives","frequentInfections"]}
           ]}}
        ]
      },
      {
        "key": "vitals",
        "title": "Vitals",
        "columns": 4,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"vitals_bp_systolic","label":"BP Systolic","type":"number","colSpan":1,"placeholder":"mmHg"},
          {"key":"vitals_bp_diastolic","label":"BP Diastolic","type":"number","colSpan":1,"placeholder":"mmHg"},
          {"key":"vitals_hr","label":"Heart Rate","type":"number","colSpan":1,"placeholder":"bpm"},
          {"key":"vitals_temp","label":"Temperature","type":"number","colSpan":1,"placeholder":"\u00b0F"},
          {"key":"vitals_spo2","label":"SpO2","type":"number","colSpan":1,"placeholder":"%"},
          {"key":"vitals_rr","label":"Respiratory Rate","type":"number","colSpan":1,"placeholder":"/min"},
          {"key":"vitals_weight","label":"Weight","type":"number","colSpan":1,"placeholder":"lbs"},
          {"key":"vitals_height","label":"Height","type":"number","colSpan":1,"placeholder":"in"},
          {"key":"vitals_bmi","label":"BMI","type":"computed","colSpan":1,"computeExpression":"(vitals_weight * 703) / (vitals_height * vitals_height)"},
          {"key":"vitals_notes","label":"Notes","type":"text","colSpan":3}
        ]
      },
      {
        "key": "physical-exam",
        "title": "Physical Exam",
        "columns": 1,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"pe_data","label":"Physical Exam","type":"exam-grid","colSpan":1,
           "examConfig":{"systems":[
             {"key":"general","label":"General Appearance","defaultNormal":"Well-appearing, in no acute distress"},
             {"key":"heent","label":"HEENT","defaultNormal":"Normocephalic, atraumatic. Pupils equal, round, reactive to light. TMs clear bilaterally. Oropharynx clear, moist mucous membranes."},
             {"key":"neck","label":"Neck","defaultNormal":"Supple, no lymphadenopathy, no thyromegaly, no JVD"},
             {"key":"lungs","label":"Chest/Lungs","defaultNormal":"Clear to auscultation bilaterally, no wheezes, rales, or rhonchi. Normal respiratory effort."},
             {"key":"cardiovascular","label":"Cardiovascular","defaultNormal":"Regular rate and rhythm, normal S1/S2, no murmurs, rubs, or gallops"},
             {"key":"abdomen","label":"Abdomen","defaultNormal":"Soft, non-tender, non-distended, normoactive bowel sounds, no hepatosplenomegaly"},
             {"key":"extremities","label":"Extremities","defaultNormal":"No edema, no cyanosis, no clubbing. Pulses 2+ bilaterally."},
             {"key":"neurological","label":"Neurological","defaultNormal":"Alert and oriented x3. Cranial nerves II-XII grossly intact. Strength 5/5 all extremities."},
             {"key":"skin","label":"Skin","defaultNormal":"Warm, dry, intact. No rashes, lesions, or concerning moles."},
             {"key":"psychiatric","label":"Psychiatric","defaultNormal":"Appropriate mood and affect. Normal speech. Cooperative."}
           ]}}
        ]
      },
      {
        "key": "past-medical-history",
        "title": "Past Medical / Surgical History",
        "columns": 1,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"pmh_conditions","label":"Past Medical History","type":"textarea","colSpan":1,"placeholder":"Known medical conditions..."},
          {"key":"pmh_surgeries","label":"Surgical History","type":"textarea","colSpan":1,"placeholder":"Prior surgeries..."}
        ]
      },
      {
        "key": "family-history",
        "title": "Family History",
        "columns": 2,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"fh_father","label":"Father","type":"text","colSpan":1,"placeholder":"Known conditions"},
          {"key":"fh_mother","label":"Mother","type":"text","colSpan":1,"placeholder":"Known conditions"},
          {"key":"fh_siblings","label":"Siblings","type":"text","colSpan":1,"placeholder":"Known conditions"},
          {"key":"fh_notes","label":"Additional Notes","type":"textarea","colSpan":2}
        ]
      },
      {
        "key": "social-history",
        "title": "Social History",
        "columns": 3,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"sh_smoking","label":"Smoking","type":"select","colSpan":1,"options":[{"value":"never","label":"Never"},{"value":"former","label":"Former"},{"value":"current","label":"Current"}]},
          {"key":"sh_alcohol","label":"Alcohol","type":"select","colSpan":1,"options":[{"value":"none","label":"None"},{"value":"social","label":"Social"},{"value":"moderate","label":"Moderate"},{"value":"heavy","label":"Heavy"}]},
          {"key":"sh_exercise","label":"Exercise","type":"select","colSpan":1,"options":[{"value":"none","label":"None"},{"value":"occasional","label":"Occasional"},{"value":"regular","label":"Regular"},{"value":"daily","label":"Daily"}]},
          {"key":"sh_occupation","label":"Occupation","type":"text","colSpan":1},
          {"key":"sh_drugs","label":"Recreational Drugs","type":"select","colSpan":1,"options":[{"value":"none","label":"None"},{"value":"marijuana","label":"Marijuana"},{"value":"other","label":"Other"}]},
          {"key":"sh_notes","label":"Additional Notes","type":"textarea","colSpan":3}
        ]
      },
      {
        "key": "assessment",
        "title": "Assessment & Diagnosis",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"assessment_diagnoses","label":"Diagnoses","type":"diagnosis-list","colSpan":1,
           "diagnosisConfig":{"codeSystem":"ICD10","searchEndpoint":"/api/global_codes?codeType=ICD10","allowMultiple":true}},
          {"key":"assessment_notes","label":"Assessment Notes","type":"textarea","colSpan":1,"placeholder":"Clinical reasoning and assessment..."}
        ]
      },
      {
        "key": "plan",
        "title": "Plan",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"plan_items","label":"Treatment Plan","type":"plan-items","colSpan":1},
          {"key":"plan_followup","label":"Follow-up","type":"text","colSpan":1,"placeholder":"Return in..."},
          {"key":"plan_notes","label":"Additional Plan Notes","type":"textarea","colSpan":1}
        ]
      },
      {
        "key": "provider-notes",
        "title": "Provider Notes",
        "columns": 1,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"provider_narrative","label":"Provider Notes","type":"textarea","colSpan":1,"placeholder":"Additional provider notes..."}
        ]
      }
    ]
  }',
  'Encounter Form', 'ClipboardList', NULL, NULL, NULL, false,
  1
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET fhir_resources = EXCLUDED.fhir_resources, field_config = EXCLUDED.field_config, updated_at = now();


-- =====================================================
-- 2. Cardiology encounter form
-- =====================================================
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, label, icon, category, category_position, position, visible, version)
VALUES (
  'encounter-form', 'cardiology', '*',
  '[{"type":"Composition","patientSearchParam":"subject"}]',
  '{
    "features": {
      "encounterForm": {
        "autoSave": { "enabled": true, "debounceMs": 2000 },
        "signLock": { "enabled": true }
      }
    },
    "sections": [
      {
        "key": "chief-complaint",
        "title": "Chief Complaint",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"cc_text","label":"Chief Complaint","type":"textarea","required":true,"colSpan":1,"placeholder":"Cardiac complaint..."}
        ]
      },
      {
        "key": "cardiac-history",
        "title": "Cardiac History",
        "columns": 3,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"ch_prior_mi","label":"Prior MI","type":"boolean","colSpan":1},
          {"key":"ch_cabg","label":"CABG","type":"boolean","colSpan":1},
          {"key":"ch_pci","label":"PCI/Stent","type":"boolean","colSpan":1},
          {"key":"ch_valve_disease","label":"Valve Disease","type":"boolean","colSpan":1},
          {"key":"ch_arrhythmia","label":"Arrhythmia History","type":"boolean","colSpan":1},
          {"key":"ch_chf","label":"Heart Failure","type":"boolean","colSpan":1},
          {"key":"ch_pacemaker","label":"Pacemaker/ICD","type":"boolean","colSpan":1},
          {"key":"ch_hypertension","label":"Hypertension","type":"boolean","colSpan":1},
          {"key":"ch_hyperlipidemia","label":"Hyperlipidemia","type":"boolean","colSpan":1},
          {"key":"ch_notes","label":"Cardiac History Notes","type":"textarea","colSpan":3}
        ]
      },
      {
        "key": "hpi",
        "title": "History of Present Illness",
        "columns": 2,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"hpi_onset","label":"Onset","type":"text","colSpan":1},
          {"key":"hpi_character","label":"Character","type":"text","colSpan":1,"placeholder":"Pressure, sharp, squeezing..."},
          {"key":"hpi_severity","label":"Severity (1-10)","type":"number","colSpan":1},
          {"key":"hpi_radiation","label":"Radiation","type":"text","colSpan":1,"placeholder":"Left arm, jaw, back..."},
          {"key":"hpi_duration","label":"Duration","type":"text","colSpan":1},
          {"key":"hpi_exertional","label":"Exertional","type":"boolean","colSpan":1},
          {"key":"hpi_modifying","label":"Relieved by","type":"text","colSpan":1,"placeholder":"Rest, NTG..."},
          {"key":"hpi_associated","label":"Associated Symptoms","type":"text","colSpan":1,"placeholder":"Dyspnea, diaphoresis, nausea..."},
          {"key":"hpi_narrative","label":"HPI Narrative","type":"textarea","colSpan":2}
        ]
      },
      {
        "key": "ros",
        "title": "Cardiac Review of Systems",
        "columns": 1,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"ros_data","label":"Cardiac ROS","type":"ros-grid","colSpan":1,
           "rosConfig":{"systems":[
             {"key":"constitutional","label":"Constitutional","findings":["fatigue","malaise","weightGain","weightLoss"]},
             {"key":"cardiovascular","label":"Cardiovascular","findings":["chestPain","palpitations","dyspneaOnExertion","orthopnea","PND","edema","syncope","presyncope","claudication"]},
             {"key":"respiratory","label":"Respiratory","findings":["dyspnea","cough","wheezing","hemoptysis"]},
             {"key":"neurological","label":"Neurological","findings":["dizziness","lightheadedness","TIA","strokeSymptoms"]}
           ]}}
        ]
      },
      {
        "key": "vitals",
        "title": "Vitals",
        "columns": 4,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"vitals_bp_systolic","label":"BP Systolic","type":"number","colSpan":1,"placeholder":"mmHg"},
          {"key":"vitals_bp_diastolic","label":"BP Diastolic","type":"number","colSpan":1,"placeholder":"mmHg"},
          {"key":"vitals_hr","label":"Heart Rate","type":"number","colSpan":1,"placeholder":"bpm"},
          {"key":"vitals_rhythm","label":"Rhythm","type":"select","colSpan":1,"options":[{"value":"regular","label":"Regular"},{"value":"irregular","label":"Irregular"},{"value":"afib","label":"Atrial Fibrillation"},{"value":"aflutter","label":"Atrial Flutter"}]},
          {"key":"vitals_spo2","label":"SpO2","type":"number","colSpan":1,"placeholder":"%"},
          {"key":"vitals_rr","label":"Respiratory Rate","type":"number","colSpan":1,"placeholder":"/min"},
          {"key":"vitals_weight","label":"Weight","type":"number","colSpan":1,"placeholder":"lbs"},
          {"key":"vitals_height","label":"Height","type":"number","colSpan":1,"placeholder":"in"}
        ]
      },
      {
        "key": "physical-exam",
        "title": "Cardiac Exam",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"pe_data","label":"Cardiac Exam","type":"exam-grid","colSpan":1,
           "examConfig":{"systems":[
             {"key":"general","label":"General Appearance","defaultNormal":"Well-appearing, in no acute distress"},
             {"key":"neck","label":"Neck/JVP","defaultNormal":"JVP not elevated. No carotid bruits."},
             {"key":"cardiovascular","label":"Heart","defaultNormal":"Regular rate and rhythm. Normal S1/S2. No S3/S4. No murmurs, rubs, or gallops."},
             {"key":"lungs","label":"Lungs","defaultNormal":"Clear to auscultation bilaterally. No crackles or wheezes."},
             {"key":"abdomen","label":"Abdomen","defaultNormal":"Soft, non-tender. No hepatomegaly."},
             {"key":"extremities","label":"Extremities/Pulses","defaultNormal":"No peripheral edema. Pulses 2+ bilaterally (DP, PT, radial). No cyanosis."}
           ]}}
        ]
      },
      {
        "key": "ecg",
        "title": "ECG/EKG Findings",
        "columns": 3,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"ecg_rhythm","label":"Rhythm","type":"select","colSpan":1,"options":[{"value":"nsr","label":"Normal Sinus Rhythm"},{"value":"afib","label":"Atrial Fibrillation"},{"value":"aflutter","label":"Atrial Flutter"},{"value":"svt","label":"SVT"},{"value":"vt","label":"Ventricular Tachycardia"},{"value":"other","label":"Other"}]},
          {"key":"ecg_rate","label":"Rate","type":"number","colSpan":1,"placeholder":"bpm"},
          {"key":"ecg_axis","label":"Axis","type":"select","colSpan":1,"options":[{"value":"normal","label":"Normal"},{"value":"lad","label":"Left Axis Deviation"},{"value":"rad","label":"Right Axis Deviation"}]},
          {"key":"ecg_intervals","label":"Intervals","type":"text","colSpan":1,"placeholder":"PR, QRS, QTc"},
          {"key":"ecg_st_changes","label":"ST Changes","type":"text","colSpan":2,"placeholder":"ST elevation/depression, T-wave changes..."},
          {"key":"ecg_notes","label":"ECG Interpretation","type":"textarea","colSpan":3}
        ]
      },
      {
        "key": "assessment",
        "title": "Assessment & Diagnosis",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"assessment_diagnoses","label":"Diagnoses","type":"diagnosis-list","colSpan":1,
           "diagnosisConfig":{"codeSystem":"ICD10","searchEndpoint":"/api/global_codes?codeType=ICD10","allowMultiple":true}},
          {"key":"assessment_notes","label":"Assessment Notes","type":"textarea","colSpan":1}
        ]
      },
      {
        "key": "plan",
        "title": "Plan",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"plan_items","label":"Treatment Plan","type":"plan-items","colSpan":1},
          {"key":"plan_followup","label":"Follow-up","type":"text","colSpan":1,"placeholder":"Return in..."},
          {"key":"plan_notes","label":"Additional Notes","type":"textarea","colSpan":1}
        ]
      }
    ]
  }',
  'Encounter Form', 'ClipboardList', NULL, NULL, NULL, false,
  1
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET fhir_resources = EXCLUDED.fhir_resources, field_config = EXCLUDED.field_config, updated_at = now();


-- =====================================================
-- 3. Psychiatry encounter form
-- =====================================================
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, label, icon, category, category_position, position, visible, version)
VALUES (
  'encounter-form', 'psychiatry', '*',
  '[{"type":"Composition","patientSearchParam":"subject"}]',
  '{
    "features": {
      "encounterForm": {
        "autoSave": { "enabled": true, "debounceMs": 2000 },
        "signLock": { "enabled": true }
      }
    },
    "sections": [
      {
        "key": "chief-complaint",
        "title": "Chief Complaint",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"cc_text","label":"Chief Complaint","type":"textarea","required":true,"colSpan":1,"placeholder":"Reason for visit / presenting complaint..."}
        ]
      },
      {
        "key": "psychiatric-history",
        "title": "Psychiatric History",
        "columns": 2,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"psh_prior_diagnoses","label":"Prior Diagnoses","type":"textarea","colSpan":2,"placeholder":"Known psychiatric diagnoses..."},
          {"key":"psh_hospitalizations","label":"Psychiatric Hospitalizations","type":"textarea","colSpan":1,"placeholder":"Dates, reasons..."},
          {"key":"psh_suicide_attempts","label":"Prior Suicide Attempts","type":"textarea","colSpan":1,"placeholder":"Dates, methods..."},
          {"key":"psh_medications_tried","label":"Medications Tried","type":"textarea","colSpan":2,"placeholder":"Previous psychotropic medications and responses..."},
          {"key":"psh_therapy_history","label":"Therapy History","type":"textarea","colSpan":2,"placeholder":"Types of therapy, duration, response..."}
        ]
      },
      {
        "key": "hpi",
        "title": "History of Present Illness",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"hpi_narrative","label":"HPI Narrative","type":"textarea","colSpan":1,"placeholder":"Onset, course, triggers, current symptoms, functional impact..."}
        ]
      },
      {
        "key": "mental-status-exam",
        "title": "Mental Status Exam",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"mse_data","label":"Mental Status Exam","type":"exam-grid","colSpan":1,
           "examConfig":{"systems":[
             {"key":"appearance","label":"Appearance","defaultNormal":"Well-groomed, appropriate dress, good hygiene, appears stated age"},
             {"key":"behavior","label":"Behavior/Psychomotor","defaultNormal":"Calm, cooperative, no psychomotor agitation or retardation"},
             {"key":"speech","label":"Speech","defaultNormal":"Normal rate, rhythm, volume, and tone. Spontaneous and goal-directed."},
             {"key":"mood","label":"Mood (patient-reported)","defaultNormal":"\"Good\" / Euthymic"},
             {"key":"affect","label":"Affect (observed)","defaultNormal":"Full range, mood-congruent, appropriate to context"},
             {"key":"thoughtProcess","label":"Thought Process","defaultNormal":"Linear, logical, goal-directed. No looseness of associations, tangentiality, or circumstantiality."},
             {"key":"thoughtContent","label":"Thought Content","defaultNormal":"No suicidal ideation, homicidal ideation, delusions, or obsessions"},
             {"key":"perception","label":"Perception","defaultNormal":"No auditory or visual hallucinations. No illusions."},
             {"key":"cognition","label":"Cognition","defaultNormal":"Alert and oriented x4. Attention and concentration intact. Memory intact for recent and remote events."},
             {"key":"insight","label":"Insight","defaultNormal":"Good insight into illness"},
             {"key":"judgment","label":"Judgment","defaultNormal":"Good judgment demonstrated"}
           ]}}
        ]
      },
      {
        "key": "risk-assessment",
        "title": "Risk Assessment",
        "columns": 2,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"risk_si","label":"Suicidal Ideation","type":"select","colSpan":1,"options":[{"value":"none","label":"None"},{"value":"passive","label":"Passive (wishes to be dead)"},{"value":"active_no_plan","label":"Active, no plan"},{"value":"active_with_plan","label":"Active with plan"},{"value":"active_with_intent","label":"Active with intent"}]},
          {"key":"risk_hi","label":"Homicidal Ideation","type":"select","colSpan":1,"options":[{"value":"none","label":"None"},{"value":"passive","label":"Passive"},{"value":"active","label":"Active"}]},
          {"key":"risk_plan","label":"Plan Details","type":"textarea","colSpan":1,"placeholder":"If SI/HI present, describe plan..."},
          {"key":"risk_means","label":"Access to Means","type":"textarea","colSpan":1,"placeholder":"Firearms, medications, etc."},
          {"key":"risk_protective","label":"Protective Factors","type":"textarea","colSpan":2,"placeholder":"Social support, reasons for living, treatment engagement..."},
          {"key":"risk_level","label":"Overall Risk Level","type":"select","colSpan":1,"options":[{"value":"low","label":"Low"},{"value":"moderate","label":"Moderate"},{"value":"high","label":"High"},{"value":"imminent","label":"Imminent"}],"badgeColors":{"low":"bg-green-100 text-green-700","moderate":"bg-yellow-100 text-yellow-700","high":"bg-orange-100 text-orange-700","imminent":"bg-red-100 text-red-700"}},
          {"key":"risk_safety_plan","label":"Safety Plan Reviewed","type":"boolean","colSpan":1}
        ]
      },
      {
        "key": "substance-use",
        "title": "Substance Use",
        "columns": 3,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"su_alcohol","label":"Alcohol","type":"select","colSpan":1,"options":[{"value":"none","label":"None"},{"value":"social","label":"Social"},{"value":"moderate","label":"Moderate"},{"value":"heavy","label":"Heavy"},{"value":"recovery","label":"In Recovery"}]},
          {"key":"su_tobacco","label":"Tobacco","type":"select","colSpan":1,"options":[{"value":"never","label":"Never"},{"value":"former","label":"Former"},{"value":"current","label":"Current"}]},
          {"key":"su_cannabis","label":"Cannabis","type":"select","colSpan":1,"options":[{"value":"none","label":"None"},{"value":"occasional","label":"Occasional"},{"value":"daily","label":"Daily"}]},
          {"key":"su_other","label":"Other Substances","type":"textarea","colSpan":3,"placeholder":"Stimulants, opioids, benzodiazepines, etc."},
          {"key":"su_notes","label":"Substance Use Notes","type":"textarea","colSpan":3}
        ]
      },
      {
        "key": "assessment",
        "title": "Assessment & Diagnosis",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"assessment_diagnoses","label":"Diagnoses","type":"diagnosis-list","colSpan":1,
           "diagnosisConfig":{"codeSystem":"ICD10","searchEndpoint":"/api/global_codes?codeType=ICD10","allowMultiple":true}},
          {"key":"assessment_notes","label":"Assessment / Formulation","type":"textarea","colSpan":1,"placeholder":"Clinical formulation, biopsychosocial factors..."}
        ]
      },
      {
        "key": "plan",
        "title": "Plan",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"plan_items","label":"Treatment Plan","type":"plan-items","colSpan":1},
          {"key":"plan_followup","label":"Follow-up","type":"text","colSpan":1,"placeholder":"Return in..."},
          {"key":"plan_notes","label":"Additional Notes","type":"textarea","colSpan":1}
        ]
      }
    ]
  }',
  'Encounter Form', 'ClipboardList', NULL, NULL, NULL, false,
  1
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET fhir_resources = EXCLUDED.fhir_resources, field_config = EXCLUDED.field_config, updated_at = now();


-- =====================================================
-- 4. Dermatology encounter form
-- =====================================================
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, label, icon, category, category_position, position, visible, version)
VALUES (
  'encounter-form', 'dermatology', '*',
  '[{"type":"Composition","patientSearchParam":"subject"}]',
  '{
    "features": {
      "encounterForm": {
        "autoSave": { "enabled": true, "debounceMs": 2000 },
        "signLock": { "enabled": true }
      }
    },
    "sections": [
      {
        "key": "chief-complaint",
        "title": "Chief Complaint",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"cc_text","label":"Chief Complaint","type":"textarea","required":true,"colSpan":1,"placeholder":"Skin concern..."}
        ]
      },
      {
        "key": "skin-history",
        "title": "Skin History",
        "columns": 2,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"sh_duration","label":"Duration","type":"text","colSpan":1,"placeholder":"How long?"},
          {"key":"sh_location","label":"Location","type":"text","colSpan":1,"placeholder":"Body site(s)"},
          {"key":"sh_changes","label":"Changes Over Time","type":"text","colSpan":1,"placeholder":"Growing, spreading, changing color..."},
          {"key":"sh_symptoms","label":"Symptoms","type":"text","colSpan":1,"placeholder":"Itching, burning, pain..."},
          {"key":"sh_treatments","label":"Treatments Tried","type":"textarea","colSpan":2,"placeholder":"OTC, prescriptions, home remedies..."},
          {"key":"sh_sun_exposure","label":"Sun Exposure","type":"select","colSpan":1,"options":[{"value":"minimal","label":"Minimal"},{"value":"moderate","label":"Moderate"},{"value":"significant","label":"Significant"},{"value":"excessive","label":"Excessive"}]},
          {"key":"sh_skin_cancer_hx","label":"Skin Cancer History","type":"boolean","colSpan":1}
        ]
      },
      {
        "key": "skin-exam",
        "title": "Skin Examination",
        "columns": 3,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"se_location","label":"Location","type":"text","colSpan":1,"placeholder":"Anatomic site"},
          {"key":"se_morphology","label":"Primary Morphology","type":"select","colSpan":1,"options":[{"value":"macule","label":"Macule"},{"value":"patch","label":"Patch"},{"value":"papule","label":"Papule"},{"value":"plaque","label":"Plaque"},{"value":"nodule","label":"Nodule"},{"value":"vesicle","label":"Vesicle"},{"value":"bulla","label":"Bulla"},{"value":"pustule","label":"Pustule"},{"value":"wheal","label":"Wheal"},{"value":"cyst","label":"Cyst"},{"value":"erosion","label":"Erosion"},{"value":"ulcer","label":"Ulcer"},{"value":"other","label":"Other"}]},
          {"key":"se_size","label":"Size","type":"text","colSpan":1,"placeholder":"cm x cm"},
          {"key":"se_color","label":"Color","type":"text","colSpan":1,"placeholder":"Pink, red, brown, black..."},
          {"key":"se_shape","label":"Shape","type":"select","colSpan":1,"options":[{"value":"round","label":"Round"},{"value":"oval","label":"Oval"},{"value":"irregular","label":"Irregular"},{"value":"annular","label":"Annular"},{"value":"linear","label":"Linear"}]},
          {"key":"se_borders","label":"Borders","type":"select","colSpan":1,"options":[{"value":"well-defined","label":"Well-defined"},{"value":"ill-defined","label":"Ill-defined"},{"value":"irregular","label":"Irregular"}]},
          {"key":"se_distribution","label":"Distribution","type":"select","colSpan":1,"options":[{"value":"localized","label":"Localized"},{"value":"regional","label":"Regional"},{"value":"generalized","label":"Generalized"},{"value":"dermatomal","label":"Dermatomal"},{"value":"photodistributed","label":"Photodistributed"}]},
          {"key":"se_surface","label":"Surface Changes","type":"text","colSpan":2,"placeholder":"Scaling, crusting, excoriation..."},
          {"key":"se_notes","label":"Exam Notes","type":"textarea","colSpan":3}
        ]
      },
      {
        "key": "dermoscopy",
        "title": "Dermoscopy Findings",
        "columns": 2,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"derm_pattern","label":"Pattern","type":"select","colSpan":1,"options":[{"value":"reticular","label":"Reticular"},{"value":"globular","label":"Globular"},{"value":"homogeneous","label":"Homogeneous"},{"value":"starburst","label":"Starburst"},{"value":"multicomponent","label":"Multicomponent"},{"value":"lacunar","label":"Lacunar"},{"value":"other","label":"Other"}]},
          {"key":"derm_structures","label":"Structures","type":"text","colSpan":1,"placeholder":"Network, dots, globules, streaks..."},
          {"key":"derm_colors","label":"Colors","type":"text","colSpan":1,"placeholder":"Brown, black, blue, red, white..."},
          {"key":"derm_notes","label":"Dermoscopy Interpretation","type":"textarea","colSpan":2}
        ]
      },
      {
        "key": "biopsy",
        "title": "Biopsy / Procedure",
        "columns": 3,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"bx_performed","label":"Biopsy Performed","type":"boolean","colSpan":1},
          {"key":"bx_type","label":"Type","type":"select","colSpan":1,"options":[{"value":"shave","label":"Shave"},{"value":"punch","label":"Punch"},{"value":"excisional","label":"Excisional"},{"value":"incisional","label":"Incisional"}]},
          {"key":"bx_location","label":"Location","type":"text","colSpan":1,"placeholder":"Anatomic site"},
          {"key":"bx_size","label":"Specimen Size","type":"text","colSpan":1,"placeholder":"mm"},
          {"key":"bx_sent_to","label":"Sent to","type":"text","colSpan":1,"placeholder":"Lab/pathology"},
          {"key":"bx_notes","label":"Procedure Notes","type":"textarea","colSpan":3}
        ]
      },
      {
        "key": "assessment",
        "title": "Assessment & Diagnosis",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"assessment_diagnoses","label":"Diagnoses","type":"diagnosis-list","colSpan":1,
           "diagnosisConfig":{"codeSystem":"ICD10","searchEndpoint":"/api/global_codes?codeType=ICD10","allowMultiple":true}},
          {"key":"assessment_notes","label":"Assessment Notes","type":"textarea","colSpan":1}
        ]
      },
      {
        "key": "plan",
        "title": "Plan",
        "columns": 1,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"plan_items","label":"Treatment Plan","type":"plan-items","colSpan":1},
          {"key":"plan_followup","label":"Follow-up","type":"text","colSpan":1,"placeholder":"Return in..."},
          {"key":"plan_notes","label":"Additional Notes","type":"textarea","colSpan":1}
        ]
      }
    ]
  }',
  'Encounter Form', 'ClipboardList', NULL, NULL, NULL, false,
  1
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET fhir_resources = EXCLUDED.fhir_resources, field_config = EXCLUDED.field_config, updated_at = now();
