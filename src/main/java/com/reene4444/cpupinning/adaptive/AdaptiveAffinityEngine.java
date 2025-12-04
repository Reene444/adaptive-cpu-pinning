package com.reene4444.cpupinning.adaptive;

import com.reene4444.cpupinning.core.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptive CPU Affinity Engine - dynamically adjusts thread CPU binding
 * based on workload type, QPS, latency, and GC pressure
 */
public class AdaptiveAffinityEngine {
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveAffinityEngine.class);
    
    private final NumaTopology numaTopology;
    private final MeterRegistry meterRegistry;
    private final Map<Thread, ThreadMetrics> threadMetrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final AtomicLong migrationCount = new AtomicLong(0);
    
    // CPU core allocation strategy
    private final Map<WorkloadType, Set<Integer>> workloadCpuMapping = new ConcurrentHashMap<>();
    private final Set<Integer> availableCpus;
    
    public AdaptiveAffinityEngine(MeterRegistry meterRegistry) {
        this.numaTopology = NumaTopology.getInstance();
        this.meterRegistry = meterRegistry;
        this.availableCpus = new HashSet<>();
        int cpuCount = CpuAffinity.getAvailableCpuCount();
        for (int i = 0; i < cpuCount; i++) {
            availableCpus.add(i);
        }
        this.scheduler = Executors.newScheduledThreadPool(1);
        initializeWorkloadMapping();
        startAdaptiveScheduler();
    }
    
    /**
     * Auto-pin thread based on workload type with adaptive behavior
     */
    public void autoPinByWorkload(Thread thread, WorkloadType type) {
        Set<Integer> preferredCpus = selectOptimalCpus(type);
        if (preferredCpus.isEmpty()) {
            preferredCpus = availableCpus;
        }
        
        boolean success = CpuAffinity.pinThread(preferredCpus);
        if (success) {
            threadMetrics.put(thread, new ThreadMetrics(type, preferredCpus));
            logger.info("Auto-pinned thread {} to CPUs {} for workload type {}", 
                       thread.getName(), preferredCpus, type);
        }
    }
    
    /**
     * NUMA-aware CPU pinning - prioritizes local NUMA node CPUs
     */
    public void numaAwarePin(Thread thread) {
        List<Integer> localCpus = numaTopology.getLocalNumaCpus();
        if (localCpus.isEmpty()) {
            localCpus = new ArrayList<>(availableCpus);
        }
        
        Set<Integer> cpus = new HashSet<>(localCpus);
        boolean success = CpuAffinity.pinThread(cpus);
        if (success) {
            threadMetrics.put(thread, new ThreadMetrics(WorkloadType.MIXED, cpus));
            logger.info("NUMA-aware pinned thread {} to local CPUs {}", 
                       thread.getName(), cpus);
        }
    }
    
    /**
     * Record metrics for adaptive decision making
     */
    public void recordLatency(Thread thread, long latencyMs) {
        ThreadMetrics metrics = threadMetrics.get(thread);
        if (metrics != null) {
            metrics.recordLatency(latencyMs);
        }
    }
    
    public void recordQps(Thread thread, long qps) {
        ThreadMetrics metrics = threadMetrics.get(thread);
        if (metrics != null) {
            metrics.recordQps(qps);
        }
    }
    
    /**
     * Select optimal CPUs based on workload type
     */
    private Set<Integer> selectOptimalCpus(WorkloadType type) {
        Set<Integer> cpus = workloadCpuMapping.get(type);
        if (cpus == null || cpus.isEmpty()) {
            // Default: use first half of CPUs for CPU-intensive, second half for IO-intensive
            int cpuCount = availableCpus.size();
            cpus = new HashSet<>();
            
            switch (type) {
                case CPU_INTENSIVE:
                    // Use first half CPUs
                    for (int i = 0; i < cpuCount / 2; i++) {
                        cpus.add(i);
                    }
                    break;
                case IO_INTENSIVE:
                    // Use second half CPUs
                    for (int i = cpuCount / 2; i < cpuCount; i++) {
                        cpus.add(i);
                    }
                    break;
                case LOW_LATENCY:
                    // Use dedicated cores (first 2-4 cores)
                    int dedicatedCores = Math.min(4, cpuCount);
                    for (int i = 0; i < dedicatedCores; i++) {
                        cpus.add(i);
                    }
                    break;
                default:
                    cpus = new HashSet<>(availableCpus);
            }
        }
        return cpus;
    }
    
    private void initializeWorkloadMapping() {
        // Initialize default CPU allocation per workload type
        int cpuCount = availableCpus.size();
        int half = cpuCount / 2;
        
        Set<Integer> cpuIntensive = new HashSet<>();
        Set<Integer> ioIntensive = new HashSet<>();
        
        for (int i = 0; i < half; i++) {
            cpuIntensive.add(i);
        }
        for (int i = half; i < cpuCount; i++) {
            ioIntensive.add(i);
        }
        
        workloadCpuMapping.put(WorkloadType.CPU_INTENSIVE, cpuIntensive);
        workloadCpuMapping.put(WorkloadType.IO_INTENSIVE, ioIntensive);
        workloadCpuMapping.put(WorkloadType.MIXED, new HashSet<>(availableCpus));
    }
    
    /**
     * Adaptive scheduler that periodically reviews and migrates threads
     */
    private void startAdaptiveScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                reviewAndMigrate();
            } catch (Exception e) {
                logger.error("Error in adaptive scheduler", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    private void reviewAndMigrate() {
        for (Map.Entry<Thread, ThreadMetrics> entry : threadMetrics.entrySet()) {
            Thread thread = entry.getKey();
            ThreadMetrics metrics = entry.getValue();
            
            if (!thread.isAlive()) {
                threadMetrics.remove(thread);
                continue;
            }
            
            // Check if migration is needed based on metrics
            if (shouldMigrate(metrics)) {
                Set<Integer> newCpus = selectOptimalCpus(metrics.workloadType);
                if (!newCpus.equals(metrics.currentCpus)) {
                    CpuAffinity.pinThread(newCpus);
                    metrics.currentCpus = newCpus;
                    migrationCount.incrementAndGet();
                    logger.debug("Migrated thread {} to CPUs {}", thread.getName(), newCpus);
                }
            }
        }
        
        if (meterRegistry != null) {
            meterRegistry.counter("cpu.pinning.migrations").increment();
        }
    }
    
    private boolean shouldMigrate(ThreadMetrics metrics) {
        // Migrate if latency is high or QPS is low
        return metrics.getAverageLatency() > 100 || metrics.getAverageQps() < 100;
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
    
    /**
     * Thread metrics for adaptive decision making
     */
    private static class ThreadMetrics {
        final WorkloadType workloadType;
        Set<Integer> currentCpus;
        private final Queue<Long> latencyHistory = new ConcurrentLinkedQueue<>();
        private final Queue<Long> qpsHistory = new ConcurrentLinkedQueue<>();
        private static final int HISTORY_SIZE = 10;
        
        ThreadMetrics(WorkloadType workloadType, Set<Integer> cpus) {
            this.workloadType = workloadType;
            this.currentCpus = cpus;
        }
        
        void recordLatency(long latencyMs) {
            latencyHistory.offer(latencyMs);
            if (latencyHistory.size() > HISTORY_SIZE) {
                latencyHistory.poll();
            }
        }
        
        void recordQps(long qps) {
            qpsHistory.offer(qps);
            if (qpsHistory.size() > HISTORY_SIZE) {
                qpsHistory.poll();
            }
        }
        
        double getAverageLatency() {
            return latencyHistory.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }
        
        double getAverageQps() {
            return qpsHistory.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }
    }
}

