package org.monostudio.common.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.common.services.RegexMatcherAdapterService;
import org.monostudio.config.ValidationProperties;

import java.util.regex.Pattern;

import static org.monostudio.config.Constants.JWT_PREFIX;

@Service
public class RegexMatcherAdapterServiceImpl
    implements RegexMatcherAdapterService {
    private final ValidationProperties validationProperties;
    private Pattern idNumberPattern = null;
    private Pattern jwtTokenPattern = null;

    @Autowired
    public RegexMatcherAdapterServiceImpl(
        ValidationProperties validationProperties
    ) {
        this.validationProperties = validationProperties;
    }

    @Override
    public boolean isAValidIdNumber(String matchAgainst) {
        if (this.idNumberPattern==null) {
            this.idNumberPattern = Pattern.compile(validationProperties.getIdNumberRegexp());
        }
        return this.idNumberPattern.matcher(matchAgainst).matches();
    }

    @Override
    public boolean isAValidAuthorizationHeader(String matchAgainst) {
        if (this.jwtTokenPattern==null) {
            this.jwtTokenPattern = Pattern.compile("^" + JWT_PREFIX + ".+$");
        }
        return this.jwtTokenPattern.matcher(matchAgainst).matches();
    }
}
