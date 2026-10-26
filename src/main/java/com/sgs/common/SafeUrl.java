package com.sgs.common;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Optional http(s) URL. Blocks javascript:, data: and other schemes (stored-XSS defence). */
@Documented
@Constraint(validatedBy = SafeUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeUrl {
    String message() default "must be a valid http(s) URL";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int max() default 500;
}
