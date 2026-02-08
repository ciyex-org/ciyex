package org.ciyex.ehr.service.telehealth;

import org.springframework.stereotype.Service;

@Service
public class TelehealthGateway {

    private final TelehealthResolver resolver;

    public TelehealthGateway(TelehealthResolver resolver) {
        this.resolver = resolver;
    }

    public String startVideoCall(Long providerId, Long patientId, String roomName) {
        return resolver.resolve().startVideoCall(providerId, patientId, roomName);
    }

    public String createJoinToken(String roomName, String identity, Integer ttlSecs) {
        return resolver.resolve().createJoinToken(roomName, identity, ttlSecs);
        
        
    }

    public String getCallStatus(String callId) {
        return resolver.resolve().getCallStatus(callId);
    }
}
