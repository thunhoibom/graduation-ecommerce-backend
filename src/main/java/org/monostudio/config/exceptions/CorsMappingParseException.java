package org.monostudio.config.exceptions;

import lombok.Getter;

public class CorsMappingParseException
    extends Exception {
    public static final String BASE_MESSAGE = "Could not parse CORS mapping. Format must be 'METHODS /path'.";
    @Getter
    private final String corsMapping;

    public CorsMappingParseException(String corsMapping) {
        this.corsMapping = corsMapping;
    }

    @Override
    public String getMessage() {
        return BASE_MESSAGE;
    }
}
