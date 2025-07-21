package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxTeXmlCallRecordingListResponseDto {
    private List<TelnyxTeXmlCallRecordingResponseDto> recordings;
    private Integer end;
    private String first_page_uri;
    private String previous_page_uri;
    private String next_page_uri;
    private Integer page;
    private Integer page_size;
    private Integer start;
    private String uri;
}
