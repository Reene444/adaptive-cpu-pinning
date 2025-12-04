package com.reene4444.cpupinning.spring.annotation;

import com.reene4444.cpupinning.core.WorkloadType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for adaptive CPU pinning based on workload type
 * 
 * Example:
 * <pre>
 * {@code @AdaptiveAffinity(workloadType = WorkloadType.CPU_INTENSIVE)
 * public void processData() { }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdaptiveAffinity {
    /**
     * Workload type for adaptive CPU selection
     */
    WorkloadType workloadType() default WorkloadType.MIXED;
    
    /**
     * Enable NUMA-aware pinning
     */
    boolean numaAware() default false;
}

