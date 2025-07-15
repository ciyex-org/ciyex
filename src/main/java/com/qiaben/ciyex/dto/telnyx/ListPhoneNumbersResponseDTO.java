package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class ListPhoneNumbersResponseDTO {
    private List<MessagingPhoneNumberDTO> data;
    private Meta meta;

    @Data
    public static class Meta {
        private Integer totalPages;
        private Integer totalResults;
        private Integer pageNumber;
        private Integer pageSize;
    }
}

