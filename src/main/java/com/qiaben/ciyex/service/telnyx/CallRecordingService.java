package com.qiaben.ciyex.service.telnyx;



import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingListResponseDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCustomStorageCredentialDto;

import java.util.List;

public interface CallRecordingService {
    TelnyxRecordingListResponseDto listRecordings(String queryString);
    TelnyxRecordingDto getRecording(String recordingId);
    void          deleteRecording(String recordingId);
    void          deleteRecordingsBatch(List<String> recordingIds);

    TelnyxRecordingTranscriptionListResponseDto listTranscriptions();
    TelnyxRecordingTranscriptionDto getTranscription(String transcriptionId);
    void                                  deleteTranscription(String transcriptionId);

    TelnyxCustomStorageCredentialDto getCredentials(String connectionId);
    TelnyxCustomStorageCredentialDto createCredentials(String connectionId, TelnyxCustomStorageCredentialDto body);
    TelnyxCustomStorageCredentialDto updateCredentials(String connectionId, TelnyxCustomStorageCredentialDto body);
    void                       deleteCredentials(String connectionId);
}
