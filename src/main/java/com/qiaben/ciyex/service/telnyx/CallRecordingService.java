package com.qiaben.ciyex.service.telnyx;


import com.qiaben.ciyex.dto.telnyx.*;

import java.util.List;

public interface CallRecordingService {
    RecordingListResponseDto listRecordings(String queryString);
    RecordingDto getRecording(String recordingId);
    void          deleteRecording(String recordingId);
    void          deleteRecordingsBatch(List<String> recordingIds);

    RecordingTranscriptionListResponseDto listTranscriptions();
    RecordingTranscriptionDto getTranscription(String transcriptionId);
    void                                  deleteTranscription(String transcriptionId);

    CustomStorageCredentialDto getCredentials(String connectionId);
    CustomStorageCredentialDto createCredentials(String connectionId, CustomStorageCredentialDto body);
    CustomStorageCredentialDto updateCredentials(String connectionId, CustomStorageCredentialDto body);
    void                       deleteCredentials(String connectionId);
}
