package com.reene4444.cpupinning.business;

import com.reene4444.cpupinning.core.CpuAffinity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Business Isolation Manager - manages CPU pools for different business services
 * to prevent interference between microservices
 */
public class BusinessIsolationManager {
    private static final Logger logger = LoggerFactory.getLogger(BusinessIsolationManager.class);
    
    private static BusinessIsolationManager instance;
    private final Map<String, CpuPool> businessPools = new ConcurrentHashMap<>();
    private final Map<Thread, String> threadToBusiness = new ConcurrentHashMap<>();
    private final ReentrantLock allocationLock = new ReentrantLock();
    private final Set<Integer> availableCpus;
    
    private BusinessIsolationManager() {
        availableCpus = new HashSet<>();
        int cpuCount = CpuAffinity.getAvailableCpuCount();
        for (int i = 0; i < cpuCount; i++) {
            availableCpus.add(i);
        }
    }
    
    public static synchronized BusinessIsolationManager getInstance() {
        if (instance == null) {
            instance = new BusinessIsolationManager();
        }
        return instance;
    }
    
    /**
     * Register a business service with CPU pool
     */
    public void registerBusinessPool(String businessName, Set<Integer> cpus) {
        allocationLock.lock();
        try {
            // Validate CPU allocation
            Set<Integer> allocated = new HashSet<>();
            for (CpuPool pool : businessPools.values()) {
                allocated.addAll(pool.getCpus());
            }
            
            Set<Integer> intersection = new HashSet<>(cpus);
            intersection.retainAll(allocated);
            
            if (!intersection.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("CPUs %s are already allocated to other businesses", intersection));
            }
            
            CpuPool pool = new CpuPool(businessName, cpus);
            businessPools.put(businessName, pool);
            logger.info("Registered business pool '{}' with CPUs: {}", businessName, cpus);
        } finally {
            allocationLock.unlock();
        }
    }
    
    /**
     * Pin thread to business service CPU pool
     */
    public boolean pinToBusiness(Thread thread, String businessName) {
        CpuPool pool = businessPools.get(businessName);
        if (pool == null) {
            logger.warn("Business pool '{}' not found", businessName);
            return false;
        }
        
        Set<Integer> cpus = pool.getCpus();
        boolean success = CpuAffinity.pinThread(cpus);
        if (success) {
            threadToBusiness.put(thread, businessName);
            pool.addThread(thread);
            logger.info("Pinned thread {} to business '{}' CPUs: {}", 
                       thread.getName(), businessName, cpus);
        }
        return success;
    }
    
    /**
     * Get CPU pool for a business
     */
    public CpuPool getBusinessPool(String businessName) {
        return businessPools.get(businessName);
    }
    
    /**
     * Migrate thread to different business pool (for scaling)
     */
    public boolean migrateThread(Thread thread, String fromBusiness, String toBusiness) {
        CpuPool fromPool = businessPools.get(fromBusiness);
        CpuPool toPool = businessPools.get(toBusiness);
        
        if (fromPool == null || toPool == null) {
            return false;
        }
        
        boolean success = pinToBusiness(thread, toBusiness);
        if (success) {
            fromPool.removeThread(thread);
        }
        return success;
    }
    
    /**
     * Get all registered business pools
     */
    public Set<String> getBusinessNames() {
        return new HashSet<>(businessPools.keySet());
    }
    
    /**
     * CPU Pool for a business service
     */
    public static class CpuPool {
        private final String businessName;
        private final Set<Integer> cpus;
        private final Set<Thread> threads = ConcurrentHashMap.newKeySet();
        
        public CpuPool(String businessName, Set<Integer> cpus) {
            this.businessName = businessName;
            this.cpus = new HashSet<>(cpus);
        }
        
        public String getBusinessName() {
            return businessName;
        }
        
        public Set<Integer> getCpus() {
            return new HashSet<>(cpus);
        }
        
        public Set<Thread> getThreads() {
            return new HashSet<>(threads);
        }
        
        void addThread(Thread thread) {
            threads.add(thread);
        }
        
        void removeThread(Thread thread) {
            threads.remove(thread);
        }
        
        public int getThreadCount() {
            return threads.size();
        }
    }
}

