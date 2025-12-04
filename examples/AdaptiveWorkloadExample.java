package examples;

import com.reene4444.cpupinning.core.WorkloadType;
import com.reene4444.cpupinning.spring.annotation.AdaptiveAffinity;
import org.springframework.stereotype.Service;

/**
 * Example: Adaptive CPU Pinning
 * Automatically selects optimal CPUs based on workload type
 */
@Service
public class AdaptiveWorkloadExample {
    
    @AdaptiveAffinity(workloadType = WorkloadType.CPU_INTENSIVE)
    public void processDataIntensive() {
        // Automatically pinned to CPU-intensive cores
        // Framework monitors latency and QPS, may migrate if needed
        System.out.println("Processing CPU-intensive workload");
    }
    
    @AdaptiveAffinity(workloadType = WorkloadType.IO_INTENSIVE)
    public void processIOIntensive() {
        // Automatically pinned to IO-intensive cores
        System.out.println("Processing IO-intensive workload");
    }
    
    @AdaptiveAffinity(workloadType = WorkloadType.LOW_LATENCY, numaAware = true)
    public void processLowLatency() {
        // Pinned to dedicated low-latency cores with NUMA awareness
        System.out.println("Processing low-latency workload");
    }
}

