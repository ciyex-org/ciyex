//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.ScheduleDto;
//import com.qiaben.ciyex.dto.integration.RequestContext;
//import com.qiaben.ciyex.entity.ProviderSchedule;
//import com.qiaben.ciyex.repository.ProviderScheduleRepository;
//import com.qiaben.ciyex.storage.ExternalStorage;
//import com.qiaben.ciyex.storage.ExternalStorageResolver;
//import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//public class ProviderScheduleService {
//
//    private final ProviderScheduleRepository repository;
//    private final ExternalStorageResolver storageResolver;
//    private final OrgIntegrationConfigProvider configProvider;
//
//    public ProviderScheduleService(ProviderScheduleRepository repository,
//                                   ExternalStorageResolver storageResolver,
//                                   OrgIntegrationConfigProvider configProvider) {
//        this.repository = repository;
//        this.storageResolver = storageResolver;
//        this.configProvider = configProvider;
//    }
//
//    @Transactional
//    public ScheduleDto create(ScheduleDto dto) {
//        Long orgId = getCurrentOrgId();
//        if (orgId == null) throw new SecurityException("No orgId available in request context");  // same guard :contentReference[oaicite:4]{index=4}L27-L35
//
//        dto.setOrgId(orgId);
//
//        // (Optional validations)
//        if (dto.getProviderId() == null || dto.getStartDate() == null || dto.getStartTime() == null
//                || dto.getFrequency() == null || dto.getInterval() == null || dto.getDurationMin() == null) {
//            throw new IllegalArgumentException("providerId, startDate, startTime, frequency, interval, durationMin are required");
//        }
//
//        ProviderSchedule entity = toEntity(dto);
//        entity.setOrgId(orgId);
//        entity.setCreatedDate(LocalDateTime.now().toString());
//        entity.setLastModifiedDate(LocalDateTime.now().toString());
//
//        // External storage first (pattern like ProviderService.create) :contentReference[oaicite:5]{index=5}L39-L73
//        String storageType = configProvider.getStorageTypeForCurrentOrg();
//        String externalId = null;
//        if (storageType != null) {
//            ExternalStorage<ScheduleDto> ext = storageResolver.resolve(ScheduleDto.class);
//            externalId = ext.create(dto); // let external system create a shadow
//            log.info("Created schedule in external storage, externalId={}", externalId);
//        }
//
//        ProviderSchedule saved = repository.save(entity);
//        return toDto(saved);
//    }
//
//    @Transactional(readOnly = true)
//    public ScheduleDto getById(Long id) {
//        Long orgId = getCurrentOrgId();
//        if (orgId == null) throw new SecurityException("No orgId available in request context");   // same pattern :contentReference[oaicite:6]{index=6}L86-L97
//
//        ProviderSchedule s = repository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
//
//        if (!orgId.equals(s.getOrgId())) {
//            throw new SecurityException("Access denied: Schedule id " + id + " does not belong to orgId " + orgId);
//        }
//
//        ScheduleDto dto = toDto(s);
//
//        // Try to enrich from external storage (like provider getById) :contentReference[oaicite:7]{index=7}L106-L150
//        String storageType = configProvider.getStorageTypeForCurrentOrg();
//        if (storageType != null) {
//            try {
//                ExternalStorage<ScheduleDto> ext = storageResolver.resolve(ScheduleDto.class);
//                // if you store an externalId on schedule later, retrieve & merge here
//                // ScheduleDto extended = ext.get(externalId);
//            } catch (Exception e) {
//                log.warn("External schedule fetch failed: {}", e.getMessage());
//            }
//        }
//        return dto;
//    }
//
//    @Transactional
//    public ScheduleDto update(Long id, ScheduleDto dto) {
//        Long orgId = getCurrentOrgId();
//        if (orgId == null) throw new SecurityException("No orgId available in request context");   // same pattern :contentReference[oaicite:8]{index=8}L170-L181
//
//        ProviderSchedule s = repository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
//
//        if (!orgId.equals(s.getOrgId())) {
//            throw new SecurityException("Access denied: Schedule id " + id + " does not belong to orgId " + orgId);
//        }
//
//        // Map updates
//        s.setTitle(dto.getTitle());
//        s.setLocation(dto.getLocation());
//        s.setProviderId(dto.getProviderId());
//        s.setStartDate(dto.getStartDate());
//        s.setEndDate(dto.getEndDate());
//        s.setStartTime(dto.getStartTime());
//        s.setDurationMin(dto.getDurationMin());
//        s.setMaxOccurrences(dto.getMaxOccurrences());
//        s.setFrequency(dto.getFrequency());
//        s.setIntervalVal(dto.getInterval());
//        s.setWeeklyDays(dto.getWeeklyDays());
//        s.setStatus(dto.getStatus());
//        s.setLastModifiedDate(LocalDateTime.now().toString());
//
//        // Push external first (like ProviderService.update) :contentReference[oaicite:9]{index=9}L199-L216
//        String storageType = configProvider.getStorageTypeForCurrentOrg();
//        if (storageType != null) {
//            ExternalStorage<ScheduleDto> ext = storageResolver.resolve(ScheduleDto.class);
//            // ext.update(dto, externalId); // if/when you persist externalId on the schedule
//        }
//
//        return toDto(repository.save(s));
//    }
//
//    @Transactional
//    public void delete(Long id) {
//        Long orgId = getCurrentOrgId();
//        if (orgId == null) throw new SecurityException("No orgId available in request context");   // same pattern :contentReference[oaicite:10]{index=10}L225-L237
//
//        ProviderSchedule s = repository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
//        if (!orgId.equals(s.getOrgId())) {
//            throw new SecurityException("Access denied: Schedule id " + id + " does not belong to orgId " + orgId);
//        }
//
//        String storageType = configProvider.getStorageTypeForCurrentOrg();
//        if (storageType != null) {
//            ExternalStorage<ScheduleDto> ext = storageResolver.resolve(ScheduleDto.class);
//            // ext.delete(externalId); // if/when externalId stored
//        }
//
//        repository.delete(s);
//    }
//
//    @Transactional(readOnly = true)
//    public ApiResponse<List<ScheduleDto>> listByProvider(Long providerId) {
//        Long orgId = getCurrentOrgId();
//        List<ProviderSchedule> list = repository.findByOrgIdAndProviderId(orgId, providerId);
//        return ApiResponse.<List<ScheduleDto>>builder()
//                .success(true)
//                .message("Schedules retrieved successfully")
//                .data(list.stream().map(this::toDto).collect(Collectors.toList()))
//                .build();
//    }
//
//    @Transactional(readOnly = true)
//    public ApiResponse<List<ScheduleDto>> listAll() {
//        Long orgId = getCurrentOrgId();
//        List<ProviderSchedule> list = repository.findAllByOrgId(orgId);         // mirrors ProviderService.getAllProviders :contentReference[oaicite:11]{index=11}L268-L288
//        return ApiResponse.<List<ScheduleDto>>builder()
//                .success(true)
//                .message("Schedules retrieved successfully")
//                .data(list.stream().map(this::toDto).collect(Collectors.toList()))
//                .build();
//    }
//
//    @Transactional(readOnly = true)
//    public long countByOrg() {
//        return repository.countByOrgId(getCurrentOrgId());                       // mirrors ProviderService.getProviderCountByOrgId :contentReference[oaicite:12]{index=12}L330-L335
//    }
//
//    /* ---------- Mappers ---------- */
//
//    private ProviderSchedule toEntity(ScheduleDto dto) {
//        return ProviderSchedule.builder()
//                .id(dto.getId())
//                .orgId(dto.getOrgId())
//                .providerId(dto.getProviderId())
//                .title(dto.getTitle())
//                .location(dto.getLocation())
//                .startDate(dto.getStartDate())
//                .endDate(dto.getEndDate())
//                .startTime(dto.getStartTime())
//                .durationMin(dto.getDurationMin())
//                .maxOccurrences(dto.getMaxOccurrences())
//                .frequency(dto.getFrequency())
//                .intervalVal(dto.getInterval())
//                .weeklyDays(dto.getWeeklyDays())
//                .status(dto.getStatus())
//                .build();
//    }
//
//    private ScheduleDto toDto(ProviderSchedule s) {
//        ScheduleDto dto = new ScheduleDto();
//        dto.setId(s.getId());
//        dto.setOrgId(s.getOrgId());
//        dto.setProviderId(s.getProviderId());
//        dto.setTitle(s.getTitle());
//        dto.setLocation(s.getLocation());
//        dto.setStartDate(s.getStartDate());
//        dto.setEndDate(s.getEndDate());
//        dto.setStartTime(s.getStartTime());
//        dto.setDurationMin(s.getDurationMin());
//        dto.setMaxOccurrences(s.getMaxOccurrences());
//        dto.setFrequency(s.getFrequency());
//        dto.setInterval(s.getIntervalVal());
//        dto.setWeeklyDays(s.getWeeklyDays());
//        dto.setStatus(s.getStatus());
//        return dto;
//    }
//
//    private Long getCurrentOrgId() {
//        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;       // same helper :contentReference[oaicite:13]{index=13}L316-L328
//        if (orgId == null) log.warn("orgId is null in RequestContext");
//        return orgId;
//    }
//}
