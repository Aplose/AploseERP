package fr.aplose.erp.mail.service;

import fr.aplose.erp.mail.entity.EmailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    public MailService(EmailTemplateService emailTemplateService,
                       @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender) {
        this.emailTemplateService = emailTemplateService;
        this.mailSender = mailSender;
    }

    @Value("${app.mail.from: noreply@aplose-erp.local}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public void sendWelcome(String toEmail, String userName, String companyName) {
        String loginUrl = baseUrl + "/login";
        Map<String, String> vars = Map.of(
                "userName", userName != null ? userName : "",
                "companyName", companyName != null ? companyName : "",
                "loginUrl", loginUrl
        );
        emailTemplateService.findByKeyAndLocale("welcome", "fr").ifPresent(t -> sendTemplate(toEmail, t, vars));
    }

    public void sendOnboarding(String toEmail, String userName) {
        String loginUrl = baseUrl + "/login";
        Map<String, String> vars = Map.of(
                "userName", userName != null ? userName : "",
                "loginUrl", loginUrl
        );
        emailTemplateService.findByKeyAndLocale("onboarding", "fr").ifPresent(t -> sendTemplate(toEmail, t, vars));
    }

    /**
     * Sends email to the leave validator when a request is submitted.
     */
    public void sendLeaveRequestSubmittedToValidator(String validatorEmail, String validatorName, String requesterName,
                                                     String leaveTypeLabel, String dateStart, String dateEnd, String requestUrl) {
        Map<String, String> vars = Map.of(
                "validatorName", validatorName != null ? validatorName : "",
                "requesterName", requesterName != null ? requesterName : "",
                "leaveTypeLabel", leaveTypeLabel != null ? leaveTypeLabel : "",
                "dateStart", dateStart != null ? dateStart : "",
                "dateEnd", dateEnd != null ? dateEnd : "",
                "requestUrl", requestUrl != null ? requestUrl : ""
        );
        emailTemplateService.findByKeyAndLocale("leave.request.submitted", "fr").ifPresent(t -> sendTemplate(validatorEmail, t, vars));
    }

    /**
     * Sends email to the requester when their leave request is denied.
     */
    public void sendLeaveRequestDeniedToRequester(String requesterEmail, String requesterName, String responseComment, String requestUrl) {
        Map<String, String> vars = Map.of(
                "requesterName", requesterName != null ? requesterName : "",
                "responseComment", responseComment != null ? responseComment : "",
                "requestUrl", requestUrl != null ? requestUrl : ""
        );
        emailTemplateService.findByKeyAndLocale("leave.request.denied", "fr").ifPresent(t -> sendTemplate(requesterEmail, t, vars));
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Sends a simple text email (e.g. for public form submission notification).
     */
    public void sendSimple(String to, String subject, String bodyText) {
        if (mailSender == null) {
            log.debug("Mail sender not configured, skipping email to {}", to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject != null ? subject : "");
            helper.setText(bodyText != null ? bodyText : "", false);
            mailSender.send(message);
            log.debug("Sent email to {}", to);
        } catch (MessagingException e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private void sendTemplate(String to, EmailTemplate template, Map<String, String> variables) {
        if (mailSender == null) {
            log.debug("Mail sender not configured, skipping email to {}", to);
            return;
        }
        String subject = emailTemplateService.render(template.getSubject(), variables);
        String bodyHtml = template.getBodyHtml() != null ? emailTemplateService.render(template.getBodyHtml(), variables) : null;
        String bodyText = template.getBodyText() != null ? emailTemplateService.render(template.getBodyText(), variables) : null;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            if (bodyHtml != null) {
                helper.setText(bodyText != null ? bodyText : bodyHtml, bodyHtml);
            } else {
                helper.setText(bodyText != null ? bodyText : "");
            }
            mailSender.send(message);
            log.debug("Sent email to {} with template {}", to, template.getTemplateKey());
        } catch (MessagingException e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
