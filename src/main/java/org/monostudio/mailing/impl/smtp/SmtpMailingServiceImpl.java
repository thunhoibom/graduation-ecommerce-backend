package org.monostudio.mailing.impl.smtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.mailing.MailingProperties;
import org.monostudio.mailing.MailingService;
import org.monostudio.mailing.MailingServiceException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.util.StringUtils;

@Service
@Profile("smtp")
public class SmtpMailingServiceImpl implements MailingService {
    private final Logger logger = LoggerFactory.getLogger(SmtpMailingServiceImpl.class);
    private final MailingProperties properties;
    private final JavaMailSender javaMailSender;
    private final OrdersRepository ordersRepository;

    @Autowired
    public SmtpMailingServiceImpl(
        MailingProperties properties,
        JavaMailSender javaMailSender,
        OrdersRepository ordersRepository
    ) {
        this.properties = properties;
        this.javaMailSender = javaMailSender;
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void notifyOrderStatusToClient(OrderPojo sell) throws MailingServiceException {
        if (sell == null || sell.getCustomer() == null) return;
        
        PersonPojo customer = sell.getCustomer();
        String recipient = customer.getEmail();
        String subject = "Cập nhật đơn hàng #" + sell.getBuyOrder() + " - MonoStudio";
        String htmlBody = buildOrderHtmlBody(sell, subject);
        
        sendEmail(recipient, subject, htmlBody);
    }

    @Override
    public void notifyOrderStatusToOwners(OrderPojo sell) throws MailingServiceException {
        String subject = "[Admin] Đơn hàng mới #" + sell.getBuyOrder();
        String htmlBody = buildOrderHtmlBody(sell, subject);
        sendEmail(properties.getOwnerEmail(), subject, htmlBody);
    }

    @Override
    public void notifyLowStockAlert(String productName, int currentStock) throws MailingServiceException {
        String subject = "[Admin] Cảnh báo tồn kho: " + productName;
        String text = "<p>Sản phẩm <b>" + productName + "</b> sắp hết hàng. Số lượng hiện tại: " + currentStock + "</p>";
        sendEmail(properties.getOwnerEmail(), subject, text);
    }

    @Override
    public void notifyReturnRequestStatusToClient(ReturnRequestPojo request) throws MailingServiceException {
        if (request == null || request.getId() == null) {
            return;
        }

        Optional<String> recipient = resolveReturnRequestRecipientEmail(request);
        if (recipient.isEmpty()) {
            logger.warn("SMTP: skip return request #{} mail — customer email not found", request.getId());
            return;
        }

        String subject = resolveReturnRequestSubject(request);
        String htmlBody = buildReturnRequestHtmlBody(request, subject);
        sendEmail(recipient.get(), subject, htmlBody);
    }

    @Override
    public void notifyReturnRequestToOwners(ReturnRequestPojo request) throws MailingServiceException {
        String subject = "[Admin] Yêu cầu trả hàng mới #" + request.getId();
        String text = "<p>Có yêu cầu trả hàng mới cần xử lý.</p>";
        sendEmail(properties.getOwnerEmail(), subject, text);
    }

    @Override
    public void notifyCheckoutOtp(String email, Long orderId, String otpCode, Instant expiresAt) throws MailingServiceException {
        String subject = "Ma OTP xac nhan don hang #" + orderId;
        String text = "<div style='font-family:Arial,sans-serif;color:#333;'>"
            + "<h3>Xac nhan dat hang</h3>"
            + "<p>Ma OTP cua ban la: <b style='font-size:22px;letter-spacing:2px;'>" + otpCode + "</b></p>"
            + "<p>Ma se het han luc: <b>" + expiresAt + "</b></p>"
            + "<p>Neu ban khong thuc hien giao dich nay, vui long bo qua email.</p>"
            + "</div>";
        sendEmail(email, subject, text);
    }

    @Override
    public void notifyContactInquiryToOwners(
        String senderName,
        String senderEmail,
        String senderPhone,
        String subjectCode,
        String message,
        Long inquiryId
    ) throws MailingServiceException {
        String recipient = StringUtils.hasText(properties.getContactInquiryNotificationTo())
            ? properties.getContactInquiryNotificationTo().trim()
            : properties.getOwnerEmail();
        String subjectBase = StringUtils.hasText(properties.getContactInquirySubject())
            ? properties.getContactInquirySubject().trim()
            : "[Mono Studio] Lien he moi";
        String subject = subjectBase + " #" + inquiryId;
        String htmlBody = buildContactInquiryHtml(senderName, senderEmail, senderPhone, subjectCode, message, inquiryId);
        sendEmail(recipient, subject, htmlBody);
    }

    private static String buildContactInquiryHtml(
        String senderName,
        String senderEmail,
        String senderPhone,
        String subjectCode,
        String message,
        Long inquiryId
    ) {
        String safeMessage = escapeHtml(message).replace("\n", "<br/>");
        String topicLabel = subjectLabelVi(subjectCode);
        String phoneRow = StringUtils.hasText(senderPhone)
            ? rowHtml("Điện thoại", escapeHtml(senderPhone))
            : "";

        String mailto = "mailto:" + escapeHtml(senderEmail);

        return ""
            + "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
            + "<body style=\"margin:0;padding:0;background-color:#f3f4f6;\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;background-color:#f3f4f6;padding:24px 12px;\">"
            + "<tr><td align=\"center\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
            + "style=\"border-collapse:collapse;max-width:560px;background-color:#ffffff;"
            + "border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(15,23,42,0.08);\">"

            + "<tr><td style=\"background:linear-gradient(135deg,#dc2626 0%,#991b1b 100%);padding:20px 28px;\">"
            + "<p style=\"margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:13px;font-weight:600;letter-spacing:0.06em;color:rgba(255,255,255,0.9);\">MONO STUDIO</p>"
            + "<h1 style=\"margin:8px 0 0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:20px;font-weight:700;line-height:1.3;color:#ffffff;\">Tin liên hệ mới</h1>"
            + "<p style=\"margin:6px 0 0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:13px;color:rgba(255,255,255,0.85);\">Mã tin <strong style=\"color:#fff;\">#" + inquiryId + "</strong>"
            + " · Phản hồi khách trong vòng 24 giờ làm việc.</p>"
            + "</td></tr>"

            + "<tr><td style=\"padding:28px 28px 8px;\">"
            + "<p style=\"margin:0 0 18px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:15px;line-height:1.55;color:#374151;\">"
            + "Có khách gửi tin qua form liên hệ trên website. Thông tin chi tiết bên dưới.</p>"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">"
            + rowHtml("Họ và tên", escapeHtml(senderName))
            + rowHtml("Email", "<a href=\"" + mailto + "\" style=\"color:#dc2626;text-decoration:none;font-weight:600;\">"
            + escapeHtml(senderEmail) + "</a>")
            + phoneRow
            + rowHtml("Chủ đề", "<span style=\"color:#111827;\">" + escapeHtml(topicLabel) + "</span>"
            + " <span style=\"font-size:12px;color:#9ca3af;\">(" + escapeHtml(subjectCode) + ")</span>")
            + "</table>"
            + "</td></tr>"

            + "<tr><td style=\"padding:0 28px 28px;\">"
            + "<p style=\"margin:0 0 10px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;\">Nội dung tin nhắn</p>"
            + "<div style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;font-size:14px;"
            + "line-height:1.65;color:#1f2937;background-color:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;"
            + "padding:16px 18px;\">" + safeMessage + "</div>"
            + "</td></tr>"

            + "<tr><td style=\"padding:0 28px 24px;border-top:1px solid #f3f4f6;\">"
            + "<p style=\"margin:18px 0 0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:12px;line-height:1.5;color:#9ca3af;\">"
            + "Email được gửi tự động từ hệ thống cửa hàng. Hãy liên hệ lại khách qua địa chỉ email họ để lại.</p>"
            + "</td></tr>"
            + "</table>"
            + "</td></tr></table>"
            + "</body></html>";
    }

    private static String rowHtml(String label, String valueCellHtml) {
        return "<tr>"
            + "<td style=\"padding:10px 0;border-bottom:1px solid #f3f4f6;vertical-align:top;width:120px;\">"
            + "<span style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:13px;color:#6b7280;\">" + label + "</span>"
            + "</td>"
            + "<td style=\"padding:10px 0 12px 16px;border-bottom:1px solid #f3f4f6;\">"
            + "<span style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:14px;color:#111827;\">" + valueCellHtml + "</span>"
            + "</td>"
            + "</tr>";
    }

    private static String subjectLabelVi(String subjectCode) {
        if (subjectCode == null || subjectCode.isBlank()) {
            return "Khác";
        }
        return switch (subjectCode.trim()) {
            case "order" -> "Tư vấn đơn hàng";
            case "product" -> "Hỏi về sản phẩm";
            case "return" -> "Đổi / trả hàng";
            case "cooperation" -> "Hợp tác kinh doanh";
            case "feedback" -> "Góp ý / phản hồi";
            case "other" -> "Khác";
            default -> subjectCode;
        };
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private void sendEmail(String to, String subject, String htmlContent) throws MailingServiceException {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(properties.getSenderEmail());
            
            // Allow name format "Name <email@dom.com>" by extracting email if needed, 
            // JavaMail can parse this automatically but just to be safe:
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml
            
            javaMailSender.send(message);
            logger.info("SMTP Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send SMTP email to {}", to, e);
            throw new MailingServiceException("Error sending email via SMTP", e);
        }
    }

    private String buildOrderHtmlBody(OrderPojo order, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: Arial, sans-serif; color: #333;'>");
        sb.append("<h2>").append(title).append("</h2>");
        if (order.getCustomer() != null) {
            sb.append("<p>Xin chào <b>").append(order.getCustomer().getFirstName()).append("</b>,</p>");
        }
        sb.append("<p>Trạng thái đơn hàng của bạn: <b>").append(order.getStatus()).append("</b></p>");
        sb.append("<p>Mã đơn hàng: #").append(order.getBuyOrder()).append("</p>");
        sb.append("<p>Tổng giá trị: <b>").append(order.getTotalValue()).append(" đ</b></p>");
        sb.append("<br/><p>Cảm ơn bạn đã mua sắm tại hệ thống của chúng tôi.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private Optional<String> resolveReturnRequestRecipientEmail(ReturnRequestPojo request) {
        if (request.getOrderId() == null) {
            return Optional.empty();
        }
        return ordersRepository.findById(request.getOrderId())
            .map(Order::getCustomer)
            .filter(customer -> customer != null && customer.getPerson() != null)
            .map(customer -> customer.getPerson().getEmail())
            .filter(StringUtils::hasText)
            .map(String::trim);
    }

    private String resolveReturnRequestSubject(ReturnRequestPojo request) {
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim().toUpperCase(Locale.ROOT) : "";
        String template = switch (status) {
            case "PENDING" -> properties.getCustomerReturnRequestCreatedSubject();
            case "APPROVED" -> properties.getCustomerReturnRequestApprovedSubject();
            case "REJECTED" -> properties.getCustomerReturnRequestRejectedSubject();
            case "REFUND_COMPLETED" -> properties.getCustomerReturnRequestRefundCompletedSubject();
            default -> "Cập nhật yêu cầu trả hàng #%s";
        };
        return formatReturnSubject(template, request.getId());
    }

    private String formatReturnSubject(String template, Long returnId) {
        if (!StringUtils.hasText(template)) {
            return "Cập nhật yêu cầu trả hàng #" + returnId;
        }
        if (template.contains("%s")) {
            return String.format(template, returnId);
        }
        return template + " #" + returnId;
    }

    private String buildReturnRequestHtmlBody(ReturnRequestPojo request, String title) {
        String customerName = resolveReturnRequestCustomerName(request);
        String greeting = StringUtils.hasText(customerName)
            ? "Xin chào <strong>" + escapeHtml(customerName) + "</strong>,"
            : "Xin chào,";
        String statusLabel = returnStatusLabelVi(request.getStatus());
        String qcLabel = returnQcStatusLabelVi(request.getQcStatus());
        String refundMethodLabel = returnRefundMethodLabelVi(request.getRefundMethod());
        String orderId = request.getOrderId() != null ? String.valueOf(request.getOrderId()) : "—";
        String tracking = StringUtils.hasText(request.getTrackingNumber())
            ? escapeHtml(request.getTrackingNumber())
            : "—";
        String refundAmount = request.getRefundAmount() != null
            ? formatVnd(request.getRefundAmount())
            : "—";
        String summary = returnStatusSummaryVi(request);

        return ""
            + "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
            + "<body style=\"margin:0;padding:0;background-color:#f3f4f6;\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;background-color:#f3f4f6;padding:24px 12px;\">"
            + "<tr><td align=\"center\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
            + "style=\"border-collapse:collapse;max-width:560px;background-color:#ffffff;"
            + "border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(15,23,42,0.08);\">"
            + "<tr><td style=\"background:linear-gradient(135deg,#0f766e 0%,#115e59 100%);padding:20px 28px;\">"
            + "<p style=\"margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:13px;font-weight:600;letter-spacing:0.06em;color:rgba(255,255,255,0.9);\">MONO STUDIO</p>"
            + "<h1 style=\"margin:8px 0 0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:20px;font-weight:700;line-height:1.3;color:#ffffff;\">" + escapeHtml(title) + "</h1>"
            + "<p style=\"margin:6px 0 0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:13px;color:rgba(255,255,255,0.85);\">Yêu cầu trả hàng <strong style=\"color:#fff;\">#"
            + request.getId() + "</strong></p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:28px 28px 8px;\">"
            + "<p style=\"margin:0 0 16px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:15px;line-height:1.55;color:#374151;\">" + greeting + "</p>"
            + "<p style=\"margin:0 0 18px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:15px;line-height:1.55;color:#374151;\">" + escapeHtml(summary) + "</p>"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">"
            + rowHtml("Trạng thái", "<strong>" + escapeHtml(statusLabel) + "</strong>")
            + rowHtml("Đơn hàng gốc", "#" + escapeHtml(orderId))
            + rowHtml("Kiểm tra hàng", escapeHtml(qcLabel))
            + rowHtml("Mã vận đơn hoàn", tracking)
            + rowHtml("Số tiền hoàn", escapeHtml(refundAmount))
            + rowHtml("Phương thức hoàn", escapeHtml(refundMethodLabel))
            + "</table>"
            + "</td></tr>"
            + "<tr><td style=\"padding:0 28px 24px;border-top:1px solid #f3f4f6;\">"
            + "<p style=\"margin:18px 0 0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;"
            + "font-size:12px;line-height:1.5;color:#9ca3af;\">"
            + "Email được gửi tự động khi yêu cầu trả hàng được cập nhật. Nếu cần hỗ trợ, vui lòng phản hồi email này hoặc liên hệ cửa hàng.</p>"
            + "</td></tr>"
            + "</table>"
            + "</td></tr></table>"
            + "</body></html>";
    }

    private String resolveReturnRequestCustomerName(ReturnRequestPojo request) {
        if (request.getOrderId() == null) {
            return "";
        }
        return ordersRepository.findById(request.getOrderId())
            .map(Order::getCustomer)
            .filter(customer -> customer != null && customer.getPerson() != null)
            .map(customer -> customer.getPerson().getFirstName())
            .filter(StringUtils::hasText)
            .orElse("");
    }

    private static String returnStatusLabelVi(String status) {
        if (!StringUtils.hasText(status)) {
            return "Không rõ";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "Chờ duyệt";
            case "APPROVED" -> "Đã duyệt";
            case "REJECTED" -> "Từ chối";
            case "RECEIVED" -> "Đã nhận hàng về kho";
            case "REFUND_PROCESSING" -> "Đang hoàn tiền";
            case "REFUND_COMPLETED" -> "Hoàn tiền xong";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    private static String returnQcStatusLabelVi(String qcStatus) {
        if (!StringUtils.hasText(qcStatus)) {
            return "—";
        }
        return switch (qcStatus.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "Chờ kiểm tra";
            case "PASSED" -> "Đạt";
            case "FAILED" -> "Không đạt";
            default -> qcStatus;
        };
    }

    private static String returnRefundMethodLabelVi(String refundMethod) {
        if (!StringUtils.hasText(refundMethod)) {
            return "—";
        }
        return switch (refundMethod.trim().toUpperCase(Locale.ROOT)) {
            case "ORIGINAL_PAYMENT" -> "Hoàn về thanh toán gốc";
            case "STORE_CREDIT" -> "Tín dụng cửa hàng";
            case "BANK_TRANSFER" -> "Chuyển khoản ngân hàng";
            default -> refundMethod;
        };
    }

    private static String returnStatusSummaryVi(ReturnRequestPojo request) {
        String status = request.getStatus() != null ? request.getStatus().trim().toUpperCase(Locale.ROOT) : "";
        return switch (status) {
            case "PENDING" -> "Chúng tôi đã nhận yêu cầu trả hàng của bạn và sẽ xem xét trong thời gian sớm nhất.";
            case "APPROVED" -> "Yêu cầu trả hàng đã được duyệt. Vui lòng gửi hàng theo hướng dẫn hoặc mã vận đơn hoàn (nếu có).";
            case "REJECTED" -> "Yêu cầu trả hàng không được chấp nhận. Vui lòng xem ghi chú từ cửa hàng hoặc liên hệ hỗ trợ nếu cần.";
            case "RECEIVED" -> "Kiện hàng hoàn đã về kho. Cửa hàng sẽ kiểm tra chất lượng trước khi xử lý hoàn tiền.";
            case "REFUND_PROCESSING" -> "Yêu cầu của bạn đang được xử lý hoàn tiền.";
            case "REFUND_COMPLETED" -> "Hoàn tiền cho yêu cầu trả hàng đã hoàn tất.";
            case "CANCELLED" -> "Yêu cầu trả hàng đã được hủy.";
            default -> "Yêu cầu trả hàng của bạn vừa được cập nhật.";
        };
    }

    private static String formatVnd(int amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " ₫";
    }
}
