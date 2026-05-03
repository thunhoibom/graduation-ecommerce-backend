package org.monostudio.api.services.impl;

import org.monostudio.api.models.CheckoutOtpInitiateResponse;
import org.monostudio.api.services.CheckoutOtpService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderOtp;
import org.monostudio.jpa.repositories.OrderOtpRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.mailing.MailingService;
import org.monostudio.mailing.MailingServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class CheckoutOtpServiceImpl implements CheckoutOtpService {
    private static final int OTP_LENGTH = 6;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_TTL_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final OrdersRepository ordersRepository;
    private final OrderOtpRepository orderOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailingService mailingService;
    private final SecureRandom secureRandom = new SecureRandom();

    public CheckoutOtpServiceImpl(
        OrdersRepository ordersRepository,
        OrderOtpRepository orderOtpRepository,
        PasswordEncoder passwordEncoder,
        MailingService mailingService
    ) {
        this.ordersRepository = ordersRepository;
        this.orderOtpRepository = orderOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailingService = mailingService;
    }

    @Override
    public CheckoutOtpInitiateResponse issueOtpForOrder(Order order) throws BadInputException {
        String email = resolveCustomerEmail(order);
        String otpCode = generateOtp();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES);

        OrderOtp entity = OrderOtp.builder()
            .order(order)
            .email(email)
            .hash(passwordEncoder.encode(otpCode))
            .expiresAt(expiresAt)
            .attemptCount(0)
            .resendCount(0)
            .lastSentAt(now)
            .used(false)
            .build();
        orderOtpRepository.save(entity);
        sendOtpMail(email, order.getId(), otpCode, expiresAt);

        return CheckoutOtpInitiateResponse.builder()
            .orderId(order.getId())
            .maskedEmail(maskEmail(email))
            .expiresAt(expiresAt)
            .resendAfterSeconds(RESEND_COOLDOWN_SECONDS)
            .build();
    }

    @Override
    public CheckoutOtpInitiateResponse resendOtpForOrder(Long orderId) throws BadInputException {
        Order order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        String email = resolveCustomerEmail(order);

        OrderOtp existing = orderOtpRepository.findTopByOrderIdAndUsedFalseOrderByCreatedAtDesc(orderId)
            .orElseThrow(() -> new BadInputException("No active OTP found for this order"));

        Instant now = Instant.now();
        if (existing.getLastSentAt() != null
            && existing.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new BadInputException("Please wait before requesting a new OTP");
        }

        existing.setUsed(true);
        orderOtpRepository.save(existing);

        String otpCode = generateOtp();
        Instant expiresAt = now.plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES);
        OrderOtp replacement = OrderOtp.builder()
            .order(order)
            .email(email)
            .hash(passwordEncoder.encode(otpCode))
            .expiresAt(expiresAt)
            .attemptCount(0)
            .resendCount(existing.getResendCount() + 1)
            .lastSentAt(now)
            .used(false)
            .build();
        orderOtpRepository.save(replacement);
        sendOtpMail(email, orderId, otpCode, expiresAt);

        return CheckoutOtpInitiateResponse.builder()
            .orderId(orderId)
            .maskedEmail(maskEmail(email))
            .expiresAt(expiresAt)
            .resendAfterSeconds(RESEND_COOLDOWN_SECONDS)
            .build();
    }

    @Override
    public void verifyOtp(Long orderId, String otpCode) throws BadInputException {
        OrderOtp otp = orderOtpRepository.findTopByOrderIdAndUsedFalseOrderByCreatedAtDesc(orderId)
            .orElseThrow(() -> new BadInputException("No active OTP found for this order"));

        Instant now = Instant.now();
        if (otp.isUsed() || otp.getExpiresAt().isBefore(now)) {
            otp.setUsed(true);
            orderOtpRepository.save(otp);
            throw new BadInputException("OTP is expired");
        }

        if (otp.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
            otp.setUsed(true);
            orderOtpRepository.save(otp);
            throw new BadInputException("OTP attempt limit exceeded");
        }

        if (!passwordEncoder.matches(otpCode, otp.getHash())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            if (otp.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
                otp.setUsed(true);
            }
            orderOtpRepository.save(otp);
            throw new BadInputException("OTP is invalid");
        }

        otp.setUsed(true);
        otp.setVerifiedAt(now);
        orderOtpRepository.save(otp);
    }

    private String resolveCustomerEmail(Order order) throws BadInputException {
        if (order == null
            || order.getCustomer() == null
            || order.getCustomer().getPerson() == null
            || order.getCustomer().getPerson().getEmail() == null
            || order.getCustomer().getPerson().getEmail().isBlank()) {
            throw new BadInputException("Customer email is required for OTP");
        }
        return order.getCustomer().getPerson().getEmail().trim();
    }

    private void sendOtpMail(String email, Long orderId, String otpCode, Instant expiresAt) throws BadInputException {
        try {
            mailingService.notifyCheckoutOtp(email, orderId, otpCode, expiresAt);
        } catch (MailingServiceException ex) {
            throw new BadInputException("Failed to send OTP email");
        }
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int value = secureRandom.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", value);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(Math.max(0, atIndex));
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
