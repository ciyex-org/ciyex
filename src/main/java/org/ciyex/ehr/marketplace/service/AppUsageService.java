package org.ciyex.ehr.marketplace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.marketplace.dto.AppUsageSummary;
import org.ciyex.ehr.marketplace.dto.RecordUsageRequest;
import org.ciyex.ehr.marketplace.entity.AppUsageDaily;
import org.ciyex.ehr.marketplace.entity.AppUsageEvent;
import org.ciyex.ehr.marketplace.repository.AppUsageDailyRepository;
import org.ciyex.ehr.marketplace.repository.AppUsageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks app usage events for metering and analytics.
 *
 * Event types:
 *   - app_launch: User launched an app (SMART or native)
 *   - plugin_render: A plugin component was rendered in a slot
 *   - cds_hook_invocation: A CDS Hooks service was invoked
 *   - smart_launch: A SMART on FHIR launch was initiated
 *   - api_call: An API call was made on behalf of an app
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppUsageService {

    private final AppUsageEventRepository eventRepository;
    private final AppUsageDailyRepository dailyRepository;

    /**
     * Record a single usage event. Fire-and-forget — non-blocking for the caller.
     */
    @Transactional
    public void recordEvent(String orgId, String appSlug, String eventType, String eventDetail,
                            String userId, UUID patientId, UUID encounterId) {
        AppUsageEvent event = AppUsageEvent.builder()
                .orgId(orgId)
                .appSlug(appSlug)
                .eventType(eventType)
                .eventDetail(eventDetail)
                .userId(userId)
                .patientId(patientId)
                .encounterId(encounterId)
                .build();
        eventRepository.save(event);
    }

    /**
     * Record a usage event from an API request.
     */
    @Transactional
    public void recordEvent(String orgId, RecordUsageRequest request) {
        AppUsageEvent event = AppUsageEvent.builder()
                .orgId(orgId)
                .appSlug(request.getAppSlug())
                .eventType(request.getEventType())
                .eventDetail(request.getEventDetail())
                .userId(request.getUserId())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .build();
        eventRepository.save(event);
    }

    /**
     * Get usage summary for all apps in an org within a time range.
     */
    @Transactional(readOnly = true)
    public List<AppUsageSummary> getUsageSummary(String orgId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = eventRepository.countByAppAndType(orgId, since);

        Map<String, AppUsageSummary> summaryMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String appSlug = (String) row[0];
            String eventType = (String) row[1];
            long count = ((Number) row[2]).longValue();

            summaryMap.computeIfAbsent(appSlug, slug -> AppUsageSummary.builder()
                    .appSlug(slug)
                    .periodDays(days)
                    .eventBreakdown(new LinkedHashMap<>())
                    .totalEvents(0)
                    .build());
            AppUsageSummary summary = summaryMap.get(appSlug);
            summary.getEventBreakdown().put(eventType, count);
            summary.setTotalEvents(summary.getTotalEvents() + count);
        }

        return new ArrayList<>(summaryMap.values());
    }

    /**
     * Get daily usage trend for a specific app.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailyTrend(String orgId, String appSlug, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = eventRepository.dailyTrend(orgId, appSlug, since);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", row[0].toString());
            point.put("eventType", row[1]);
            point.put("count", ((Number) row[2]).longValue());
            trend.add(point);
        }
        return trend;
    }

    /**
     * Get total event count for an app in an org.
     */
    @Transactional(readOnly = true)
    public long getEventCount(String orgId, String appSlug, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return eventRepository.countByOrgAndApp(orgId, appSlug, since);
    }

    /**
     * Aggregate raw events into daily summaries.
     * Called periodically by a scheduler or manual trigger.
     */
    @Transactional
    public int aggregateDaily() {
        List<AppUsageEvent> unreported = eventRepository.findUnreported();
        if (unreported.isEmpty()) return 0;

        // Group by org + app + eventType + date
        Map<String, List<AppUsageEvent>> groups = unreported.stream()
                .collect(Collectors.groupingBy(e ->
                        e.getOrgId() + "|" + e.getAppSlug() + "|" + e.getEventType() + "|" + e.getRecordedAt().toLocalDate()));

        int aggregated = 0;
        for (Map.Entry<String, List<AppUsageEvent>> entry : groups.entrySet()) {
            List<AppUsageEvent> events = entry.getValue();
            AppUsageEvent sample = events.get(0);
            LocalDate date = sample.getRecordedAt().toLocalDate();

            long totalCount = events.stream().mapToLong(e -> e.getQuantity() != null ? e.getQuantity() : 1).sum();
            int uniqueUsers = (int) events.stream()
                    .map(AppUsageEvent::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            // Upsert daily summary
            AppUsageDaily daily = dailyRepository.findByOrgIdAndAppSlugAndEventTypeAndUsageDate(
                    sample.getOrgId(), sample.getAppSlug(), sample.getEventType(), date)
                    .orElse(AppUsageDaily.builder()
                            .orgId(sample.getOrgId())
                            .appSlug(sample.getAppSlug())
                            .eventType(sample.getEventType())
                            .usageDate(date)
                            .totalCount(0L)
                            .uniqueUsers(0)
                            .build());

            daily.setTotalCount(daily.getTotalCount() + totalCount);
            daily.setUniqueUsers(Math.max(daily.getUniqueUsers(), uniqueUsers));
            dailyRepository.save(daily);
            aggregated++;
        }

        // Mark events as reported
        List<UUID> ids = unreported.stream().map(AppUsageEvent::getId).toList();
        eventRepository.markReported(ids);

        log.info("Aggregated {} daily usage summaries from {} events", aggregated, unreported.size());
        return aggregated;
    }
}
