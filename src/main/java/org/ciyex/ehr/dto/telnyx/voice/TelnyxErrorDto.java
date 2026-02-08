

package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxErrorDto {
    private List<ErrorDetail> errors;

    public Object getCode() {
        return null;
    }

    public Object getTitle() {
        return null;
    }

    public Object getDetail() {
        return null;
    }

    @Data
    public static class ErrorDetail {
        private Integer code;
        private String title;
        private String detail;
        private ErrorSource source;
    }

    @Data
    public static class ErrorSource {
        private String pointer;
        private String parameter;
    }

}
