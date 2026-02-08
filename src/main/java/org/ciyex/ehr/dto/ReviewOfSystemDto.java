//package org.ciyex.ehr.dto;
//
//import lombok.Data;
//import java.util.List;
//
//@Data
//public class ReviewOfSystemDto {
//    private Long id;
//    private String externalId;
//
//       // tenant
//    private Long patientId;   // scope
//    private Long encounterId; // scope
//
//    // Single ROS line item per “system”
//    private String systemName;         // e.g., "Cardiovascular"
//    private Boolean isNegative;        // true = all negative, false = positive findings present
//    private String notes;              // free text for the system
//
//    private List<String> systemDetails; // e.g., ["Chest pain", "Shortness of breath"]
//
//    // client-friendly audit strings (mapped from created_at/updated_at)
//    private String createdDate;        // yyyy-MM-dd
//    private String lastModifiedDate;   // yyyy-MM-dd
//}

package org.ciyex.ehr.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ReviewOfSystemDto {
    private Long id;
    private String externalId;
    private String fhirId;
    private Long patientId;
    private Long encounterId;

    // System categories
    private Constitutional constitutional;
    private Eyes eyes;
    private Ent ent;
    private Neck neck;
    private Cardiovascular cardiovascular;
    private Respiratory respiratory;
    private Gastrointestinal gastrointestinal;
    private GenitourinaryMale genitourinaryMale;
    private GenitourinaryFemale genitourinaryFemale;
    private Musculoskeletal musculoskeletal;
    private Skin skin;
    private Neurologic neurologic;
    private Psychiatric psychiatric;
    private Endocrine endocrine;
    private HematologicLymphatic hematologicLymphatic;
    private AllergicImmunologic allergicImmunologic;

    // server-managed eSign/print
    private Boolean eSigned;
    private OffsetDateTime signedAt;
    private String signedBy;
    private OffsetDateTime printedAt;

    private Audit audit;

    @Data
    public static class Constitutional {
        private Boolean fever;
        private Boolean chills;
        private Boolean nightSweats;
        private Boolean lossOfAppetite;
        private Boolean weightLoss;
        private Boolean weightGain;
        private Boolean fatigue;
        private Boolean weakness;
        private Boolean tiredness;
        private Boolean troubleSleeping;
        private String note;
    }

    @Data
    public static class Eyes {
        private Boolean visionLoss;
        private Boolean doubleVision;
        private Boolean blurredVision;
        private Boolean eyeIrritation;
        private Boolean eyePain;
        private Boolean eyeDischarge;
        private Boolean lightSensitivity;
        private Boolean eyeRedness;
        private String note;
    }

    @Data
    public static class Ent {
        private Boolean earache;
        private Boolean earDischarge;
        private Boolean ringingInEars;
        private Boolean decreasedHearing;
        private Boolean frequentColds;
        private Boolean nasalCongestion;
        private Boolean nosebleeds;
        private Boolean bleedingGums;
        private Boolean difficultySwallowing;
        private Boolean hoarseness;
        private Boolean soreThroat;
        private Boolean dryLips;
        private Boolean redSwollenTongue;
        private Boolean toothAche;
        private Boolean sinusitis;
        private Boolean dryMouth;
        private String note;
    }

    @Data
    public static class Neck {
        private Boolean thyroidEnlargement;
        private Boolean neckPain;
        private String note;
    }

    @Data
    public static class Cardiovascular {
        private Boolean difficultyBreathingAtNight;
        private Boolean chestPain;
        private Boolean irregularHeartbeats;
        private Boolean shortnessOfBreathOnExertion;
        private Boolean palpitations;
        private Boolean difficultyBreathingWhenLyingDown;
        private Boolean rapidHeartbeat;
        private Boolean slowHeartbeat;
        private Boolean lossOfConsciousness;
        private Boolean chestDiscomfort;
        private Boolean chestTightness;
        private Boolean legSwelling;
        private Boolean legCramps;
        private Boolean tortuousLegVeins;
        private String note;
    }

    @Data
    public static class Respiratory {
        private Boolean shortnessOfBreath;
        private Boolean wheezing;
        private Boolean cough;
        private Boolean chestDiscomfort;
        private Boolean snoring;
        private Boolean excessiveSputum;
        private Boolean coughingUpBlood;
        private Boolean painfulBreathing;
        private String note;
    }

    @Data
    public static class Gastrointestinal {
        private Boolean changeInAppetite;
        private Boolean indigestion;
        private Boolean heartburn;
        private Boolean nausea;
        private Boolean vomiting;
        private Boolean excessiveGas;
        private Boolean abdominalPain;
        private Boolean abdominalBloating;
        private Boolean hemorrhoids;
        private Boolean diarrhea;
        private Boolean changeInBowelHabits;
        private Boolean constipation;
        private Boolean blackOrTarryStools;
        private Boolean bloodyStools;
        private Boolean abdominalSwelling;
        private Boolean enlargedLiver;
        private Boolean jaundice;
        private Boolean ascites;
        private Boolean vomitingBlood;
        private Boolean distendedAbdomen;
        private Boolean clayColoredStool;
        private String note;
    }

    @Data
    public static class GenitourinaryMale {
        private Boolean frequentUrination;
        private Boolean bloodInUrine;
        private Boolean foulUrinaryDischarge;
        private Boolean kidneyPain;
        private Boolean urinaryUrgency;
        private Boolean troubleStartingUrine;
        private Boolean inabilityToEmptyBladder;
        private Boolean burningOnUrination;
        private Boolean genitalRashesOrSores;
        private Boolean testicularPainOrMasses;
        private Boolean urinaryRetention;
        private Boolean leakingUrine;
        private Boolean excessiveNightUrination;
        private Boolean urinaryHesitancy;
        private Boolean kidneyStones;
        private Boolean hernia;
        private Boolean penileDischarge;
        private Boolean shortWeakErections;
        private Boolean painfulErection;
        private Boolean decreasedSexualDesire;
        private Boolean prematureEjaculation;
        private String note;
    }

    @Data
    public static class GenitourinaryFemale {
        private Boolean inabilityToControlBladder;
        private Boolean unusualUrinaryColor;
        private Boolean missedPeriods;
        private Boolean excessivelyHeavyPeriods;
        private Boolean lumpsOrSores;
        private Boolean pelvicPain;
        private Boolean urinaryRetention;
        private Boolean vaginalDischarge;
        private Boolean vaginalItching;
        private Boolean vaginalRash;
        private Boolean urinaryFrequency;
        private Boolean urinaryHesitancy;
        private Boolean excessiveNightUrination;
        private Boolean urinaryUrgency;
        private Boolean painfulMenstruation;
        private Boolean irregularMenses;
        private Boolean kidneyStones;
        private String note;
    }

    @Data
    public static class Musculoskeletal {
        private Boolean jointPain;
        private Boolean jointStiffness;
        private Boolean backPain;
        private Boolean muscleCramps;
        private Boolean muscleWeakness;
        private Boolean muscleAches;
        private Boolean lossOfStrength;
        private Boolean neckPain;
        private Boolean swellingHandsFeet;
        private Boolean legCramps;
        private Boolean shoulderPain;
        private Boolean elbowPain;
        private Boolean handPain;
        private Boolean hipPain;
        private Boolean thighPain;
        private Boolean calfPain;
        private Boolean legPain;
        private Boolean wristPain;
        private Boolean fingerPain;
        private Boolean heelPain;
        private Boolean toePain;
        private Boolean anklePain;
        private Boolean kneePain;
        private String note;
    }

    @Data
    public static class Skin {
        private Boolean suspiciousLesions;
        private Boolean excessivePerspiration;
        private Boolean poorWoundHealing;
        private Boolean dryness;
        private Boolean itching;
        private Boolean rash;
        private Boolean flushing;
        private Boolean cyanosis;
        private Boolean clammySkin;
        private Boolean hairLoss;
        private Boolean lumps;
        private Boolean changesInHairOrNails;
        private Boolean skinColorChanges;
        private Boolean jaundice;
        private String note;
    }

    @Data
    public static class Neurologic {
        private Boolean headaches;
        private Boolean poorBalance;
        private Boolean difficultySpeaking;
        private Boolean difficultyConcentrating;
        private Boolean coordinationProblems;
        private Boolean weakness;
        private Boolean briefParalysis;
        private Boolean numbness;
        private Boolean tingling;
        private Boolean visualDisturbances;
        private Boolean seizures;
        private Boolean tremors;
        private Boolean roomSpinning;
        private Boolean memoryLoss;
        private Boolean excessiveDaytimeSleepiness;
        private Boolean dizziness;
        private Boolean facialPain;
        private Boolean lightheadedness;
        private Boolean faintingSpells;
        private Boolean lethargy;
        private Boolean insomnia;
        private Boolean somnolence;
        private Boolean disorientation;
        private String note;
    }

    @Data
    public static class Psychiatric {
        private Boolean anxiety;
        private Boolean nervousness;
        private Boolean depression;
        private Boolean hallucinations;
        private Boolean frighteningVisionsOrSounds;
        private Boolean suicidalIdeation;
        private Boolean homicidalIdeation;
        private Boolean impendingSenseOfDoom;
        private Boolean disturbingThoughts;
        private Boolean memoryLoss;
        private String note;
    }

    @Data
    public static class Endocrine {
        private Boolean heatColdIntolerance;
        private Boolean weightChange;
        private Boolean excessiveThirstOrHunger;
        private Boolean excessiveSweating;
        private Boolean frequentUrination;
        private String note;
    }

    @Data
    public static class HematologicLymphatic {
        private Boolean skinDiscoloration;
        private Boolean easyBleeding;
        private Boolean enlargedLymphNodes;
        private Boolean easyBruising;
        private Boolean anemia;
        private Boolean bloodClots;
        private Boolean swollenGlandsOrThyroid;
        private String note;
    }

    @Data
    public static class AllergicImmunologic {
        private Boolean seasonalAllergies;
        private Boolean hivesOrRash;
        private Boolean persistentInfections;
        private Boolean hivExposure;
        private Boolean immuneDeficiencies;
        private String note;
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
