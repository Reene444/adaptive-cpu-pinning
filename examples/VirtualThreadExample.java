package examples;

import com.reene4444.cpupinning.spring.annotation.PinnedAffinity;
import com.reene4444.cpupinning.virtual.VirtualThreadPinner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Example: Virtual Thread CPU Pinning
 * Java 21+ virtual threads with CPU affinity support
 */
@Service
public class VirtualThreadExample {
    
    @Autowired
    private VirtualThreadPinner virtualThreadPinner;
    
    @PinnedAffinity(cores = {0, 2, 4})
    public ExecutorService createPinnedExecutor() {
        // Returns a VirtualThreadExecutor pinned to CPUs 0, 2, 4
        return virtualThreadPinner.createPinnedExecutor(Set.of(0, 2, 4));
    }
    
    public void processWithPinnedVirtualThreads() {
        ExecutorService executor = createPinnedExecutor();
        
        // Submit tasks - virtual threads will run on pinned carrier threads
        for (int i = 0; i < 1000; i++) {
            final int taskId = i;
            executor.submit(() -> {
                // This virtual thread runs on a carrier thread pinned to CPUs 0, 2, 4
                System.out.println("Task " + taskId + " running on pinned CPU");
            });
        }
    }
}

