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
    @NotNull
    private String status;
    @NotNull
    private String refundMethod;
    private Integer refundAmount;
    private String trackingNumber;
    private Long orderId;
    @Valid
    @NotEmpty
    private Collection<ReturnRequestItemPojo> items;
}
