package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.PatientDto;
import com.qiaben.ciyex.service.PatientService;
import com.qiaben.ciyex.service.notification.EmailNotificationService;
import com.qiaben.ciyex.service.notification.SmsNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationController {

    private final SmsNotificationService smsService;
    private final EmailNotificationService emailService;
    private final PatientService patientService;

    public NotificationController(SmsNotificationService smsService,
                                  EmailNotificationService emailService,
                                  PatientService patientService) {
        this.smsService = smsService;
        this.emailService = emailService;
        this.patientService = patientService;
    }

    // ---------- Direct SMS ----------
    public record SmsRequest(@NotBlank String to, @NotBlank String body) {}
    public record SmsResponse(String status) {}

    @PostMapping("/sms")
    public ResponseEntity<SmsResponse> sendSms(@RequestBody SmsRequest req) {
        smsService.sendSms(req.to(), req.body());
        return ResponseEntity.ok(new SmsResponse("✅ SMS sent to " + req.to()));
    }

    // ---------- Direct Email ----------
    public record EmailRequest(@Email String to,
                               @NotBlank String subject,
                               @NotBlank String body) {}
    public record EmailResponse(String status) {}

    @PostMapping("/email")
    public ResponseEntity<EmailResponse> sendEmail(@RequestBody EmailRequest req) {
        emailService.sendEmail(req.to(), req.subject(), req.body());
        return ResponseEntity.ok(new EmailResponse("✅ Email sent to " + req.to()));
    }

    // ---------- Both (send SMS + Email in one call) ----------
    public record BothRequest(@NotBlank String toPhone,
                              @Email String toEmail,
                              @NotBlank String subject,
                              @NotBlank String body) {}
    public record BothResponse(String status) {}

    @PostMapping("/both")
    public ResponseEntity<BothResponse> sendSmsAndEmail(@RequestBody BothRequest req) {
        boolean smsOk = false;
        boolean emailOk = false;

        try {
            smsService.sendSms(req.toPhone(), req.body());
            smsOk = true;
        } catch (Exception e) {
            // log already handled in SmsNotificationService
        }

        try {
            emailService.sendEmail(req.toEmail(), req.subject(), req.body());
            emailOk = true;
        } catch (Exception e) {
            // log already handled in EmailNotificationService
        }

        if (smsOk || emailOk) {
            return ResponseEntity.ok(new BothResponse(
                    "Notification results: SMS=" + smsOk + ", Email=" + emailOk
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BothResponse("❌ Both SMS and Email failed"));
        }
    }

    // ---------- Notify patient by ID ----------
    public record PatientNotificationRequest(@NotBlank String subject,
                                             @NotBlank String body) {}
    public record PatientNotificationResponse(String status) {}

    @PostMapping("/patient/{id}")
    public ResponseEntity<PatientNotificationResponse> notifyPatient(@PathVariable Long id,
                                                                     @RequestBody PatientNotificationRequest req) {
        PatientDto patient = patientService.getById(id);
        if (patient == null) {
            throw new RuntimeException("Patient not found");
        }

        boolean smsOk = false;
        boolean emailOk = false;

        String phone = patient.getPhoneNumber();
        String email = patient.getEmail();

        if (phone != null && !phone.isBlank()) {
            try {
                smsService.sendSms(phone, req.body());
                smsOk = true;
            } catch (Exception e) {
                // handled in service logs
            }
        }

        if (email != null && !email.isBlank()) {
            try {
                emailService.sendEmail(email, req.subject(), req.body());
                emailOk = true;
            } catch (Exception e) {
                // handled in service logs
            }
        }

        if (smsOk || emailOk) {
            return ResponseEntity.ok(new PatientNotificationResponse(
                    "Notification sent to patient " + id + " (SMS=" + smsOk + ", Email=" + emailOk + ")"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PatientNotificationResponse("❌ No notification could be sent to patient " + id));
        }
    }
}
