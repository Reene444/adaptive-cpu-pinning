package com.reene4444.cpupinning.spring.aspect;

import com.reene4444.cpupinning.spring.annotation.PinnedAffinity;
import com.reene4444.cpupinning.virtual.VirtualThreadPinner;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AOP Aspect for @PinnedAffinity annotation - handles virtual thread executor creation
 */
@Aspect
@Component
public class PinnedAffinityAspect {
    
    private final VirtualThreadPinner virtualThreadPinner;
    
    @Autowired
    public PinnedAffinityAspect(VirtualThreadPinner virtualThreadPinner) {
        this.virtualThreadPinner = virtualThreadPinner;
    }
    
    @Around("@annotation(pinnedAffinity) && execution(* *(..))")
    public Object createPinnedExecutor(ProceedingJoinPoint joinPoint, PinnedAffinity pinnedAffinity) 
            throws Throwable {
        Set<Integer> cores = Arrays.stream(pinnedAffinity.cores())
                .boxed()
                .collect(Collectors.toSet());
        
        // For methods returning ExecutorService, wrap with pinned executor
        Object result = joinPoint.proceed();
        
        if (result instanceof java.util.concurrent.ExecutorService) {
            return virtualThreadPinner.createPinnedExecutor(cores);
        }
        
        return result;
    }
}

