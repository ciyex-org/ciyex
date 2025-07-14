package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class TeXmlParticipantListResponseDto {
    private List<TeXmlParticipantDto> participants;
    private Integer start;
    private Integer end;
    private Integer page;
    private Integer page_size;
    private String uri;
    private String first_page_uri;
    private String next_page_uri;
}
