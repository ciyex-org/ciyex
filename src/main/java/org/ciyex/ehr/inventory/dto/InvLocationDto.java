package org.ciyex.ehr.inventory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvLocationDto {
    private Long id;
    private String name;
    private String type;
    private Long parentId;
    private String parentName;
}
