package com.whereyouad.WhereYouAd.infrastructure.client.mail;

import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.WeeklyReportResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIMailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M월 d일");

    public void sendWeeklyReport(
            List<String> recipients,
            WeeklyReportResponse.WeeklyAnalysisResponse analysis,
            String orgName,
            LocalDate weekStart,
            LocalDate weekEnd) {

        String subject = String.format("[WhereYouAd] %s 주간 광고 리포트 | %s ~ %s",
                orgName, weekStart.format(DATE_FMT), weekEnd.format(DATE_FMT));
        String htmlBody = buildHtml(analysis, orgName, weekStart, weekEnd);

        for (String email : recipients) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
                log.info("[MailService] 주간 리포트 발송 성공. to={}, org={}", maskEmail(email), orgName);
            } catch (Exception e) {
                log.error("[MailService] 주간 리포트 발송 실패. to={}, error={}", maskEmail(email), e.getMessage());
            }
        }
    }

    private String buildHtml(WeeklyReportResponse.WeeklyAnalysisResponse analysis,
                              String orgName, LocalDate weekStart, LocalDate weekEnd) {
        Context context = new Context();
        context.setVariable("analysis", analysis);
        context.setVariable("orgName", orgName);
        context.setVariable("weekStartStr", weekStart.format(DATE_FMT));
        context.setVariable("weekEndStr", weekEnd.format(DATE_FMT));
        return templateEngine.process("mail/weekly-report", context);
    }

    private String maskEmail(String email) {
        if (email == null) return "(null)";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String visible = local.length() <= 2 ? local.charAt(0) + "*" : local.substring(0, 2) + "**";
        return visible + domain;
    }
}
