package com.reene4444.cpupinning.spring.aspect;

import com.reene4444.cpupinning.business.BusinessIsolationManager;
import com.reene4444.cpupinning.spring.annotation.AffinityPool;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AOP Aspect for @AffinityPool annotation
 */
@Aspect
@Component
public class AffinityPoolAspect {
    private static final Logger logger = LoggerFactory.getLogger(AffinityPoolAspect.class);
    
    private final BusinessIsolationManager businessIsolationManager;
    
    @Autowired
    public AffinityPoolAspect(BusinessIsolationManager businessIsolationManager) {
        this.businessIsolationManager = businessIsolationManager;
    }
    
    @Around("@within(affinityPool) || @annotation(affinityPool)")
    public Object pinToBusinessPool(ProceedingJoinPoint joinPoint, AffinityPool affinityPool) throws Throwable {
        String businessName = affinityPool.value();
        Set<Integer> cpus = Arrays.stream(affinityPool.cpus())
                .boxed()
                .collect(Collectors.toSet());
        
        // Register pool if not exists
        if (businessIsolationManager.getBusinessPool(businessName) == null && !cpus.isEmpty()) {
            businessIsolationManager.registerBusinessPool(businessName, cpus);
        }
        
        // Pin current thread
        Thread currentThread = Thread.currentThread();
        boolean pinned = businessIsolationManager.pinToBusiness(currentThread, businessName);
        
        if (!pinned) {
            logger.warn("Failed to pin thread {} to business pool {}", 
                       currentThread.getName(), businessName);
        }
        
        try {
            return joinPoint.proceed();
        } finally {
            // Optionally unpin after method execution
        }
    }
}

