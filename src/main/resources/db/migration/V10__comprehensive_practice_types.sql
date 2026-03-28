-- Add comprehensive practice types covering all major healthcare categories
-- Uses ON CONFLICT to skip existing codes

INSERT INTO practice_type (code, name, category, description, icon) VALUES
-- MEDICAL
('cardiology',              'Cardiology',                   'MEDICAL',        'Heart and cardiovascular care',                       'Heart'),
('dermatology',             'Dermatology',                  'MEDICAL',        'Skin, hair, and nail conditions',                     'Scan'),
('endocrinology',           'Endocrinology',                'MEDICAL',        'Hormonal and metabolic disorders',                    'Pill'),
('family-medicine',         'Family Medicine',              'MEDICAL',        'Comprehensive care for all ages',                     'Stethoscope'),
('gastroenterology',        'Gastroenterology',             'MEDICAL',        'Digestive system disorders',                          'Stethoscope'),
('geriatric-medicine',      'Geriatric Medicine',           'MEDICAL',        'Elderly and aging-related care',                      'Heart'),
('hematology-oncology',     'Hematology / Oncology',        'MEDICAL',        'Blood disorders and cancer treatment',                'Droplet'),
('infectious-disease',      'Infectious Disease',           'MEDICAL',        'Bacterial, viral, and parasitic infections',           'Shield'),
('nephrology',              'Nephrology',                   'MEDICAL',        'Kidney diseases and dialysis',                        'Stethoscope'),
('neurology',               'Neurology',                    'MEDICAL',        'Brain, spine, and nervous system disorders',           'Brain'),
('obstetrics-gynecology',   'Obstetrics & Gynecology',      'MEDICAL',        'Women''s reproductive health and pregnancy',           'Heart'),
('oncology',                'Oncology',                     'MEDICAL',        'Cancer diagnosis and treatment',                      'Stethoscope'),
('pulmonology',             'Pulmonology',                  'MEDICAL',        'Lung and respiratory diseases',                       'Wind'),
('rheumatology',            'Rheumatology',                 'MEDICAL',        'Autoimmune and musculoskeletal diseases',              'Bone'),
('sports-medicine',         'Sports Medicine',              'MEDICAL',        'Athletic injuries and performance',                   'Dumbbell'),
('urgent-care',             'Urgent Care',                  'MEDICAL',        'Walk-in acute care services',                         'Zap'),
('allergy-immunology',      'Allergy & Immunology',         'MEDICAL',        'Allergies, asthma, and immune disorders',              'AlertTriangle'),
('pain-management',         'Pain Management',              'MEDICAL',        'Chronic and acute pain treatment',                    'Stethoscope'),
('sleep-medicine',          'Sleep Medicine',               'MEDICAL',        'Sleep disorders diagnosis and treatment',              'Moon'),
('neonatology',             'Neonatology',                  'MEDICAL',        'Newborn and premature infant care',                   'Baby'),
('hospice-palliative',      'Hospice & Palliative Care',    'MEDICAL',        'End-of-life and comfort care',                        'Heart'),
('preventive-medicine',     'Preventive Medicine',          'MEDICAL',        'Disease prevention and wellness',                     'ShieldCheck'),

-- SURGICAL
('cardiothoracic-surgery',  'Cardiothoracic Surgery',       'SURGICAL',       'Heart and chest surgery',                             'Heart'),
('neurosurgery',            'Neurosurgery',                 'SURGICAL',       'Brain and spinal cord surgery',                       'Brain'),
('plastic-surgery',         'Plastic & Reconstructive',     'SURGICAL',       'Cosmetic and reconstructive procedures',              'Scissors'),
('vascular-surgery',        'Vascular Surgery',             'SURGICAL',       'Blood vessel surgery',                                'Heart'),
('colorectal-surgery',      'Colorectal Surgery',           'SURGICAL',       'Colon, rectal, and anal surgery',                     'Scissors'),
('urology',                 'Urology',                      'SURGICAL',       'Urinary tract and male reproductive surgery',          'Stethoscope'),
('ophthalmology',           'Ophthalmology',                'SURGICAL',       'Eye surgery and medical eye care',                    'Eye'),
('otolaryngology',          'Otolaryngology (ENT)',         'SURGICAL',       'Ear, nose, and throat surgery',                       'Ear'),
('oral-maxillofacial',      'Oral & Maxillofacial Surgery', 'SURGICAL',       'Jaw, face, and mouth surgery',                        'Scissors'),
('bariatric-surgery',       'Bariatric Surgery',            'SURGICAL',       'Weight-loss surgery',                                 'Scissors'),
('hand-surgery',            'Hand Surgery',                 'SURGICAL',       'Hand and upper extremity surgery',                    'Hand'),
('trauma-surgery',          'Trauma Surgery',               'SURGICAL',       'Emergency and trauma surgical care',                  'Zap'),

