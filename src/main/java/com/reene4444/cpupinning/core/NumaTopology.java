package com.reene4444.cpupinning.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * NUMA (Non-Uniform Memory Access) topology detection and management
 */
public class NumaTopology {
    private static final Logger logger = LoggerFactory.getLogger(NumaTopology.class);
    private static NumaTopology instance;
    
    private final Map<Integer, List<Integer>> numaNodeToCpus = new HashMap<>();
    private final Map<Integer, Integer> cpuToNumaNode = new HashMap<>();
    
    private NumaTopology() {
        detectTopology();
    }
    
    public static synchronized NumaTopology getInstance() {
        if (instance == null) {
            instance = new NumaTopology();
        }
        return instance;
    }
    
    /**
     * Detect NUMA topology from system
     */
    private void detectTopology() {
        // Try to read from /sys/devices/system/node/node*/cpulist on Linux
        try {
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                detectLinuxNumaTopology();
            } else {
                // Fallback: assume single NUMA node
                int cpuCount = CpuAffinity.getAvailableCpuCount();
                numaNodeToCpus.put(0, new ArrayList<>());
                for (int i = 0; i < cpuCount; i++) {
                    numaNodeToCpus.get(0).add(i);
                    cpuToNumaNode.put(i, 0);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to detect NUMA topology, assuming single node", e);
            int cpuCount = CpuAffinity.getAvailableCpuCount();
            numaNodeToCpus.put(0, new ArrayList<>());
            for (int i = 0; i < cpuCount; i++) {
                numaNodeToCpus.get(0).add(i);
                cpuToNumaNode.put(i, 0);
            }
        }
    }
    
    private void detectLinuxNumaTopology() {
        // Simplified detection - in production, read from /sys filesystem
        int cpuCount = CpuAffinity.getAvailableCpuCount();
        // Assume 2 NUMA nodes for multi-socket systems
        int cpusPerNode = cpuCount / 2;
        
        for (int node = 0; node < 2; node++) {
            List<Integer> cpus = new ArrayList<>();
            for (int i = node * cpusPerNode; i < (node + 1) * cpusPerNode && i < cpuCount; i++) {
                cpus.add(i);
                cpuToNumaNode.put(i, node);
            }
            if (!cpus.isEmpty()) {
                numaNodeToCpus.put(node, cpus);
            }
        }
        
        if (numaNodeToCpus.isEmpty()) {
            // Fallback to single node
            numaNodeToCpus.put(0, new ArrayList<>());
            for (int i = 0; i < cpuCount; i++) {
                numaNodeToCpus.get(0).add(i);
                cpuToNumaNode.put(i, 0);
            }
        }
    }
    
    /**
     * Get NUMA node for a specific CPU core
     */
    public int getNumaNode(int cpu) {
        return cpuToNumaNode.getOrDefault(cpu, 0);
    }
    
    /**
     * Get all CPUs in a NUMA node
     */
    public List<Integer> getCpusInNode(int numaNode) {
        return new ArrayList<>(numaNodeToCpus.getOrDefault(numaNode, Collections.emptyList()));
    }
    
    /**
     * Get local NUMA node CPUs for current thread
     */
    public List<Integer> getLocalNumaCpus() {
        // Simplified: return first NUMA node CPUs
        return getCpusInNode(0);
    }
    
    /**
     * Get all NUMA nodes
     */
    public Set<Integer> getNumaNodes() {
        return new HashSet<>(numaNodeToCpus.keySet());
    }
}

