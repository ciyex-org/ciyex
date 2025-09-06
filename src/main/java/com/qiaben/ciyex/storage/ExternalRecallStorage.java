package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.RecallDto;
import java.util.List;

public interface ExternalRecallStorage extends ExternalStorage<RecallDto> {
    String createRecall(RecallDto recallDto);
    void updateRecall(RecallDto recallDto, String externalId);
    RecallDto getRecall(String externalId);
    void deleteRecall(String externalId);
    List<RecallDto> searchAllRecalls();
}