-- BEHAVIORAL / MENTAL HEALTH
('addiction-medicine',       'Addiction Medicine',           'BEHAVIORAL',     'Substance abuse and addiction treatment',              'Brain'),
('counseling',               'Counseling',                  'BEHAVIORAL',     'Licensed professional counseling',                    'MessageSquare'),
('clinical-social-work',     'Clinical Social Work',        'BEHAVIORAL',     'Licensed clinical social work services',              'Users'),
('marriage-family-therapy',  'Marriage & Family Therapy',   'BEHAVIORAL',     'Relationship and family counseling',                  'Users'),
('neuropsychology',          'Neuropsychology',             'BEHAVIORAL',     'Brain-behavior relationships',                        'Brain'),
('child-adolescent-psych',   'Child & Adolescent Psychiatry','BEHAVIORAL',    'Youth mental health services',                        'Baby'),
('behavioral-health',        'Behavioral Health',           'BEHAVIORAL',     'Integrated behavioral health services',               'Brain'),
('applied-behavior-analysis','Applied Behavior Analysis',   'BEHAVIORAL',     'ABA therapy for autism and developmental disorders',   'Brain'),

-- DENTAL
('orthodontics',            'Orthodontics',                 'DENTAL',         'Braces, aligners, and bite correction',               'SmilePlus'),
('periodontics',            'Periodontics',                 'DENTAL',         'Gum disease and dental implants',                     'SmilePlus'),
('endodontics',             'Endodontics',                  'DENTAL',         'Root canal and dental pulp therapy',                  'SmilePlus'),
('prosthodontics',          'Prosthodontics',               'DENTAL',         'Crowns, bridges, and dentures',                       'SmilePlus'),
('oral-surgery',            'Oral Surgery',                 'DENTAL',         'Tooth extractions and dental surgery',                'Scissors'),
('pediatric-dentistry',     'Pediatric Dentistry',          'DENTAL',         'Dental care for children',                            'Baby'),
('cosmetic-dentistry',      'Cosmetic Dentistry',           'DENTAL',         'Aesthetic dental procedures',                         'SmilePlus'),

-- VISION
('optometry',               'Optometry',                    'VISION',         'Eye exams, glasses, and contact lenses',              'Eye'),
('pediatric-ophthalmology',  'Pediatric Ophthalmology',     'VISION',         'Children''s eye care and surgery',                    'Eye'),
('retina-specialist',        'Retina Specialist',           'VISION',         'Retinal diseases and surgery',                        'Eye'),
('low-vision-rehab',         'Low Vision Rehabilitation',   'VISION',         'Visual rehabilitation services',                      'Eye'),

-- ALLIED HEALTH
('speech-language-pathology','Speech-Language Pathology',    'ALLIED_HEALTH',  'Speech, language, and swallowing therapy',            'MessageSquare'),
('audiology',               'Audiology',                    'ALLIED_HEALTH',  'Hearing and balance disorders',                       'Ear'),
('chiropractic',            'Chiropractic',                 'ALLIED_HEALTH',  'Spinal manipulation and musculoskeletal care',         'Bone'),
('podiatry',                'Podiatry',                     'ALLIED_HEALTH',  'Foot, ankle, and lower leg care',                     'Footprints'),
('acupuncture',             'Acupuncture',                  'ALLIED_HEALTH',  'Traditional Chinese medicine and acupuncture',         'Stethoscope'),
('massage-therapy',         'Massage Therapy',              'ALLIED_HEALTH',  'Therapeutic massage services',                        'Hand'),
('dietetics-nutrition',     'Dietetics & Nutrition',        'ALLIED_HEALTH',  'Medical nutrition therapy and counseling',             'Apple'),
('respiratory-therapy',     'Respiratory Therapy',          'ALLIED_HEALTH',  'Breathing and ventilator management',                 'Wind'),
('athletic-training',       'Athletic Training',            'ALLIED_HEALTH',  'Sports injury prevention and rehabilitation',          'Dumbbell'),
('midwifery',               'Midwifery',                    'ALLIED_HEALTH',  'Pregnancy, birth, and postpartum care',               'Baby'),
('genetic-counseling',      'Genetic Counseling',           'ALLIED_HEALTH',  'Genetic testing and hereditary conditions',            'Dna'),

