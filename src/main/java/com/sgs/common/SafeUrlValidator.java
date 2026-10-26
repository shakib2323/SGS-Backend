package com.sgs.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafeUrlValidator implements ConstraintValidator<SafeUrl, String> {
    private int max;

    @Override
    public void initialize(SafeUrl annotation) {
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return UrlRules.isSafeHttpUrl(value, max);
    }
}
