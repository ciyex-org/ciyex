package com.qiaben.ciyex;

import com.qiaben.ciyex.entity.InvoiceBill;
import com.qiaben.ciyex.repository.InvoiceBillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InvoiceReceiptControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    InvoiceBillRepository invoiceRepo;

    @Test
    public void receiptEndpointReturnsPdf() throws Exception {
        InvoiceBill inv = InvoiceBill.builder()
                .amount(BigDecimal.valueOf(12.34))
                .status(com.qiaben.ciyex.entity.InvoiceStatus.PAID)
                .externalId("TEST-123")
                .invoiceNumber("INV-TEST")
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        inv = invoiceRepo.save(inv);

        mvc.perform(get("/api/invoice-bills/" + inv.getId() + "/receipt").header("X-Org-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("receipt-")));
    }
}
