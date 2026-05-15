package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Collection;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class ReturnRequestPojo {
    private Long id;
    private Instant date;
    private Instant lastModified;
    @NotBlank
    private String reason;
    private String adminNotes;
    private String status;
    @NotNull
    private String refundMethod;
    private Integer refundAmount;
    private String trackingNumber;
    private String refundBankName;
    private String refundBankAccountNumber;
    private String refundBankAccountHolder;
    private String refundProofUrl;
    private String refundReference;
    private Instant refundedAt;
    private String qcStatus;
    private String qcNotes;
    private String qcPhotoUrls;
    private Instant qcCompletedAt;
    private Long orderId;
    private String orderRecipientName;
    private String orderRecipientPhone;
    @Valid
    @NotEmpty
    private Collection<ReturnRequestItemPojo> items;
}
