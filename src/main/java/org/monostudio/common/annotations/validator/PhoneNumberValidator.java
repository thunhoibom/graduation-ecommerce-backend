package org.monostudio.common.annotations.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.monostudio.common.annotations.PhoneNumber;
import org.monostudio.config.ValidationProperties;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    @Autowired
    private ValidationProperties validationProperties;

    private Pattern pattern;

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        pattern = Pattern.compile(validationProperties.getPhoneNumberRegexp());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }
}
