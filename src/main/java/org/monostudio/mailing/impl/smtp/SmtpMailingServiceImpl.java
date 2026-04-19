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
import org.monostudio.mailing.MailingProperties;
import org.monostudio.mailing.MailingService;
import org.monostudio.mailing.MailingServiceException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@Profile("smtp")
public class SmtpMailingServiceImpl implements MailingService {
    private final Logger logger = LoggerFactory.getLogger(SmtpMailingServiceImpl.class);
    private final MailingProperties properties;
    private final JavaMailSender javaMailSender;

    @Autowired
    public SmtpMailingServiceImpl(MailingProperties properties, JavaMailSender javaMailSender) {
        this.properties = properties;
        this.javaMailSender = javaMailSender;
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
        String subject = "Cập nhật yêu cầu trả hàng #" + request.getId();
        String text = "<p>Yêu cầu trả hàng của bạn hiện đang ở trạng thái: <b>" + request.getStatus() + "</b></p>";
        // Assuming we could fetch email, but for simplicity:
        // sendEmail(customerEmail, subject, text);
        logger.info("SMTP: Return request status to client: {}", subject);
    }

    @Override
    public void notifyReturnRequestToOwners(ReturnRequestPojo request) throws MailingServiceException {
        String subject = "[Admin] Yêu cầu trả hàng mới #" + request.getId();
        String text = "<p>Có yêu cầu trả hàng mới cần xử lý.</p>";
        sendEmail(properties.getOwnerEmail(), subject, text);
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
}
