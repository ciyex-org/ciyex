package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TeXmlConferenceListResponseDto {
    private List<TeXmlConferenceDto> conferences;
    private Integer start;
    private Integer end;
    private Integer page;
    private Integer page_size;
    private String uri;
    private String first_page_uri;
    private String next_page_uri;
}
