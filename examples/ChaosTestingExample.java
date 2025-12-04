package examples;

import com.reene4444.cpupinning.chaos.ChaosIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Example: Chaos Engineering Integration
 * Test CPU pinning resilience under stress
 */
@Service
public class ChaosTestingExample {
    
    @Autowired
    private ChaosIntegration chaosIntegration;
    
    public void testPinningResilience() {
        // Enable CPU chaos on specific cores
        chaosIntegration.enableCpuChaos(Set.of(0, 1), 30); // 30 seconds
        
        // Your application continues running
        // Framework validates that threads stay pinned despite CPU contention
        
        // Validate resilience
        Thread testThread = Thread.currentThread();
        boolean resilient = chaosIntegration.validatePinningResilience(
            testThread, Set.of(0, 1), 10);
        
        System.out.println("Pinning resilience: " + resilient);
    }
}

