package com.reene4444.cpupinning.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to assign a class/service to a specific CPU pool for business isolation
 * 
 * Example:
 * <pre>
 * {@code @AffinityPool("payment-service", cpus = {0,1,2,3})
 * @Service
 * public class PaymentService { }
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AffinityPool {
    /**
     * Business service name
     */
    String value();
    
    /**
     * CPU cores allocated to this business pool
     */
    int[] cpus() default {};
}

