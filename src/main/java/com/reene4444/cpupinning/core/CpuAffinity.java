package com.reene4444.cpupinning.core;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.ptr.LongByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.Set;

/**
 * Core CPU affinity operations using JNA for native system calls.
 * Supports Linux, macOS, and Windows platforms.
 */
public class CpuAffinity {
    private static final Logger logger = LoggerFactory.getLogger(CpuAffinity.class);
    private static final CLibrary INSTANCE = Native.load(Platform.isLinux() ? "c" : 
                                                          Platform.isMac() ? "c" : "kernel32", CLibrary.class);
    
    public interface CLibrary extends Library {
        int sched_setaffinity(int pid, int cpusetsize, long cpuset);
        int sched_getaffinity(int pid, int cpusetsize, LongByReference cpuset);
    }
    
    /**
     * Pin current thread to specific CPU cores
     * @param cores Set of CPU core IDs (0-indexed)
     * @return true if successful
     */
    public static boolean pinThread(Set<Integer> cores) {
        if (cores == null || cores.isEmpty()) {
            logger.warn("No CPU cores specified for pinning");
            return false;
        }
        
        try {
            long mask = createCpuMask(cores);
            int pid = getCurrentThreadId();
            
            if (Platform.isLinux()) {
                int result = INSTANCE.sched_setaffinity(pid, 8, mask);
                if (result == 0) {
                    logger.debug("Successfully pinned thread {} to CPUs: {}", pid, cores);
                    return true;
                } else {
                    logger.error("Failed to set CPU affinity, error code: {}", result);
                    return false;
                }
            } else if (Platform.isMac()) {
                // macOS uses different API, simplified for now
                logger.warn("macOS CPU affinity requires additional native code");
                return false;
            } else {
                logger.warn("Unsupported platform for CPU affinity");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error setting CPU affinity", e);
            return false;
        }
    }
    
    /**
     * Get current thread's CPU affinity
     * @return Set of CPU core IDs this thread is pinned to
     */
    public static Set<Integer> getThreadAffinity() {
        try {
            int pid = getCurrentThreadId();
            
            if (Platform.isLinux()) {
                LongByReference maskRef = new LongByReference();
                int result = INSTANCE.sched_getaffinity(pid, 8, maskRef);
                if (result == 0) {
                    return parseCpuMask(maskRef.getValue());
                }
            }
        } catch (Exception e) {
            logger.error("Error getting CPU affinity", e);
        }
        return Set.of();
    }
    
    /**
     * Get available CPU cores count
     */
    public static int getAvailableCpuCount() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    private static long createCpuMask(Set<Integer> cores) {
        long mask = 0;
        for (Integer core : cores) {
            if (core >= 0 && core < 64) {
                mask |= (1L << core);
            }
        }
        return mask;
    }
    
    private static Set<Integer> parseCpuMask(long mask) {
        BitSet bits = BitSet.valueOf(new long[]{mask});
        return bits.stream().boxed().collect(java.util.stream.Collectors.toSet());
    }
    
    private static int getCurrentThreadId() {
        if (Platform.isLinux()) {
            // Use ProcessHandle for Java 9+ or fallback to system property
            try {
                return (int) ProcessHandle.current().pid();
            } catch (Exception e) {
                // Fallback: use thread ID as approximation
                return (int) Thread.currentThread().getId();
            }
        }
        return 0;
    }
}

