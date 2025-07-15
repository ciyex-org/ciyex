// src/main/java/com/qiaben/ciyex/dto/telnyx/TelnyxErrorDto.java
package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

/** 400 / 401 error wrapper (simplified). */
@Data
public class TelnyxErrorDto {
    private int    code;
    private String title;
    private String detail;
}
