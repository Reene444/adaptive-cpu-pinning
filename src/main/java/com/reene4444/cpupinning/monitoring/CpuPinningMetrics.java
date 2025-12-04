package com.reene4444.cpupinning.monitoring;

import com.reene4444.cpupinning.core.CpuAffinity;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collection for CPU pinning framework
 */
public class CpuPinningMetrics {
    
    private final MeterRegistry meterRegistry;
    private final AtomicLong pinnedThreads = new AtomicLong(0);
    private final AtomicLong migrations = new AtomicLong(0);
    
    public CpuPinningMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        registerMetrics();
    }
    
    private void registerMetrics() {
        Gauge.builder("cpu.pinning.available.cpus", () -> CpuAffinity.getAvailableCpuCount())
                .description("Number of available CPU cores")
                .register(meterRegistry);
        
        Gauge.builder("cpu.pinning.pinned.threads", pinnedThreads::get)
                .description("Number of threads with CPU affinity set")
                .register(meterRegistry);
        
        Gauge.builder("cpu.pinning.migrations", migrations::get)
                .description("Total number of CPU migrations")
                .register(meterRegistry);
    }
    
    public void recordThreadPinned() {
        pinnedThreads.incrementAndGet();
        meterRegistry.counter("cpu.pinning.thread.pinned").increment();
    }
    
    public void recordThreadUnpinned() {
        pinnedThreads.decrementAndGet();
        meterRegistry.counter("cpu.pinning.thread.unpinned").increment();
    }
    
    public void recordMigration() {
        migrations.incrementAndGet();
        meterRegistry.counter("cpu.pinning.migration").increment();
    }
    
    public void recordLatency(String operation, long latencyMs) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("cpu.pinning.operation.latency")
                .tag("operation", operation)
                .register(meterRegistry));
    }
}

