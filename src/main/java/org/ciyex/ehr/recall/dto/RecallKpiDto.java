package org.ciyex.ehr.recall.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallKpiDto {
    private long dueToday;
    private long overdue;
    private long completedThisMonth;
    private long pendingTotal;
    private long contactedTotal;
    private long scheduledTotal;
    private long cancelledTotal;
    private double complianceRate; // completedThisMonth / (completedThisMonth + overdue) * 100
}
