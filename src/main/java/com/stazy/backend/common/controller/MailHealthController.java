package com.stazy.backend.common.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class MailHealthController {

    private static final Logger log = LoggerFactory.getLogger(MailHealthController.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.host}")
    private String mailHost;
    
    @Value("${spring.mail.port}")
    private int mailPort;
    
    @Value("${spring.mail.username}")
    private String mailUsername;

    public MailHealthController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @GetMapping("/mail-config")
    public ResponseEntity<Map<String, Object>> checkMailConfig() {
        Map<String, Object> config = new HashMap<>();
        
        log.info("[MAIL-HEALTH] Checking mail configuration");
        
        config.put("host", mailHost);
        config.put("port", mailPort);
        config.put("username", mailUsername);
        config.put("usernameLength", mailUsername != null ? mailUsername.length() : 0);
        
        // Test SMTP connection
        try {
            log.info("[MAIL-HEALTH] Testing SMTP connection to {}:{}", mailHost, mailPort);
            
            // Try to get transport and connect
            var session = mailSender.createMimeMessage().getSession();
            Transport transport = session.getTransport("smtp");
            
            log.info("[MAIL-HEALTH] Attempting to connect to SMTP server...");
            transport.connect(mailHost, mailPort, mailUsername, null); // Don't pass password in test
            
            config.put("smtpConnectionTest", "Cannot test with password in health check");
            config.put("status", "Configuration loaded");
            
            log.info("[MAIL-HEALTH] ✅ Mail configuration loaded successfully");
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("[MAIL-HEALTH] ❌ SMTP connection test failed: {}", e.getMessage(), e);
            config.put("error", e.getMessage());
            config.put("status", "Configuration loaded but connection test failed");
            return ResponseEntity.status(500).body(config);
        }
    }
}
