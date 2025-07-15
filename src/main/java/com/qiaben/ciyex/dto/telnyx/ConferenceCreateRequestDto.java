package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Body for POST /v2/conferences.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConferenceCreateRequestDto {

    private String callControlId;          // required
    private String name;                   // required

    // optional params
    private String beepEnabled;            // always, never, on_enter, on_exit
    private String clientState;            // base64-encoded
    private Boolean comfortNoise;          // defaults true
    private String commandId;
    private Integer durationMinutes;
    private String holdAudioUrl;
    private String holdMediaName;
    private Integer maxParticipants;       // 2-800, defaults 250
    private Boolean startConferenceOnCreate; // defaults true
}
