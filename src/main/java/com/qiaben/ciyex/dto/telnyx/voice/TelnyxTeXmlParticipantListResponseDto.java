package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxTeXmlParticipantListResponseDto {
    private List<TelnyxTeXmlParticipantDto> participants;
    private Integer start;
    private Integer end;
    private Integer page;
    private Integer page_size;
    private String uri;
    private String first_page_uri;
    private String next_page_uri;
}
