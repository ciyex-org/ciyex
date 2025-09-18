package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.InvoiceBill;
import com.qiaben.ciyex.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceBillRepository extends JpaRepository<InvoiceBill, Long> {
    List<InvoiceBill> findByOrgId(Long orgId);
    List<InvoiceBill> findByOrgIdAndStatus(Long orgId, InvoiceStatus status);
}
