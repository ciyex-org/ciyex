package org.ciyex.ehr.inventory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvCategoryDto {
    private Long id;
    private String name;
    private Long parentId;
    private String parentName;
}
