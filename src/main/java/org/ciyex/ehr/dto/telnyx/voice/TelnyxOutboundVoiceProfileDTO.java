package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxOutboundVoiceProfileDTO {
    private Long id;
    private String record_type;
    private String name;
    private Integer connections_count;
    private String traffic_type;
    private String service_plan;
    private Integer concurrent_call_limit;
    private Boolean enabled;
    private List<String> tags;
    private String usage_payment_method;
    private List<String> whitelisted_destinations;
    private Double max_destination_rate;
    private String daily_spend_limit;
    private Boolean daily_spend_limit_enabled;
    private CallRecording call_recording;
    private String billing_group_id;
    private String created_at;
    private String updated_at;

    @Data
    public static class CallRecording {
        private String call_recording_type;
        private List<String> call_recording_caller_phone_numbers;
        private String call_recording_channels;
        private String call_recording_format;
    }
}
