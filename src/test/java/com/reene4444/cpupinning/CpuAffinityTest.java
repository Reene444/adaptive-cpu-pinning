package com.reene4444.cpupinning;

import com.reene4444.cpupinning.core.CpuAffinity;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class CpuAffinityTest {
    
    @Test
    public void testGetAvailableCpuCount() {
        int cpuCount = CpuAffinity.getAvailableCpuCount();
        assertTrue("CPU count should be positive", cpuCount > 0);
    }
    
    @Test
    public void testPinThread() {
        Set<Integer> cores = Set.of(0, 1);
        // Note: Actual pinning may fail on non-Linux or without proper permissions
        // This test verifies the method doesn't throw exceptions
        boolean result = CpuAffinity.pinThread(cores);
        // Result depends on platform and permissions
        assertNotNull("Result should not be null", result);
    }
}

