package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class RecordingTranscriptionListResponseDto {
    private List<RecordingTranscriptionDto> transcriptions;
    private Integer end;
    private String first_page_uri;
    private String previous_page_uri;
    private String next_page_uri;
    private Integer page;
    private Integer page_size;
    private Integer start;
    private String uri;
}
