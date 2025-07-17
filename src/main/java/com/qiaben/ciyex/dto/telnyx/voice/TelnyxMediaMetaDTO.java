package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxMediaMetaDTO {
    private int total_pages;
    private int total_results;
    private int page_number;
    private int page_size;
}