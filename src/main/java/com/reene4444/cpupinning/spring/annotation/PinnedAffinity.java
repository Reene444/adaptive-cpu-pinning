package com.reene4444.cpupinning.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to pin virtual threads or methods to specific CPU cores
 * 
 * Example:
 * <pre>
 * {@code @PinnedAffinity(cores = {0,2,4})
 * public VirtualThreadExecutor pinnedExecutor() { }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PinnedAffinity {
    /**
     * CPU cores to pin to
     */
    int[] cores();
}

