package examples;

import com.reene4444.cpupinning.spring.annotation.AffinityPool;
import org.springframework.stereotype.Service;

/**
 * Example: Business Isolation with CPU Pool
 * Notification service isolated to CPUs 4-7
 */
@AffinityPool(value = "notification-service", cpus = {4, 5, 6, 7})
@Service
public class NotificationServiceExample {
    
    public void sendNotification(String userId, String message) {
        // This method will automatically run on CPUs 4-7
        // Completely isolated from payment service
        System.out.println("Sending notification to " + userId + ": " + message);
    }
}

