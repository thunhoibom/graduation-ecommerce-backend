package org.monostudio.jpa.services.patch.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.services.patch.ReturnRequestsPatchService;

import java.time.Instant;
import java.util.Map;

@Transactional
@Service
public class ReturnRequestsPatchServiceImpl
    implements ReturnRequestsPatchService {
    private final org.monostudio.jpa.repositories.ReturnRequestsRepository returnRequestsRepository;

    @Autowired
    public ReturnRequestsPatchServiceImpl(
        org.monostudio.jpa.repositories.ReturnRequestsRepository returnRequestsRepository
    ) {
        this.returnRequestsRepository = returnRequestsRepository;
    }

    @Transactional
    @Override
    public ReturnRequest patchExistingEntity(Map<String, Object> changes, ReturnRequest existing) throws BadInputException {
        ReturnRequest target = new ReturnRequest(existing);

        try {
            if (changes.containsKey("status")) {
                String statusValue = (String) changes.get("status");
                if (!StringUtils.isBlank(statusValue)) {
                    target.setStatus(ReturnRequest.ReturnRequestStatus.valueOf(statusValue));
                }
            }

            if (changes.containsKey("adminNotes")) {
                String adminNotes = (String) changes.get("adminNotes");
                target.setAdminNotes(adminNotes);
            }

            if (changes.containsKey("refundMethod")) {
                String refundMethod = (String) changes.get("refundMethod");
                if (!StringUtils.isBlank(refundMethod)) {
                    target.setRefundMethod(ReturnRequest.RefundMethod.valueOf(refundMethod));
                }
            }

            if (changes.containsKey("refundAmount")) {
                Object refundAmount = changes.get("refundAmount");
                if (refundAmount instanceof Number) {
                    target.setRefundAmount(((Number) refundAmount).intValue());
                }
            }

            if (changes.containsKey("trackingNumber")) {
                String trackingNumber = (String) changes.get("trackingNumber");
                target.setTrackingNumber(trackingNumber);
            }

            if (changes.containsKey("refundBankName")) {
                target.setRefundBankName((String) changes.get("refundBankName"));
            }

            if (changes.containsKey("refundBankAccountNumber")) {
                target.setRefundBankAccountNumber((String) changes.get("refundBankAccountNumber"));
            }

            if (changes.containsKey("refundBankAccountHolder")) {
                target.setRefundBankAccountHolder((String) changes.get("refundBankAccountHolder"));
            }

            if (changes.containsKey("refundProofUrl")) {
                target.setRefundProofUrl((String) changes.get("refundProofUrl"));
            }

            if (changes.containsKey("refundReference")) {
                target.setRefundReference((String) changes.get("refundReference"));
            }

            if (changes.containsKey("refundedAt")) {
                Object refundedAt = changes.get("refundedAt");
                if (refundedAt instanceof String text && !StringUtils.isBlank(text)) {
                    target.setRefundedAt(Instant.parse(text.trim()));
                }
            }

            if (changes.containsKey("reason")) {
                String reason = (String) changes.get("reason");
                target.setReason(reason);
            }
        } catch (ClassCastException | IllegalArgumentException ex) {
            throw new BadInputException("Invalid value in patch data: " + ex.getMessage());
        }

        return target;
    }

    @Override
    public ReturnRequest patchExistingEntity(ReturnRequestPojo changes, ReturnRequest existing) throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
