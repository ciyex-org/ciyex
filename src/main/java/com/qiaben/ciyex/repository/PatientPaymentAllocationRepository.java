//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.PatientPaymentAllocation;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface PatientPaymentAllocationRepository extends JpaRepository<PatientPaymentAllocation, Long> {
//    List<PatientPaymentAllocation> findByInvoiceLine_Invoice_Id(Long invoiceId);
//    List<PatientPaymentAllocation> findByPayment_PatientId(Long patientId);
//}
//

package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientPaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientPaymentAllocationRepository extends JpaRepository<PatientPaymentAllocation, Long> {

    @Query("SELECT a FROM PatientPaymentAllocation a WHERE a.payment.patientId = :patientId")
    List<PatientPaymentAllocation> findByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT a FROM PatientPaymentAllocation a WHERE a.invoiceLine.invoice.id = :invoiceId")
    List<PatientPaymentAllocation> findByInvoiceId(@Param("invoiceId") Long invoiceId);
}
