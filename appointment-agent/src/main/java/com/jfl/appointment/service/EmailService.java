package com.jfl.appointment.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(
            String to,
            String subject,
            String htmlContent
    ) throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("chetan.dahule03@gmail.com", "Hola MD");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
    @Async("emailTaskExecutor")
    public void sendSubscriptionActivatedEmail(
            String toEmail,
            String adminName,
            String clinicName,
            String planName,
            LocalDate startDate,
            LocalDate endDate) {
        log.info("Email Sending.....");
        String subject = "Your Hola MD subscription is now active";

        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Subscription Activated</title>
                </head>

                <body style="font-family: Arial, sans-serif;
                             background-color: #f5f7fa;
                             padding: 30px;">

                    <div style="max-width: 600px;
                                margin: auto;
                                background: white;
                                padding: 30px;
                                border-radius: 10px;">

                        <h2 style="color: #2563eb;">
                            Your Hola MD subscription is active 🎉
                        </h2>

                        <p>Hello %s,</p>

                        <p>
                            Your subscription for
                            <strong>%s</strong>
                            has been approved and activated by the
                            Hola MD administrator.
                        </p>

                        <div style="background: #f1f5f9;
                                    padding: 20px;
                                    border-radius: 8px;
                                    margin: 20px 0;">

                            <p>
                                <strong>Plan:</strong> %s
                            </p>

                            <p>
                                <strong>Start Date:</strong> %s
                            </p>

                            <p>
                                <strong>End Date:</strong> %s
                            </p>

                            <p>
                                <strong>Status:</strong>
                                <span style="color: green;">
                                    ACTIVE
                                </span>
                            </p>

                        </div>

                        <p>
                            You can now log in to your Hola MD dashboard
                            and start using the features included in your plan.
                        </p>

                        <p>
                            Thank you for choosing Hola MD.
                        </p>

                        <p>
                            Regards,<br>
                            <strong>Hola MD Team</strong>
                        </p>

                    </div>

                </body>
                </html>
                """.formatted(
                adminName,
                clinicName,
                planName,
                startDate,
                endDate
        );

        try {
            sendEmail(toEmail,subject,htmlContent);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendSubscriptionPaymentRejectedEmail(
            String toEmail,
            String adminName,
            String clinicName,
            String planName,
            String transactionId,
            BigDecimal amount,
            String rejectionReason) {

        log.info("Sending subscription rejection email.....");

        String subject = "Hola MD subscription payment rejected";

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Subscription Payment Rejected</title>
            </head>

            <body style="font-family: Arial, sans-serif;
                         background-color: #f5f7fa;
                         padding: 30px;">

                <div style="max-width: 600px;
                            margin: auto;
                            background: white;
                            padding: 30px;
                            border-radius: 10px;">

                    <h2 style="color: #dc2626;">
                        Your Hola MD subscription payment was rejected
                    </h2>

                    <p>Hello %s,</p>

                    <p>
                        Your subscription payment for
                        <strong>%s</strong>
                        could not be verified and has been rejected
                        by the Hola MD administrator.
                    </p>

                    <div style="background: #f1f5f9;
                                padding: 20px;
                                border-radius: 8px;
                                margin: 20px 0;">

                        <p>
                            <strong>Plan:</strong> %s
                        </p>

                        <p>
                            <strong>Transaction ID:</strong> %s
                        </p>

                        <p>
                            <strong>Amount:</strong> ₹%s
                        </p>

                        <p>
                            <strong>Status:</strong>
                            <span style="color: #dc2626;">
                                REJECTED
                            </span>
                        </p>

                    </div>

                    <div style="background: #fef2f2;
                                border-left: 4px solid #dc2626;
                                padding: 15px;
                                margin: 20px 0;">

                        <p style="margin: 0;">
                            <strong>Reason for rejection:</strong>
                        </p>

                        <p style="margin-bottom: 0;">
                            %s
                        </p>

                    </div>

                    <p>
                        Please verify your payment details and submit
                        a new payment with the correct transaction ID.
                    </p>

                    <p>
                        If you believe this payment was rejected in error,
                        please contact the Hola MD administrator.
                    </p>

                    <p>
                        Regards,<br>
                        <strong>Hola MD Team</strong>
                    </p>

                </div>

            </body>
            </html>
            """.formatted(
                adminName,
                clinicName,
                planName,
                transactionId,
                amount,
                rejectionReason != null && !rejectionReason.isBlank()
                        ? rejectionReason
                        : "The transaction could not be verified."
        );

        try {
            sendEmail(toEmail, subject, htmlContent);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendSubscriptionPaymentSubmittedEmail(
            String toEmail,
            String adminName,
            String submittedBy,
            String clinicName,
            String planName,
            String transactionId,
            BigDecimal amount) {

        log.info("Sending subscription payment verification email to Super Admin.....");

        String subject = "Hola MD - New Subscription Payment Requires Verification";

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Payment Verification Required</title>
            </head>

            <body style="font-family: Arial, sans-serif;
                         background-color: #f5f7fa;
                         padding: 30px;">

                <div style="max-width: 600px;
                            margin: auto;
                            background: white;
                            padding: 30px;
                            border-radius: 10px;">

                    <h2 style="color: #2563eb;">
                        New Subscription Payment Requires Verification
                    </h2>

                    <p>Hello %s,</p>

                    <p>
                        A clinic administrator has submitted a new
                        subscription payment that requires your verification.
                    </p>

                    <div style="background: #f1f5f9;
                                padding: 20px;
                                border-radius: 8px;
                                margin: 20px 0;">

                        <p>
                            <strong>Clinic:</strong> %s
                        </p>

                        <p>
                            <strong>Submitted By:</strong> %s
                        </p>

                        <p>
                            <strong>Plan:</strong> %s
                        </p>

                        <p>
                            <strong>Transaction ID:</strong> %s
                        </p>

                        <p>
                            <strong>Amount:</strong> ₹%s
                        </p>

                        <p>
                            <strong>Status:</strong>
                            <span style="color: #f59e0b;">
                                PENDING VERIFICATION
                            </span>
                        </p>

                    </div>

                    <p>
                        Please verify the payment transaction and approve
                        or reject the subscription from the Hola MD
                        administration dashboard.
                    </p>

                    <p>
                        Regards,<br>
                        <strong>Hola MD Team</strong>
                    </p>

                </div>

            </body>
            </html>
            """.formatted(
                adminName,
                clinicName,
                submittedBy,
                planName,
                transactionId,
                amount
        );

        try {
            sendEmail(toEmail, subject, htmlContent);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error(
                    "Failed to send subscription payment verification email to {}",
                    toEmail,
                    e
            );
        }
    }

}