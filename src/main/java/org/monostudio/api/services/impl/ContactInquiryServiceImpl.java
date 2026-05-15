package org.monostudio.api.services.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ContactInquiryCreatedResponse;
import org.monostudio.api.models.ContactInquiryRequest;
import org.monostudio.api.services.ContactInquiryService;
import org.monostudio.jpa.entities.ContactInquiry;
import org.monostudio.jpa.repositories.ContactInquiriesRepository;
import org.monostudio.mailing.MailingService;
import org.monostudio.mailing.MailingServiceException;

@Service
public class ContactInquiryServiceImpl implements ContactInquiryService {

    private static final Logger log = LoggerFactory.getLogger(ContactInquiryServiceImpl.class);

    private final ContactInquiriesRepository contactInquiriesRepository;
    private final MailingService mailingService;

    public ContactInquiryServiceImpl(
        ContactInquiriesRepository contactInquiriesRepository,
        MailingService mailingService
    ) {
        this.contactInquiriesRepository = contactInquiriesRepository;
        this.mailingService = mailingService;
    }

    @Override
    @Transactional
    public ContactInquiryCreatedResponse submit(ContactInquiryRequest request, HttpServletRequest httpRequest) {
        ContactInquiry entity = new ContactInquiry();
        entity.setName(trim(request.getName()));
        entity.setEmail(trim(request.getEmail()));
        entity.setPhone(blankToNull(trim(request.getPhone())));
        entity.setSubject(request.getSubject());
        entity.setMessage(trim(request.getMessage()));
        entity.setSubmitterIp(truncate(resolveClientIp(httpRequest), 64));
        setUserAgentTruncated(entity, httpRequest.getHeader("User-Agent"));

        ContactInquiry saved = contactInquiriesRepository.saveAndFlush(entity);

        try {
            mailingService.notifyContactInquiryToOwners(
                saved.getName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getSubject(),
                saved.getMessage(),
                saved.getId()
            );
        } catch (MailingServiceException ex) {
            log.warn("Contact inquiry {} saved but notification email failed: {}", saved.getId(), ex.getMessage());
        }

        return new ContactInquiryCreatedResponse(saved.getId());
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static void setUserAgentTruncated(ContactInquiry entity, String raw) {
        if (raw == null || raw.isEmpty()) {
            entity.setUserAgent(null);
            return;
        }
        entity.setUserAgent(raw.length() > 512 ? raw.substring(0, 512) : raw);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen);
    }
}
