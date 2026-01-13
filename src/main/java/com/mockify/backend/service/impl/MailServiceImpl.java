package com.mockify.backend.service.impl;

import com.mockify.backend.service.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordPageUrl;
    @Value("${app.verification.email.frontend-url}")
    private String emailVerificationBaseUrl;
    @Value("${app.verification.email.ttl-minutes}")
    private Long emailVerificationTTL;


    @Override
    public void sendPasswordResetMail(String to, String resetLink) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Mockify <mockify.noreply@gmail.com>");
            helper.setTo(to);
            helper.setSubject("Reset your Mockify password");
            helper.setText(buildHtmlContent(resetLink), true);

            mailSender.send(message);

            log.info("Password reset email sent to {}", to);

        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}", to, ex);
            // DO NOT throw, avoid leaking info
        }
    }
    @Override
    public void sendEmailVerificationMail(String to, String verifyLink) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Mockify <mockify.noreply@gmail.com>");
            helper.setTo(to);
            helper.setSubject("Verify your Mockify email");
            helper.setText(buildVerificationHtml(verifyLink), true);

            mailSender.send(message);

            log.info("Verification email sent to {}", to);

        } catch (Exception ex) {
            log.error("Failed to send verification email to {}", to, ex);
        }
    }

    private String buildVerificationHtml(String verificationLink) {
        return """
        <div style="background-color:#f6f8fa; padding:40px 0; 
                    font-family:-apple-system, BlinkMacSystemFont, 
                    'Segoe UI', Helvetica, Arial, sans-serif;">

            <table align="center" width="100%%" cellpadding="0" cellspacing="0"
                   style="max-width:480px; background:#ffffff;
                          border-radius:6px;
                          border:1px solid #d0d7de;">

                <tr>
                    <td style="padding:32px;">

                        <h2 style="text-align:center; margin-top:0;
                                   color:#24292f; font-size:20px;">
                            Verify your email address
                        </h2>

                        <p style="color:#57606a; font-size:14px; line-height:1.5;">
                            Thanks for signing up for Mockify! Please confirm your
                            email address by clicking the button below.
                        </p>

                        <div style="margin:24px 0; text-align:center;">
                            <a href="%1$s"
                               style="background-color:#2da44e;
                                      color:#ffffff;
                                      padding:10px 20px;
                                      text-decoration:none;
                                      border-radius:6px;
                                      font-size:14px;
                                      display:inline-block;">
                                Verify email
                            </a>
                        </div>

                        <p style="color:#57606a; font-size:13px;">
                            This verification link will expire in
                            <strong>%2$s minutes</strong>.
                        </p>

                        <p style="color:#57606a; font-size:13px;">
                            If you didn’t create a Mockify account,
                            you can safely ignore this email.
                        </p>

                        <hr style="border:none;
                                   border-top:1px solid #d0d7de;
                                   margin:24px 0;">

                        <p style="font-size:13px; color:#57606a;">
                            <a href="%3$s" style="color:#0969da; text-decoration:none;">
                                Click here to request a new verification link.
                            </a>
                        </p>

                        <p style="font-size:13px; color:#57606a; margin-top:24px;">
                            Thanks,<br>
                            <strong>The Mockify Team</strong>
                        </p>

                    </td>
                </tr>
            </table>
        </div>
    """.formatted(
                verificationLink,
                emailVerificationTTL,
                emailVerificationBaseUrl
        );
    }


    private String buildHtmlContent(String resetLink) {
        return """
        <div style="background-color:#f6f8fa; padding:40px 0; 
                    font-family:-apple-system, BlinkMacSystemFont, 
                    'Segoe UI', Helvetica, Arial, sans-serif;">

            <table align="center" width="100%%" cellpadding="0" cellspacing="0"
                   style="max-width:480px; background:#ffffff;
                          border-radius:6px;
                          border:1px solid #d0d7de;">

                <tr>
                    <td style="padding:32px;">

                        <h2 style="text-align:center; margin-top:0;
                                   color:#24292f; font-size:20px;">
                            Reset your Mockify password
                        </h2>

                        <p style="color:#57606a; font-size:14px; line-height:1.5;">
                            You’re receiving this email because you requested
                            a password reset for your Mockify account.
                        </p>

                        <div style="margin:24px 0; text-align:center;">
                            <a href="%1$s"
                               style="background-color:#2da44e;
                                      color:#ffffff;
                                      padding:10px 20px;
                                      text-decoration:none;
                                      border-radius:6px;
                                      font-size:14px;
                                      display:inline-block;">
                                Reset password
                            </a>
                        </div>

                        <p style="color:#57606a; font-size:13px;">
                            This password reset link will expire in
                            <strong>15 minutes</strong>.
                        </p>

                        <p style="color:#57606a; font-size:13px;">
                            If you didn’t request a password reset,
                            you can safely ignore this email.
                        </p>

                        <hr style="border:none;
                                   border-top:1px solid #d0d7de;
                                   margin:24px 0;">

                        <p style="font-size:13px; color:#57606a;">
                            <a href="%2$s" style="color:#0969da; text-decoration:none;">
                                Click here to get a new password reset link.
                            </a>
                        </p>

                        <p style="font-size:13px; color:#57606a; margin-top:24px;">
                            Thanks,<br>
                            <strong>The Mockify Team</strong>
                        </p>

                    </td>
                </tr>
            </table>
        </div>
    """.formatted(
                resetLink,
                resetPasswordPageUrl
        );
    }
}
