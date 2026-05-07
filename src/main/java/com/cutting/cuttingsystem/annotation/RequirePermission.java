package com.cutting.cuttingsystem.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /** Permission code(s) required. The user must have at least one. */
    String[] value() default {};
}
