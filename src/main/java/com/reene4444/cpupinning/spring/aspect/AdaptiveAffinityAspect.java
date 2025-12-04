package com.reene4444.cpupinning.spring.aspect;

import com.reene4444.cpupinning.adaptive.AdaptiveAffinityEngine;
import com.reene4444.cpupinning.spring.annotation.AdaptiveAffinity;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect for @AdaptiveAffinity annotation
 */
@Aspect
@Component
public class AdaptiveAffinityAspect {
    
    private final AdaptiveAffinityEngine adaptiveAffinityEngine;
    
    @Autowired
    public AdaptiveAffinityAspect(AdaptiveAffinityEngine adaptiveAffinityEngine) {
        this.adaptiveAffinityEngine = adaptiveAffinityEngine;
    }
    
    @Around("@within(adaptiveAffinity) || @annotation(adaptiveAffinity)")
    public Object applyAdaptiveAffinity(ProceedingJoinPoint joinPoint, AdaptiveAffinity adaptiveAffinity) 
            throws Throwable {
        Thread currentThread = Thread.currentThread();
        
        if (adaptiveAffinity.numaAware()) {
            adaptiveAffinityEngine.numaAwarePin(currentThread);
        } else {
            adaptiveAffinityEngine.autoPinByWorkload(currentThread, adaptiveAffinity.workloadType());
        }
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long latency = System.currentTimeMillis() - startTime;
            adaptiveAffinityEngine.recordLatency(currentThread, latency);
            return result;
        } catch (Throwable e) {
            long latency = System.currentTimeMillis() - startTime;
            adaptiveAffinityEngine.recordLatency(currentThread, latency);
            throw e;
        }
    }
}

