package com.stazy.backend.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendOtpEmail(String toEmail, String otp, String roleName) {
        log.info("[OTP-EMAIL] Starting async OTP email send to: {} for role: {}", toEmail, roleName);
        try {
            String subject = "Your " + roleName + " Login OTP - Stazy";
            String body = buildOtpEmailBody(otp, roleName);
            sendHtmlEmail(toEmail, subject, body);
            log.info("[OTP-EMAIL] Completed async OTP email send to: {}", toEmail);
        } catch (Exception e) {
            log.error("[OTP-EMAIL] ❌ CRITICAL: Failed to send OTP email to: {}. Error: {}", toEmail, e.getMessage(), e);
            // Re-throw to trigger AsyncUncaughtExceptionHandler
            throw e;
        }
    }
    
    // Synchronous version for testing SMTP configuration
    public void sendOtpEmailSync(String toEmail, String otp, String roleName) {
        log.info("[OTP-EMAIL-SYNC] Starting synchronous OTP email send to: {} for role: {}", toEmail, roleName);
        String subject = "Your " + roleName + " Login OTP - Stazy";
        String body = buildOtpEmailBody(otp, roleName);
        sendHtmlEmail(toEmail, subject, body);
        log.info("[OTP-EMAIL-SYNC] Completed synchronous OTP email send to: {}", toEmail);
    }

    @Async
    public void sendAdminHiredEmail(String toEmail, String adminId, String password, String secretCode, String cityName) {
        log.info("[ADMIN-HIRED] Starting async admin hired email send to: {}", toEmail);
        String subject = "Welcome to Stazy - Admin Account Created";
        String body = buildAdminHiredEmailBody(adminId, password, secretCode, cityName);
        sendHtmlEmail(toEmail, subject, body);
        log.info("[ADMIN-HIRED] Completed async admin hired email send to: {}", toEmail);
    }

    @Async
    public void sendAdminRejectionEmail(String toEmail, String fullName) {
        log.info("[ADMIN-REJECT] Starting async admin rejection email send to: {}", toEmail);
        String subject = "Stazy Admin Application Update";
        String body = buildAdminRejectionEmailBody(fullName);
        sendHtmlEmail(toEmail, subject, body);
        log.info("[ADMIN-REJECT] Completed async admin rejection email send to: {}", toEmail);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        long startTime = System.currentTimeMillis();
        log.info("[EMAIL-SEND] Attempting to send email to: {} with subject: {}", toEmail, subject);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom("noreply@stazy.in");
            
            log.info("[EMAIL-SEND] Calling mailSender.send() for: {}", toEmail);
            mailSender.send(message);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[EMAIL-SEND] ✅ Email sent successfully to: {} in {}ms", toEmail, duration);
            
        } catch (MessagingException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[EMAIL-SEND] ❌ MessagingException sending email to: {} after {}ms. Error: {}", 
                     toEmail, duration, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[EMAIL-SEND] ❌ Unexpected error sending email to: {} after {}ms. Error: {}", 
                     toEmail, duration, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private String buildOtpEmailBody(String otp, String roleName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #0047AB 0%%, #0066CC 100%%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                        .otp-box { background: white; border: 2px solid #0047AB; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
                        .otp-code { font-size: 32px; font-weight: bold; color: #0047AB; letter-spacing: 8px; }
                        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 12px; margin: 20px 0; }
                        .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0;">🔐 Stazy Login OTP</h1>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>You have requested to log in to your <strong>%s</strong> account on Stazy.</p>
                            <div class="otp-box">
                                <p style="margin: 0 0 10px 0; color: #666;">Your One-Time Password (OTP) is:</p>
                                <div class="otp-code">%s</div>
                            </div>
                            <div class="warning">
                                <strong>⚠️ Important:</strong>
                                <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                                    <li>This OTP is valid for 10 minutes only</li>
                                    <li>Do not share this OTP with anyone</li>
                                    <li>If you did not request this OTP, please ignore this email</li>
                                </ul>
                            </div>
                            <p>Thank you for using Stazy!</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 Stazy. All rights reserved.</p>
                            <p>This is an automated email. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(roleName, otp);
    }

    private String buildAdminHiredEmailBody(String adminId, String password, String secretCode, String cityName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #0047AB 0%%, #0066CC 100%%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                        .credentials-box { background: white; border: 2px solid #28a745; border-radius: 8px; padding: 20px; margin: 20px 0; }
                        .credential-row { display: flex; justify-content: space-between; padding: 12px; border-bottom: 1px solid #eee; }
                        .credential-row:last-child { border-bottom: none; }
                        .credential-label { font-weight: bold; color: #666; }
                        .credential-value { font-family: monospace; color: #0047AB; font-weight: bold; }
                        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 12px; margin: 20px 0; }
                        .success { background: #d4edda; border-left: 4px solid #28a745; padding: 12px; margin: 20px 0; }
                        .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0;">🎉 Welcome to Stazy Admin Team!</h1>
                        </div>
                        <div class="content">
                            <div class="success">
                                <strong>✅ Congratulations!</strong> Your admin account has been successfully created.
                            </div>
                            <p>Dear Admin,</p>
                            <p>Your application has been approved by the Super Admin. Below are your login credentials:</p>
                            <div class="credentials-box">
                                <div class="credential-row">
                                    <span class="credential-label">Admin ID / Username:</span>
                                    <span class="credential-value">%s</span>
                                </div>
                                <div class="credential-row">
                                    <span class="credential-label">Password:</span>
                                    <span class="credential-value">%s</span>
                                </div>
                                <div class="credential-row">
                                    <span class="credential-label">Secret Code:</span>
                                    <span class="credential-value">%s</span>
                                </div>
                                <div class="credential-row">
                                    <span class="credential-label">Assigned City:</span>
                                    <span class="credential-value">%s</span>
                                </div>
                            </div>
                            <div class="warning">
                                <strong>🔒 Security Instructions:</strong>
                                <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                                    <li><strong>Keep these credentials secure</strong> - Do not share with anyone</li>
                                    <li><strong>Change your password</strong> after first login (recommended)</li>
                                    <li><strong>Secret Code is required</strong> for every login along with password</li>
                                    <li><strong>OTP will be sent</strong> to this email for login verification</li>
                                </ul>
                            </div>
                            <p><strong>Next Steps:</strong></p>
                            <ol>
                                <li>Visit the Stazy admin login page</li>
                                <li>Enter your Admin ID and Password</li>
                                <li>Enter your Secret Code when prompted</li>
                                <li>Verify the OTP sent to this email</li>
                                <li>Complete your profile setup</li>
                            </ol>
                            <p>If you have any questions or need assistance, please contact the Super Admin.</p>
                            <p>Welcome aboard!</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 Stazy. All rights reserved.</p>
                            <p>This is an automated email. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(adminId, password, secretCode, cityName);
    }

    private String buildAdminRejectionEmailBody(String fullName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #dc3545 0%%, #c82333 100%%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                        .info-box { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0;">Stazy Admin Application Update</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>Thank you for your interest in joining the Stazy admin team.</p>
                            <div class="info-box">
                                <strong>Application Status:</strong> Unfortunately, your admin application has been rejected by the Super Admin.
                            </div>
                            <p>We appreciate the time and effort you put into your application. While we are unable to proceed with your application at this time, we encourage you to:</p>
                            <ul>
                                <li>Review the requirements for the admin position</li>
                                <li>Consider reapplying in the future if circumstances change</li>
                                <li>Contact the Super Admin if you have any questions</li>
                            </ul>
                            <p>Thank you for your understanding.</p>
                            <p>Best regards,<br>Stazy Team</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 Stazy. All rights reserved.</p>
                            <p>This is an automated email. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(fullName);
    }
}
