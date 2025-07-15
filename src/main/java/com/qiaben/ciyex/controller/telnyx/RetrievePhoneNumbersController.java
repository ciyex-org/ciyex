package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ListPhoneMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.RetrievePhoneNumbersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class RetrievePhoneNumbersController {

    private final RetrievePhoneNumbersService retrievePhoneNumbersService;

    @Autowired
    public RetrievePhoneNumbersController(RetrievePhoneNumbersService retrievePhoneNumbersService) {
        this.retrievePhoneNumbersService = retrievePhoneNumbersService;
    }

    @GetMapping("/messaging_profiles/{id}/phone_numbers")
    public ListPhoneMessagingProfileDto listPhoneNumbers(
            @PathVariable("id") String id,
            @RequestParam(value = "page[number]", defaultValue = "1") int pageNumber,
            @RequestParam(value = "page[size]", defaultValue = "20") int pageSize) {
        return retrievePhoneNumbersService.listPhoneNumbers(id, pageNumber, pageSize);
    }
}
