package com.reene4444.cpupinning.spring;

import com.reene4444.cpupinning.adaptive.AdaptiveAffinityEngine;
import com.reene4444.cpupinning.business.BusinessIsolationManager;
import com.reene4444.cpupinning.chaos.ChaosIntegration;
import com.reene4444.cpupinning.monitoring.CpuPinningMetrics;
import com.reene4444.cpupinning.spring.aspect.AffinityPoolAspect;
import com.reene4444.cpupinning.spring.aspect.AdaptiveAffinityAspect;
import com.reene4444.cpupinning.spring.aspect.PinnedAffinityAspect;
import com.reene4444.cpupinning.virtual.VirtualThreadPinner;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Spring Boot Auto Configuration for CPU Pinning Framework
 */
@Configuration
@EnableAspectJAutoProxy
@ConditionalOnProperty(name = "cpu.pinning.enabled", havingValue = "true", matchIfMissing = true)
public class CpuPinningAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public AdaptiveAffinityEngine adaptiveAffinityEngine(MeterRegistry meterRegistry) {
        return new AdaptiveAffinityEngine(meterRegistry);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public BusinessIsolationManager businessIsolationManager() {
        return BusinessIsolationManager.getInstance();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public VirtualThreadPinner virtualThreadPinner() {
        return new VirtualThreadPinner();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ChaosIntegration chaosIntegration() {
        return new ChaosIntegration();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CpuPinningMetrics cpuPinningMetrics(MeterRegistry meterRegistry) {
        return new CpuPinningMetrics(meterRegistry);
    }
    
    @Bean
    public AffinityPoolAspect affinityPoolAspect(BusinessIsolationManager businessIsolationManager) {
        return new AffinityPoolAspect(businessIsolationManager);
    }
    
    @Bean
    public AdaptiveAffinityAspect adaptiveAffinityAspect(AdaptiveAffinityEngine adaptiveAffinityEngine) {
        return new AdaptiveAffinityAspect(adaptiveAffinityEngine);
    }
    
    @Bean
    public PinnedAffinityAspect pinnedAffinityAspect(VirtualThreadPinner virtualThreadPinner) {
        return new PinnedAffinityAspect(virtualThreadPinner);
    }
}

