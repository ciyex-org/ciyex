package org.ciyex.ehr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyOrderCountDto {
    private Integer month;
    private Long count;
}
