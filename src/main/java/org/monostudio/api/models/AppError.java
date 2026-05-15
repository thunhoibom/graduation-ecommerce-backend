package org.monostudio.api.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppError {
    private String code;
    private String message;
    private String detailMessage;
    private boolean canRetry;
}
