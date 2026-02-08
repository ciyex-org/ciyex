package org.ciyex.ehr.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;

/**
 * Minimal but extensible representation of the enormous Dial payload.
 * Add / remove fields as you need – everything is already serialisable by Jackson.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxDialRequestDTO {

    @NonNull
    private Object to;                       // String or List<String>

    @NonNull
    private String from;

    @Builder.Default
    private String connection_id = "";

    private String from_display_name;
    private String audio_url;
    private String media_name;
    private String preferred_codecs;
    private Integer timeout_secs;
    private Integer time_limit_secs;

    // grab bag for any parameter you don't want explicitly modelled out
    private Map<String, Object> extra;
}
