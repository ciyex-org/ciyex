package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.MessageAttachmentDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Message Attachment Service.
 * Uses FHIR DocumentReference resource for storing message attachments.
 * No S3 storage or local database - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageAttachmentService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String DOC_TYPE_SYSTEM = "http://ciyex.com/fhir/document-type";
    private static final String DOC_TYPE_CODE = "message-attachment";
    private static final String EXT_MESSAGE_ID = "http://ciyex.com/fhir/StructureDefinition/message-id";
    private static final String EXT_CATEGORY = "http://ciyex.com/fhir/StructureDefinition/category";
    private static final String EXT_TYPE = "http://ciyex.com/fhir/StructureDefinition/attachment-type";

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public MessageAttachmentDto create(Long messageId, MessageAttachmentDto dto, MultipartFile file) {
        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File exceeds max upload size (" + MAX_FILE_SIZE + " bytes)");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new RuntimeException("Invalid file name");
        }

        log.debug("Creating FHIR DocumentReference (MessageAttachment) for message {}", messageId);

        try {
            DocumentReference doc = new DocumentReference();
            doc.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

            // Type to identify as message attachment
            CodeableConcept type = new CodeableConcept();
            type.addCoding().setSystem(DOC_TYPE_SYSTEM).setCode(DOC_TYPE_CODE).setDisplay("Message Attachment");
            doc.setType(type);

            // Description
            doc.setDescription(dto.getDescription() != null ? dto.getDescription() : fileName);

            // Message ID extension
            doc.addExtension(new Extension(EXT_MESSAGE_ID, new StringType(messageId.toString())));

            // Category extension
            if (dto.getCategory() != null) {
                doc.addExtension(new Extension(EXT_CATEGORY, new StringType(dto.getCategory())));
            }

            // Type extension
            if (dto.getType() != null) {
                doc.addExtension(new Extension(EXT_TYPE, new StringType(dto.getType())));
            }

            // Content with embedded data
            DocumentReference.DocumentReferenceContentComponent content = doc.addContent();
            Attachment attachment = new Attachment();
            attachment.setContentType(file.getContentType());
            attachment.setData(file.getBytes());
            attachment.setTitle(fileName);
            attachment.setSize((int) file.getSize());
            content.setAttachment(attachment);

            // Create in FHIR
            var outcome = fhirClientService.create(doc, getPracticeId());
            String fhirId = outcome.getId().getIdPart();

            dto.setId((long) Math.abs(fhirId.hashCode()));
            dto.setMessageId(messageId);
            dto.setFileName(fileName);
            dto.setContentType(file.getContentType());
            dto.setContent(null); // Don't return content in response
            dto.setEncrypted(false);

            log.info("Created FHIR DocumentReference (MessageAttachment) with id: {}", fhirId);
            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create message attachment", e);
        }
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting message attachment: {}", fhirId);
        fhirClientService.delete(DocumentReference.class, fhirId, getPracticeId());
    }

    // DOWNLOAD
    public DownloadResult download(String fhirId) {
        log.debug("Downloading message attachment: {}", fhirId);
        DocumentReference doc = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());

        if (doc.hasContent() && !doc.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = doc.getContent().get(0);
            if (content.hasAttachment()) {
                Attachment att = content.getAttachment();
                byte[] data = att.hasData() ? att.getData() : new byte[0];
                String contentType = att.hasContentType() ? att.getContentType() : "application/octet-stream";
                String fileName = att.hasTitle() ? att.getTitle() : "attachment";

                return new DownloadResult(
                        new ByteArrayInputStream(data),
                        contentType,
                        fileName
                );
            }
        }

        throw new RuntimeException("Message attachment not found or has no content");
    }

    // GET ALL FOR MESSAGE
    public ApiResponse<List<MessageAttachmentDto>> getAllForMessage(Long messageId) {
        log.debug("Getting all attachments for message: {}", messageId);
        Bundle bundle = fhirClientService.search(DocumentReference.class, getPracticeId());

        List<MessageAttachmentDto> dtos = fhirClientService.extractResources(bundle, DocumentReference.class).stream()
                .filter(this::isMessageAttachment)
                .filter(doc -> messageId.equals(getMessageId(doc)))
                .map(this::fromFhirDocumentReference)
                .collect(Collectors.toList());

        return ApiResponse.<List<MessageAttachmentDto>>builder()
                .success(true)
                .message("Message attachments retrieved successfully")
                .data(dtos)
                .build();
    }

    // GET BY ID
    public MessageAttachmentDto getById(String fhirId) {
        log.debug("Getting message attachment: {}", fhirId);
        DocumentReference doc = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
        return fromFhirDocumentReference(doc);
    }

    // -------- FHIR Mapping --------

    private MessageAttachmentDto fromFhirDocumentReference(DocumentReference doc) {
        MessageAttachmentDto dto = new MessageAttachmentDto();

        String fhirId = doc.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));

        // Message ID
        dto.setMessageId(getMessageId(doc));

        // Category
        Extension catExt = doc.getExtensionByUrl(EXT_CATEGORY);
        if (catExt != null && catExt.getValue() instanceof StringType) {
            dto.setCategory(((StringType) catExt.getValue()).getValue());
        }

        // Type
        Extension typeExt = doc.getExtensionByUrl(EXT_TYPE);
        if (typeExt != null && typeExt.getValue() instanceof StringType) {
            dto.setType(((StringType) typeExt.getValue()).getValue());
        }

        // Description
        dto.setDescription(doc.getDescription());

        // File info from attachment
        if (doc.hasContent() && !doc.getContent().isEmpty()) {
            Attachment att = doc.getContent().get(0).getAttachment();
            dto.setFileName(att.getTitle());
            dto.setContentType(att.getContentType());
        }

        dto.setEncrypted(false);
        return dto;
    }

    private boolean isMessageAttachment(DocumentReference doc) {
        if (!doc.hasType()) return false;
        return doc.getType().getCoding().stream()
                .anyMatch(c -> DOC_TYPE_SYSTEM.equals(c.getSystem()) && DOC_TYPE_CODE.equals(c.getCode()));
    }

    private Long getMessageId(DocumentReference doc) {
        Extension ext = doc.getExtensionByUrl(EXT_MESSAGE_ID);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public static class DownloadResult {
        private final InputStream inputStream;
        private final String contentType;
        private final String fileName;

        public DownloadResult(InputStream inputStream, String contentType, String fileName) {
            this.inputStream = inputStream;
            this.contentType = contentType;
            this.fileName = fileName;
        }

        public InputStream getInputStream() { return inputStream; }
        public String getContentType() { return contentType; }
        public String getFileName() { return fileName; }
    }
}