-- HOME HEALTH
('home-health-nursing',     'Home Health Nursing',          'HOME_HEALTH',    'Skilled nursing at home',                             'Home'),
('home-health-aide',        'Home Health Aide',             'HOME_HEALTH',    'Personal care assistance at home',                    'Home'),
('home-infusion',           'Home Infusion Therapy',        'HOME_HEALTH',    'IV therapy and infusions at home',                    'Droplet'),
('home-hospice',            'Home Hospice',                 'HOME_HEALTH',    'End-of-life care at home',                            'Heart'),
('home-physical-therapy',   'Home Physical Therapy',        'HOME_HEALTH',    'Physical therapy in home setting',                    'Dumbbell'),

-- INPATIENT
('emergency-medicine',      'Emergency Medicine',           'INPATIENT',      'Emergency department care',                           'Zap'),
('critical-care',           'Critical Care / ICU',          'INPATIENT',      'Intensive care unit medicine',                        'HeartPulse'),
('inpatient-rehab',         'Inpatient Rehabilitation',     'INPATIENT',      'Hospital-based rehabilitation',                       'Dumbbell'),
('long-term-acute-care',    'Long-Term Acute Care',         'INPATIENT',      'Extended hospital care for complex conditions',        'Building2'),
('skilled-nursing',         'Skilled Nursing Facility',     'INPATIENT',      'Post-acute skilled nursing care',                     'Building2'),

-- RADIOLOGY / DIAGNOSTIC
('diagnostic-radiology',    'Diagnostic Radiology',         'DIAGNOSTIC',     'Medical imaging and interpretation',                  'Scan'),
('interventional-radiology','Interventional Radiology',     'DIAGNOSTIC',     'Image-guided minimally invasive procedures',           'Scan'),
('nuclear-medicine',        'Nuclear Medicine',             'DIAGNOSTIC',     'Radioactive tracers for diagnosis and therapy',         'Atom'),
('pathology',               'Pathology',                    'DIAGNOSTIC',     'Laboratory diagnosis of disease',                     'Microscope'),

-- PHARMACY
('retail-pharmacy',         'Retail Pharmacy',              'PHARMACY',       'Community/retail pharmacy services',                   'Pill'),
('clinical-pharmacy',       'Clinical Pharmacy',            'PHARMACY',       'Hospital/clinical pharmacist services',                'Pill'),
('compounding-pharmacy',    'Compounding Pharmacy',         'PHARMACY',       'Custom medication compounding',                       'FlaskConical'),

-- WELLNESS / INTEGRATIVE
('integrative-medicine',    'Integrative Medicine',         'WELLNESS',       'Combines conventional and complementary approaches',   'Leaf'),
('naturopathic-medicine',   'Naturopathic Medicine',        'WELLNESS',       'Natural and holistic treatments',                     'Leaf'),
('functional-medicine',     'Functional Medicine',          'WELLNESS',       'Root-cause approach to chronic disease',               'Stethoscope'),
('med-spa',                 'Medical Spa / Aesthetics',     'WELLNESS',       'Aesthetic and anti-aging treatments',                  'Sparkles')

ON CONFLICT (code, org_id) DO NOTHING;

-- Add new categories to existing specialties
INSERT INTO specialty (code, name, description, icon, parent_code) VALUES
-- Vision
('optometry',               'Optometry',                    'Vision and eye care',                         'Eye',         NULL),
('ophthalmology',           'Ophthalmology',                'Medical and surgical eye care',               'Eye',         NULL),
-- Chiropractic
('chiropractic',            'Chiropractic',                 'Spinal and musculoskeletal care',             'Bone',        NULL),
-- Podiatry
('podiatry',                'Podiatry',                     'Foot and ankle care',                         'Footprints',  NULL),
-- Audiology
('audiology',               'Audiology',                    'Hearing and balance care',                    'Ear',         NULL),
-- Addictions
('addiction-medicine',       'Addiction Medicine',           'Substance abuse treatment',                   'Brain',       'psychiatry'),
-- Counseling
('counseling',               'Counseling',                  'Professional counseling services',             'MessageSquare', NULL),
-- Pain management
('pain-management',          'Pain Management',             'Chronic pain treatment',                      'Stethoscope',  NULL),
-- Speech
('speech-language-pathology', 'Speech-Language Pathology',  'Speech and swallowing therapy',               'MessageSquare', NULL),
-- Respiratory
('respiratory-therapy',       'Respiratory Therapy',        'Breathing and pulmonary care',                'Wind',         NULL),
-- Nutrition
('dietetics-nutrition',       'Dietetics & Nutrition',      'Medical nutrition therapy',                   'Apple',        NULL)
ON CONFLICT (code, org_id) DO NOTHING;
