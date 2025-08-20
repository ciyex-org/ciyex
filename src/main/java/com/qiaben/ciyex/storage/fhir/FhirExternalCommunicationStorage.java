package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalCommunicationStorage")
@Slf4j
public class FhirExternalCommunicationStorage implements ExternalStorage<CommunicationDto> {

    private final FhirClientProvider fhirClientProvider;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    public FhirExternalCommunicationStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalCommunicationStorage with FhirClientProvider");
    }

    @Override
    public String create(CommunicationDto entityDto) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering create for orgId: {}, sender: {}", orgId, entityDto.getSender());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Communication fhirCommunication = mapToFhirCommunication(entityDto);
            log.debug("Mapped CommunicationDto to FHIR Communication: status={}, category={}", fhirCommunication.getStatus(), fhirCommunication.getCategoryFirstRep());
            String externalId = client.create().resource(fhirCommunication).execute().getId().getIdPart();
            log.info("Created Communication with externalId: {} for orgId: {}", externalId, orgId);
            return externalId;
        });
    }

    @Override
    public void update(CommunicationDto entityDto, String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering update for orgId: {}, externalId: {}, sender: {}", orgId, externalId, entityDto.getSender());
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Communication fhirCommunication = mapToFhirCommunication(entityDto);
            fhirCommunication.setId(externalId);
            log.debug("Updating FHIR Communication with id: {}, status={}, category={}", externalId, fhirCommunication.getStatus(), fhirCommunication.getCategoryFirstRep());
            client.update().resource(fhirCommunication).execute();
            log.info("Updated Communication with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public CommunicationDto get(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering get for orgId: {}, externalId: {}", orgId, externalId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Communication fhirCommunication = client.read().resource(Communication.class).withId(externalId).execute();
            log.debug("Retrieved FHIR Communication with id: {}, status={}", externalId, fhirCommunication.getStatus());
            CommunicationDto communicationDto = mapFromFhirCommunication(fhirCommunication);
            log.info("Retrieved CommunicationDto with externalId: {} for orgId: {}", externalId, orgId);
            log.debug("Mapped CommunicationDto: externalId={}, status={}, category={}", communicationDto.getExternalId(), communicationDto.getStatus(), communicationDto.getCategory());
            return communicationDto;
        });
    }

    @Override
    public void delete(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering delete for orgId: {}, externalId: {}", orgId, externalId);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            log.info("Deleting Communication with externalId: {} for orgId: {}", externalId, orgId);
            client.delete().resourceById("Communication", externalId).execute();
            log.info("Deleted Communication with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public List<CommunicationDto> searchAll() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering searchAll for orgId: {}", orgId);
        if (orgId == null) {
            log.warn("orgId is null in RequestContext, defaulting to no filtering");
        }

        Bundle bundle = fhirClientProvider.getForCurrentOrg().search()
                .forResource(Communication.class)
                .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", orgId != null ? orgId.toString() : ""))
                .returnBundle(Bundle.class)
                .execute();

        log.debug("Received Bundle with {} entries for orgId: {}", bundle.getEntry().size(), orgId);
        List<CommunicationDto> communicationDtos = bundle.getEntry().stream()
                .map(entry -> {
                    Communication communication = (Communication) entry.getResource();
                    log.debug("Processing Communication entry: id={}, status={}", communication.getIdElement().getIdPart(), communication.getStatus());
                    return mapFromFhirCommunication(communication);
                })
                .collect(Collectors.toList());

        return communicationDtos;
    }

    public List<CommunicationDto> searchAllWithPaging(String orgId, int offset, int count, boolean sortDesc) {
        Bundle bundle = fhirClientProvider.getForCurrentOrg().search()
                .forResource(Communication.class)
                .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", orgId))
                .sort(sortDesc ? new SortSpec().setOrder(SortOrderEnum.DESC).setParamName(Communication.SP_SENT) : null)
                .offset(offset)
                .count(count)
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().stream()
                .map(entry -> mapFromFhirCommunication((Communication) entry.getResource()))
                .collect(Collectors.toList());
    }

    public long countAllForOrg(String orgId) {
        Bundle bundle = fhirClientProvider.getForCurrentOrg().search()
                .forResource(Communication.class)
                .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", orgId))
                .summaryMode(SummaryEnum.COUNT)
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getTotal();
    }

    public List<CommunicationDto> searchForPatient(String patientExternalId, int offset, int count, boolean sortDesc) {
        IGenericClient client = fhirClientProvider.getForCurrentOrg();
        List<Communication> allCommunications = new ArrayList<>();

        // Fetch all for recipient
        Bundle bundleRec = client.search().forResource(Communication.class)
                .where(Communication.RECIPIENT.hasId("Patient/" + patientExternalId))
                .sort(sortDesc ? new SortSpec().setOrder(SortOrderEnum.DESC).setParamName(Communication.SP_SENT) : new SortSpec().setOrder(SortOrderEnum.ASC).setParamName(Communication.SP_SENT))
                .returnBundle(Bundle.class)
                .execute();
        loadAllPages(client, bundleRec, allCommunications);

        // Fetch all for subject
        Bundle bundleSub = client.search().forResource(Communication.class)
                .where(Communication.SUBJECT.hasId("Patient/" + patientExternalId))
                .sort(sortDesc ? new SortSpec().setOrder(SortOrderEnum.DESC).setParamName(Communication.SP_SENT) : new SortSpec().setOrder(SortOrderEnum.ASC).setParamName(Communication.SP_SENT))
                .returnBundle(Bundle.class)
                .execute();
        loadAllPages(client, bundleSub, allCommunications);

        // Unique by ID and sort
        Map<String, Communication> uniqueMap = new LinkedHashMap<>();
        Comparator<Communication> comparator = Comparator.comparing(comm -> comm.getSent() != null ? comm.getSent() : new Date(0));
        if (sortDesc) {
            comparator = comparator.reversed();
        }
        allCommunications.stream()
                .sorted(comparator)
                .forEach(comm -> uniqueMap.put(comm.getIdElement().getIdPart(), comm));

        // Apply offset and count
        List<Communication> paged = new ArrayList<>(uniqueMap.values()).subList(Math.min(offset, uniqueMap.size()), Math.min(offset + count, uniqueMap.size()));

        return paged.stream()
                .map(this::mapFromFhirCommunication)
                .collect(Collectors.toList());
    }

    public long countForPatient(String patientExternalId) {
        IGenericClient client = fhirClientProvider.getForCurrentOrg();
        long countRec = client.search().forResource(Communication.class)
                .where(Communication.RECIPIENT.hasId("Patient/" + patientExternalId))
                .summaryMode(SummaryEnum.COUNT)
                .returnBundle(Bundle.class)
                .execute().getTotal();
        long countSub = client.search().forResource(Communication.class)
                .where(Communication.SUBJECT.hasId("Patient/" + patientExternalId))
                .summaryMode(SummaryEnum.COUNT)
                .returnBundle(Bundle.class)
                .execute().getTotal();
        long countBoth = client.search().forResource(Communication.class)
                .where(Communication.RECIPIENT.hasId("Patient/" + patientExternalId))
                .where(Communication.SUBJECT.hasId("Patient/" + patientExternalId))
                .summaryMode(SummaryEnum.COUNT)
                .returnBundle(Bundle.class)
                .execute().getTotal();
        return countRec + countSub - countBoth;
    }

    public List<CommunicationDto> searchForProvider(String providerExternalId, int offset, int count, boolean sortDesc) {
        IGenericClient client = fhirClientProvider.getForCurrentOrg();
        List<Communication> allCommunications = new ArrayList<>();

        // Fetch all for sender
        Bundle bundleSender = client.search().forResource(Communication.class)
                .where(Communication.SENDER.hasId("Provider/" + providerExternalId))
                .sort(sortDesc ? new SortSpec().setOrder(SortOrderEnum.DESC).setParamName(Communication.SP_SENT) : new SortSpec().setOrder(SortOrderEnum.ASC).setParamName(Communication.SP_SENT))
                .returnBundle(Bundle.class)
                .execute();
        loadAllPages(client, bundleSender, allCommunications);

        // Fetch all for recipient
        Bundle bundleRec = client.search().forResource(Communication.class)
                .where(Communication.RECIPIENT.hasId("Provider/" + providerExternalId))
                .sort(sortDesc ? new SortSpec().setOrder(SortOrderEnum.DESC).setParamName(Communication.SP_SENT) : new SortSpec().setOrder(SortOrderEnum.ASC).setParamName(Communication.SP_SENT))
                .returnBundle(Bundle.class)
                .execute();
        loadAllPages(client, bundleRec, allCommunications);

        // Unique by ID and sort
        Map<String, Communication> uniqueMap = new LinkedHashMap<>();
        Comparator<Communication> comparator = Comparator.comparing(comm -> comm.getSent() != null ? comm.getSent() : new Date(0));
        if (sortDesc) {
            comparator = comparator.reversed();
        }
        allCommunications.stream()
                .sorted(comparator)
                .forEach(comm -> uniqueMap.put(comm.getIdElement().getIdPart(), comm));

        // Apply offset and count
        List<Communication> paged = new ArrayList<>(uniqueMap.values()).subList(Math.min(offset, uniqueMap.size()), Math.min(offset + count, uniqueMap.size()));

        return paged.stream()
                .map(this::mapFromFhirCommunication)
                .collect(Collectors.toList());
    }

    public long countForProvider(String providerExternalId) {
        IGenericClient client = fhirClientProvider.getForCurrentOrg();
        long countSender = client.search().forResource(Communication.class)
                .where(Communication.SENDER.hasId("Provider/" + providerExternalId))
                .summaryMode(SummaryEnum.COUNT)
                .returnBundle(Bundle.class)
                .execute().getTotal();
        long countRec = client.search().forResource(Communication.class)
                .where(Communication.RECIPIENT.hasId("Provider/" + providerExternalId))
                .summaryMode(SummaryEnum.COUNT)
                .returnBundle(Bundle.class)
                .execute().getTotal();
        long countBoth = client.search().forResource(Communication.class)
                .where(Communication.SENDER.hasId("Provider/" + providerExternalId))
                .where(Communication.RECIPIENT.hasId("Provider/" + providerExternalId))
                .summaryMode(SummaryEnum.COUNT)
                .returnBundle(Bundle.class)
                .execute().getTotal();
        return countSender + countRec - countBoth;
    }

    public List<CommunicationDto> searchForChildren(String externalId) {
        IGenericClient client = fhirClientProvider.getForCurrentOrg();
        Bundle bundle = client.search().forResource(Communication.class)
                .where(new ReferenceClientParam("in-response-to").hasId("Communication/" + externalId))
                .sort(new SortSpec().setOrder(SortOrderEnum.DESC).setParamName(Communication.SP_SENT))
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().stream()
                .map(entry -> mapFromFhirCommunication((Communication) entry.getResource()))
                .collect(Collectors.toList());
    }

    private void loadAllPages(IGenericClient client, Bundle bundle, List<Communication> list) {
        Bundle currentBundle = bundle;
        while (currentBundle != null) {
            currentBundle.getEntry().stream()
                    .map(entry -> (Communication) entry.getResource())
                    .forEach(list::add);
            if (currentBundle.getLink(Bundle.LINK_NEXT) != null) {
                currentBundle = client.loadPage().next(currentBundle).execute();
            } else {
                currentBundle = null;
            }
        }
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return CommunicationDto.class.equals(entityType);
    }

    private <R> R executeWithRetry(java.util.function.Supplier<R> action) {
        try {
            return action.get();
        } catch (FhirClientConnectionException e) {
            log.warn("FHIR client connection issue, retrying: {}", e.getMessage());
            return action.get();
        } catch (Exception e) {
            log.error("Failed FHIR operation: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Communication mapToFhirCommunication(CommunicationDto dto) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        Communication fhirCommunication = new Communication();
        fhirCommunication.getMeta().addTag().setSystem("http://ciyex.com/tenant").setCode(orgId != null ? orgId.toString() : null);

        if (dto.getStatus() != null) {
            fhirCommunication.setStatus(Communication.CommunicationStatus.fromCode(dto.getStatus().toLowerCase()));
        }
        if (dto.getCategory() != null) {
            fhirCommunication.addCategory(new CodeableConcept().addCoding(new Coding().setCode(dto.getCategory())));
        }
        if (dto.getSentDate() != null) {
            try {
                Date sentDate = DATE_FORMAT.parse(dto.getSentDate());
                fhirCommunication.setSent(sentDate);
            } catch (Exception e) {
                log.warn("Failed to parse sentDate: {}, using null", dto.getSentDate(), e);
            }
        }
        if (dto.getSender() != null) {
            fhirCommunication.setSender(new Reference(dto.getSender()));
        }
        if (dto.getRecipients() != null) {
            dto.getRecipients().forEach(rec -> fhirCommunication.addRecipient(new Reference(rec)));
        }
        if (dto.getSubject() != null) {
            fhirCommunication.setSubject(new Reference(dto.getSubject()));
        }
        if (dto.getInResponseTo() != null) {
            fhirCommunication.addInResponseTo(new Reference(dto.getInResponseTo()));
        }
        if (dto.getPayload() != null) {
            fhirCommunication.addPayload().setContent(new StringType(dto.getPayload()));
        }

        log.debug("Mapped FHIR Communication for orgId: {}, status: {}, category: {}", orgId, dto.getStatus(), dto.getCategory());
        return fhirCommunication;
    }

    private CommunicationDto mapFromFhirCommunication(Communication fhirCommunication) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping FHIR Communication to CommunicationDto for orgId: {}, id: {}, status: {}", orgId, fhirCommunication.getIdElement().getIdPart(), fhirCommunication.getStatus());
        CommunicationDto dto = new CommunicationDto();
        dto.setExternalId(fhirCommunication.getIdElement().getIdPart());
        dto.setStatus(fhirCommunication.getStatus() != null ? fhirCommunication.getStatus().name() : null);
        dto.setCategory(fhirCommunication.getCategoryFirstRep() != null ? fhirCommunication.getCategoryFirstRep().getCodingFirstRep().getCode() : null);
        dto.setSentDate(fhirCommunication.getSent() != null ? DATE_FORMAT.format(fhirCommunication.getSent()) : null);
        dto.setSender(fhirCommunication.getSender() != null ? fhirCommunication.getSender().getReference() : null);
        dto.setRecipients(fhirCommunication.getRecipient().stream().map(Reference::getReference).collect(Collectors.toList()));
        dto.setSubject(fhirCommunication.getSubject() != null ? fhirCommunication.getSubject().getReference() : null);
        dto.setInResponseTo(fhirCommunication.getInResponseToFirstRep() != null ? fhirCommunication.getInResponseToFirstRep().getReference() : null);
        dto.setPayload(fhirCommunication.getPayloadFirstRep() != null && fhirCommunication.getPayloadFirstRep().getContent() instanceof StringType ?
                ((StringType) fhirCommunication.getPayloadFirstRep().getContent()).getValue() : null);
        dto.setOrgId(orgId);

        log.debug("Mapped CommunicationDto for orgId: {}, externalId: {}, status: {}, category: {}", orgId, dto.getExternalId(), dto.getStatus(), dto.getCategory());
        return dto;
    }
}