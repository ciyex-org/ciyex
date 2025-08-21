package com.qiaben.ciyex.service.telehealth;

public interface TelehealthService {

    /**
     * Initiates a video call for a scheduled slot between a provider and a patient.
     *
     * @param providerId The ID of the provider
     * @param patientId The ID of the patient
     * @param roomName A unique name for the video call room
     * @return A call identifier (e.g., room SID or credential ID) for joining the call
     */
    String startVideoCall(Long providerId, Long patientId, String roomName);

    /**
     * Sends an SMS reminder to a patient for a scheduled slot.
     *
     * @param phoneNumber The patient's phone number
     * @param message The reminder message
     */
    void sendSMSReminder(String phoneNumber, String message);

    /**
     * Retrieves the status of an ongoing or completed call.
     *
     * @param callId The identifier of the call (e.g., room SID or credential ID)
     * @return The current status of the call (e.g., "active", "completed")
     */
    String getCallStatus(String callId);
}