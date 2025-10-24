package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.util.TenantContextUtil;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.provider.S3ClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

@StorageType("fhir")
@Component("fhirExternalDocumentStorage")
@Slf4j
public class FhirExternalDocumentStorage implements ExternalStorage<DocumentDto> {

    private final FhirClientProvider fhirClientProvider;
    private final S3ClientProvider s3ClientProvider;

    private static final String EXT_S3_BUCKET = "http://example.org/fhir/s3Bucket";
    private static final String EXT_S3_KEY = "http://example.org/fhir/s3Key";

    @Autowired
    public FhirExternalDocumentStorage(FhirClientProvider fhirClientProvider, S3ClientProvider s3ClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        this.s3ClientProvider = s3ClientProvider;
        log.info("Initializing FhirExternalDocumentStorage");
    }

    @Override
    public String create(DocumentDto dto) {
    String tenantName = TenantContextUtil.getTenantName();
    log.info("Entering create for tenant: {}, fileName: {}", tenantName, dto.getFileName());

        return executeWithRetry(() -> {
            // Compress and upload to S3
            byte[] compressedBytes = compress(dto.getContent());
            String key = "documents/" + (tenantName != null ? tenantName : "unknown") + "/" + dto.getPatientId() + "/" + UUID.randomUUID() + "/" + dto.getFileName() + ".gz";
            String bucket = s3ClientProvider.getBucketForCurrentOrg();
            S3Client s3Client = s3ClientProvider.getForCurrentTenant();
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/gzip")
                    .contentLength((long) compressedBytes.length)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(compressedBytes));
            log.info("Uploaded to S3: bucket={}, key={}, tenant={}", bucket, key, tenantName);

            // Create FHIR DocumentReference
            IGenericClient fhirClient = fhirClientProvider.getForCurrentTenant();
            DocumentReference docRef = mapToFhirDocumentReference(dto, bucket, key);
            String fhirId = fhirClient.create().resource(docRef).execute().getId().getIdPart();
            log.info("Created FHIR DocumentReference: id={}, tenant={}", fhirId, tenantName);

            return fhirId;
        });
    }

    @Override
    public void update(DocumentDto dto, String externalId) {
        throw new UnsupportedOperationException("Document updates not supported");
    }

    @Override
    public DocumentDto get(String externalId) {
    String tenantName = TenantContextUtil.getTenantName();
    log.info("Entering get for tenant: {}, externalId: {}", tenantName, externalId);

        return executeWithRetry(() -> {
            IGenericClient fhirClient = fhirClientProvider.getForCurrentTenant();
            DocumentReference docRef = fhirClient.read().resource(DocumentReference.class).withId(externalId).execute();
            DocumentDto dto = mapFromFhirDocumentReference(docRef);
            // Set S3 paths for download
            docRef.getExtension().forEach(ext -> {
                if (EXT_S3_BUCKET.equals(ext.getUrl())) {
                    dto.setS3Bucket(ext.getValueAsPrimitive().getValueAsString());
                } else if (EXT_S3_KEY.equals(ext.getUrl())) {
                    dto.setS3Key(ext.getValueAsPrimitive().getValueAsString());
                }
            });
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
    String tenantName = TenantContextUtil.getTenantName();
    log.info("Entering delete for tenant: {}, externalId: {}", tenantName, externalId);

        executeWithRetry(() -> {
            IGenericClient fhirClient = fhirClientProvider.getForCurrentTenant();
            DocumentReference docRef = fhirClient.read().resource(DocumentReference.class).withId(externalId).execute();

            String s3Bucket = docRef.getExtensionByUrl(EXT_S3_BUCKET).getValueAsPrimitive().getValueAsString();
            String s3Key = docRef.getExtensionByUrl(EXT_S3_KEY).getValueAsPrimitive().getValueAsString();

            // Delete from S3
            S3Client s3Client = s3ClientProvider.getForCurrentTenant();
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Deleted from S3: bucket={}, key={}, tenant={}", s3Bucket, s3Key, tenantName);

            // Delete from FHIR
            fhirClient.delete().resourceById("DocumentReference", externalId).execute();
            log.info("Deleted FHIR DocumentReference: id={}, tenant={}", externalId, tenantName);

            return null;
        });
    }

    @Override
    public List<DocumentDto> searchAll() {
        throw new UnsupportedOperationException("Search all not implemented for documents");
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return DocumentDto.class.equals(entityType);
    }

    private byte[] compress(byte[] content) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOs = new GZIPOutputStream(baos)) {
            gzipOs.write(content);
            gzipOs.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compress content", e);
        }
    }

    private DocumentReference mapToFhirDocumentReference(DocumentDto dto, String bucket, String key) {
        DocumentReference docRef = new DocumentReference();
        docRef.setSubject(new Reference("Patient/" + dto.getPatientId())); // Using patientId as FHIR ID; adjust if needed
        docRef.addCategory(new CodeableConcept().addCoding(new Coding("http://example.org/category", dto.getCategory(), null)));
        docRef.setType(new CodeableConcept().addCoding(new Coding("http://example.org/type", dto.getType(), null)));
        docRef.setDescription(dto.getDescription());
        DocumentReference.DocumentReferenceContentComponent content = new DocumentReference.DocumentReferenceContentComponent();
        Attachment attachment = new Attachment();
        attachment.setContentType(dto.getContentType());
        attachment.setSize(dto.getContent() != null ? dto.getContent().length : 0);
        attachment.setTitle(dto.getFileName());
        attachment.setCreationElement(new DateTimeType(LocalDateTime.now().toString()));
        content.setAttachment(attachment);
        docRef.addContent(content);
        docRef.addExtension(new Extension(EXT_S3_BUCKET, new StringType(bucket)));
        docRef.addExtension(new Extension(EXT_S3_KEY, new StringType(key)));
        return docRef;
    }

    private DocumentDto mapFromFhirDocumentReference(DocumentReference docRef) {
        DocumentDto dto = new DocumentDto();
        dto.setDescription(docRef.getDescription());
        if (!docRef.getCategory().isEmpty()) {
            dto.setCategory(docRef.getCategoryFirstRep().getCodingFirstRep().getCode());
        }
        if (docRef.getType() != null) {
            dto.setType(docRef.getType().getCodingFirstRep().getCode());
        }
        Attachment attachment = docRef.getContentFirstRep().getAttachment();
        dto.setFileName(attachment.getTitle());
        dto.setContentType(attachment.getContentType());
        return dto;
    }

    private <R> R executeWithRetry(Callable<R> action) {
        int retries = 3;
        for (int i = 0; i < retries; i++) {
            try {
                return action.call();
            } catch (FhirClientConnectionException e) {
                if (i == retries - 1) throw new RuntimeException("Failed after retries", e);
                try {
                    Thread.sleep(1000 * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                throw new RuntimeException("Execution failed", e);
            }
        }
        throw new RuntimeException("Unexpected retry failure");
    }

    @FunctionalInterface
    private interface Callable<R> {
        R call() throws Exception;
    }
}