package com.reene4444.cpupinning.core;

/**
 * Workload type classification for adaptive CPU pinning
 */
public enum WorkloadType {
    /**
     * CPU-intensive workloads: computation, encryption, compression
     */
    CPU_INTENSIVE,
    
    /**
     * IO-intensive workloads: network I/O, disk I/O, database queries
     */
    IO_INTENSIVE,
    
    /**
     * Mixed workloads with both CPU and IO operations
     */
    MIXED,
    
    /**
     * Memory-intensive workloads: large data processing, caching
     */
    MEMORY_INTENSIVE,
    
    /**
     * Low-latency critical workloads: real-time processing, trading systems
     */
    LOW_LATENCY
}

