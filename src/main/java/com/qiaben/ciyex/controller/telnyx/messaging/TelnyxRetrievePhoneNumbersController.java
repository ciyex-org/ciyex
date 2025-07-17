package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListPhoneMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxRetrievePhoneNumbersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxRetrievePhoneNumbersController {

    private final TelnyxRetrievePhoneNumbersService telnyxRetrievePhoneNumbersService;

    @Autowired
    public TelnyxRetrievePhoneNumbersController(TelnyxRetrievePhoneNumbersService telnyxRetrievePhoneNumbersService) {
        this.telnyxRetrievePhoneNumbersService = telnyxRetrievePhoneNumbersService;
    }

    @GetMapping("/messaging_profiles/{id}/phone_numbers")
    public TelnyxListPhoneMessagingProfileDto listPhoneNumbers(
            @PathVariable("id") String id,
            @RequestParam(value = "page[number]", defaultValue = "1") int pageNumber,
            @RequestParam(value = "page[size]", defaultValue = "20") int pageSize) {
        return telnyxRetrievePhoneNumbersService.listPhoneNumbers(id, pageNumber, pageSize);
    }
}
