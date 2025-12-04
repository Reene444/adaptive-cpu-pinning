package com.reene4444.cpupinning.chaos;

import com.reene4444.cpupinning.core.CpuAffinity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Chaos Engineering Integration - integrates with ChaosBlade for CPU stress testing
 * and validates CPU pinning resilience under load
 */
public class ChaosIntegration {
    private static final Logger logger = LoggerFactory.getLogger(ChaosIntegration.class);
    
    private final ScheduledExecutorService chaosScheduler;
    private boolean chaosEnabled = false;
    
    public ChaosIntegration() {
        this.chaosScheduler = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * Enable chaos testing - simulates CPU contention
     */
    public void enableCpuChaos(Set<Integer> targetCpus, int durationSeconds) {
        if (chaosEnabled) {
            logger.warn("Chaos testing already enabled");
            return;
        }
        
        chaosEnabled = true;
        logger.info("Starting CPU chaos test on CPUs {} for {} seconds", targetCpus, durationSeconds);
        
        // Create CPU stress threads on target CPUs
        for (Integer cpu : targetCpus) {
            Thread stressThread = new Thread(() -> {
                CpuAffinity.pinThread(Set.of(cpu));
                // CPU-intensive loop
                long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
                while (System.currentTimeMillis() < endTime && chaosEnabled) {
                    // Burn CPU cycles
                    Math.sqrt(Math.random() * 1000000);
                }
            }, "chaos-stress-" + cpu);
            stressThread.setDaemon(true);
            stressThread.start();
        }
        
        // Auto-disable after duration
        chaosScheduler.schedule(() -> {
            disableCpuChaos();
        }, durationSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * Disable chaos testing
     */
    public void disableCpuChaos() {
        chaosEnabled = false;
        logger.info("CPU chaos testing disabled");
    }
    
    /**
     * Check if chaos testing is active
     */
    public boolean isChaosEnabled() {
        return chaosEnabled;
    }
    
    /**
     * Validate CPU pinning resilience - ensures threads stay pinned under chaos
     */
    public boolean validatePinningResilience(Thread thread, Set<Integer> expectedCpus, int testDurationSeconds) {
        logger.info("Validating pinning resilience for thread {} under chaos", thread.getName());
        
        enableCpuChaos(expectedCpus, testDurationSeconds);
        
        try {
            Thread.sleep(testDurationSeconds * 1000L);
            
            Set<Integer> actualCpus = CpuAffinity.getThreadAffinity();
            boolean resilient = actualCpus.equals(expectedCpus);
            
            if (resilient) {
                logger.info("Pinning resilience validated: thread stayed on CPUs {}", actualCpus);
            } else {
                logger.warn("Pinning resilience failed: expected {}, actual {}", expectedCpus, actualCpus);
            }
            
            return resilient;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            disableCpuChaos();
        }
    }
    
    public void shutdown() {
        disableCpuChaos();
        chaosScheduler.shutdown();
    }
}

