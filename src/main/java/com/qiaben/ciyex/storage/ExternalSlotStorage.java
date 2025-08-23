package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.SlotDto;
import java.util.List;

public interface ExternalSlotStorage extends ExternalStorage<SlotDto> {

    String createSlot(SlotDto slotDto);

    void updateSlot(SlotDto slotDto, String externalId);

    SlotDto getSlot(String externalId);

    void deleteSlot(String externalId);

    List<SlotDto> searchAllSlots();

    @Override
    default boolean supports(Class<?> entityType) {
        return SlotDto.class.isAssignableFrom(entityType);
    }
}
