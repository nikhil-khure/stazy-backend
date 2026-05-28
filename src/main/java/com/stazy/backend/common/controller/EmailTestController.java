package com.stazy.backend.common.controller;

import com.stazy.backend.common.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class EmailTestController {

    private static final Logger log = LoggerFactory.getLogger(EmailTestController.class);
    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send-test-email")
    public ResponseEntity<String> sendTestEmail(@RequestParam String email) {
        log.info("[EMAIL-TEST] Sending test email to: {}", email);
        try {
            emailService.sendOtpEmail(email, "123456", "Test");
            return ResponseEntity.ok("Test email triggered. Check logs and inbox.");
        } catch (Exception e) {
            log.error("[EMAIL-TEST] Failed to send test email: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed: " + e.getMessage());
        }
    }
}
