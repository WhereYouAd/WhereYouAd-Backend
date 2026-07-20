package com.whereyouad.WhereYouAd.infrastructure.client.mail;

import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.WeeklyReportResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIMailService {

    private final JavaMailSender mailSender;

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
                log.info("[MailService] 주간 리포트 발송 성공. to={}, org={}", email, orgName);
            } catch (Exception e) {
                log.error("[MailService] 주간 리포트 발송 실패. to={}, error={}", email, e.getMessage());
            }
        }
    }

    private String buildHtml(WeeklyReportResponse.WeeklyAnalysisResponse a,
                              String orgName, LocalDate weekStart, LocalDate weekEnd) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"ko\"><head><meta charset=\"UTF-8\"></head><body style=\"margin:0;padding:0;background:#f4f5f7;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;\">");

        // 최외곽 컨테이너
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:32px 16px;\">");
        sb.append("<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;\">");

        // 헤더
        sb.append("<tr><td style=\"background:#1a1a2e;border-radius:12px 12px 0 0;padding:32px;\">");
        sb.append("<p style=\"margin:0 0 4px;color:#8b9dc3;font-size:13px;letter-spacing:1px;\">WHEREYOUAD</p>");
        sb.append(String.format("<h1 style=\"margin:0 0 8px;color:#ffffff;font-size:24px;font-weight:700;\">주간 광고 리포트</h1>"));
        sb.append(String.format("<p style=\"margin:0;color:#a0b0c8;font-size:14px;\">%s | %s ~ %s</p>",
                escapeHtml(orgName), weekStart.format(DATE_FMT), weekEnd.format(DATE_FMT)));
        sb.append("</td></tr>");

        // weekSummary 배너
        sb.append("<tr><td style=\"background:#2a52be;padding:16px 32px;\">");
        sb.append(String.format("<p style=\"margin:0;color:#ffffff;font-size:15px;font-weight:600;\">📊 %s</p>", escapeHtml(a.weekSummary())));
        sb.append("</td></tr>");

        // 본문 영역
        sb.append("<tr><td style=\"background:#ffffff;padding:32px;\">");

        // KPI 요약
        if (a.kpiOverview() != null) {
            sb.append("<h2 style=\"margin:0 0 16px;font-size:16px;color:#1a1a2e;border-bottom:2px solid #f0f0f0;padding-bottom:8px;\">이번 주 핵심 지표</h2>");
            sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>");
            appendKpiCard(sb, "총 광고비", a.kpiOverview().totalSpend(), "원");
            appendKpiCard(sb, "총 전환수", a.kpiOverview().totalConversions(), "건");
            appendKpiCard(sb, "통합 ROAS", a.kpiOverview().blendedRoas(), "%");
            sb.append("</tr></table>");
        }

        // Highlights
        if (a.highlights() != null && !a.highlights().isEmpty()) {
            sb.append("<h2 style=\"margin:24px 0 16px;font-size:16px;color:#1a1a2e;border-bottom:2px solid #f0f0f0;padding-bottom:8px;\">성과 분석</h2>");
            for (WeeklyReportResponse.Highlight h : a.highlights()) {
                String borderColor = "POSITIVE".equals(h.type()) ? "#28a745" : "#dc3545";
                String typeLabel = "POSITIVE".equals(h.type()) ? "✅ 긍정" : "⚠️ 주의";
                String confidenceColor = switch (h.confidence()) {
                    case "HIGH" -> "#28a745";
                    case "MEDIUM" -> "#fd7e14";
                    default -> "#6c757d";
                };
                sb.append(String.format("<div style=\"border-left:4px solid %s;padding:16px;margin-bottom:16px;background:#fafafa;border-radius:0 8px 8px 0;\">", borderColor));
                sb.append(String.format("<div style=\"display:flex;justify-content:space-between;margin-bottom:8px;\">"));
                sb.append(String.format("<span style=\"font-size:11px;font-weight:600;color:%s;\">%s</span>", borderColor, typeLabel));
                sb.append(String.format("<span style=\"font-size:11px;color:%s;\">신뢰도: %s</span>", confidenceColor, h.confidence()));
                sb.append("</div>");
                sb.append(String.format("<p style=\"margin:0 0 8px;font-size:15px;font-weight:700;color:#1a1a2e;\">%s</p>", escapeHtml(h.title())));
                sb.append(String.format("<p style=\"margin:0 0 6px;font-size:14px;color:#333;\"><strong>무슨 일:</strong> %s</p>", escapeHtml(h.what())));
                sb.append(String.format("<p style=\"margin:0 0 6px;font-size:14px;color:#333;\"><strong>원인 분석:</strong> %s</p>", escapeHtml(h.why())));
                if (h.evidence() != null && !h.evidence().isBlank()) {
                    sb.append(String.format("<p style=\"margin:0 0 6px;font-size:13px;color:#666;background:#f0f0f0;padding:6px 10px;border-radius:4px;\">📌 근거: %s</p>", escapeHtml(h.evidence())));
                }
                sb.append(String.format("<p style=\"margin:0;font-size:14px;color:#2a52be;\"><strong>👉 다음 주 액션:</strong> %s</p>", escapeHtml(h.action())));
                sb.append("</div>");
            }
        }

        // Platform Insights
        if (a.platformInsights() != null && !a.platformInsights().isEmpty()) {
            sb.append("<h2 style=\"margin:24px 0 16px;font-size:16px;color:#1a1a2e;border-bottom:2px solid #f0f0f0;padding-bottom:8px;\">플랫폼별 현황</h2>");
            for (WeeklyReportResponse.PlatformInsight p : a.platformInsights()) {
                sb.append("<div style=\"padding:12px 16px;margin-bottom:8px;border:1px solid #e8e8e8;border-radius:8px;\">");
                sb.append(String.format("<p style=\"margin:0 0 4px;font-size:13px;font-weight:700;color:#2a52be;\">%s</p>", escapeHtml(p.platform())));
                sb.append(String.format("<p style=\"margin:0 0 4px;font-size:14px;color:#333;\">%s</p>", escapeHtml(p.oneLineSummary())));
                sb.append(String.format("<p style=\"margin:0;font-size:13px;color:#666;\">%s</p>", escapeHtml(p.keyObservation())));
                sb.append("</div>");
            }
        }

        // 다음 주 집중 포인트
        if (a.nextWeekFocus() != null) {
            sb.append("<div style=\"background:#eef2ff;border-radius:8px;padding:20px;margin-top:24px;\">");
            sb.append("<h2 style=\"margin:0 0 12px;font-size:15px;color:#2a52be;\">🎯 다음 주 집중 포인트</h2>");
            sb.append(String.format("<p style=\"margin:0 0 6px;font-size:14px;font-weight:700;color:#1a1a2e;\">%s</p>", escapeHtml(a.nextWeekFocus().focus())));
            sb.append(String.format("<p style=\"margin:0 0 6px;font-size:14px;color:#333;\">이유: %s</p>", escapeHtml(a.nextWeekFocus().reason())));
            sb.append(String.format("<p style=\"margin:0;font-size:14px;color:#555;\">기대 효과: %s</p>", escapeHtml(a.nextWeekFocus().expectedOutcome())));
            sb.append("</div>");
        }

        // Data Caveats
        if (a.dataCaveats() != null && !a.dataCaveats().isEmpty()) {
            sb.append("<div style=\"margin-top:20px;padding:12px 16px;background:#fff8e1;border-radius:6px;\">");
            sb.append("<p style=\"margin:0 0 6px;font-size:13px;font-weight:600;color:#e6a817;\">⚠️ 데이터 해석 주의사항</p>");
            for (String caveat : a.dataCaveats()) {
                sb.append(String.format("<p style=\"margin:2px 0;font-size:13px;color:#666;\">• %s</p>", escapeHtml(caveat)));
            }
            sb.append("</div>");
        }

        sb.append("</td></tr>");

        // 푸터
        sb.append("<tr><td style=\"background:#f8f9fa;border-radius:0 0 12px 12px;padding:20px 32px;border-top:1px solid #e8e8e8;\">");
        sb.append("<p style=\"margin:0;font-size:12px;color:#999;text-align:center;\">이 리포트는 WhereYouAd에서 자동 발송된 주간 광고 성과 분석 이메일입니다.</p>");
        sb.append("</td></tr>");

        sb.append("</table></td></tr></table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendKpiCard(StringBuilder sb, String label, WeeklyReportResponse.KpiMetric metric, String unit) {
        if (metric == null) {
            sb.append("<td width=\"33%\" style=\"padding:0 4px;\"><div style=\"background:#f8f9fa;border-radius:8px;padding:16px;text-align:center;\"><p style=\"margin:0;font-size:12px;color:#666;\">").append(escapeHtml(label)).append("</p></div></td>");
            return;
        }
        String changeColor = metric.changeRate() >= 0 ? "#28a745" : "#dc3545";
        String changeArrow = metric.changeRate() >= 0 ? "▲" : "▼";
        String valueStr = unit.equals("원")
                ? String.format("%,.0f%s", metric.thisWeek(), unit)
                : unit.equals("건")
                        ? String.format("%,.0f%s", metric.thisWeek(), unit)
                        : String.format("%.1f%s", metric.thisWeek(), unit);

        sb.append("<td width=\"33%\" style=\"padding:0 4px;\">");
        sb.append("<div style=\"background:#f8f9fa;border-radius:8px;padding:16px;text-align:center;\">");
        sb.append(String.format("<p style=\"margin:0 0 4px;font-size:12px;color:#666;\">%s</p>", escapeHtml(label)));
        sb.append(String.format("<p style=\"margin:0 0 4px;font-size:20px;font-weight:700;color:#1a1a2e;\">%s</p>", valueStr));
        sb.append(String.format("<p style=\"margin:0;font-size:12px;color:%s;\">%s %.1f%%</p>", changeColor, changeArrow, Math.abs(metric.changeRate())));
        sb.append("</div></td>");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
