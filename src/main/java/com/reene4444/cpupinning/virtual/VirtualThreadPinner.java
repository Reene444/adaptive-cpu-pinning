package com.reene4444.cpupinning.virtual;

import com.reene4444.cpupinning.core.CpuAffinity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Virtual Thread CPU Pinner - provides CPU pinning support for Java 21+ Virtual Threads
 * Solves CPU cache invalidation issues in high-concurrency IO tasks
 */
public class VirtualThreadPinner {
    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadPinner.class);
    
    private final Map<Integer, ExecutorService> pinnedExecutors = new ConcurrentHashMap<>();
    private final AtomicInteger executorCounter = new AtomicInteger(0);
    private final Set<Integer> availableCpus;
    
    public VirtualThreadPinner() {
        availableCpus = new HashSet<>();
        int cpuCount = CpuAffinity.getAvailableCpuCount();
        for (int i = 0; i < cpuCount; i++) {
            availableCpus.add(i);
        }
    }
    
    /**
     * Create a VirtualThreadExecutor pinned to specific CPU cores
     * @param cores CPU cores to pin to
     * @return ExecutorService with pinned virtual threads
     */
    public ExecutorService createPinnedExecutor(Set<Integer> cores) {
        if (cores == null || cores.isEmpty()) {
            throw new IllegalArgumentException("CPU cores must be specified");
        }
        
        // Validate cores
        for (Integer core : cores) {
            if (!availableCpus.contains(core)) {
                throw new IllegalArgumentException("Invalid CPU core: " + core);
            }
        }
        
        // Create a custom executor that pins carrier threads
        // For Java 19+, use Thread.ofVirtual().factory()
        // For Java 17, fallback to regular executor
        ExecutorService executor;
        try {
            // Try Java 19+ virtual threads API
            executor = (ExecutorService) Executors.class
                .getMethod("newVirtualThreadPerTaskExecutor")
                .invoke(null);
        } catch (Exception e) {
            // Fallback for Java 17: use regular executor
            // Note: Virtual threads require Java 19+
            logger.warn("Virtual threads not available, using regular executor. Java 19+ required.");
            executor = Executors.newCachedThreadPool();
        }
        
        // Wrap with pinning logic
        ExecutorService pinnedExecutor = new PinnedVirtualThreadExecutor(executor, cores);
        pinnedExecutors.put(executorCounter.incrementAndGet(), pinnedExecutor);
        
        logger.info("Created pinned virtual thread executor for CPUs: {}", cores);
        return pinnedExecutor;
    }
    
    /**
     * Create executor pinned to a single CPU core
     */
    public ExecutorService createPinnedExecutor(int core) {
        return createPinnedExecutor(Set.of(core));
    }
    
    /**
     * Pinned Virtual Thread Executor wrapper
     */
    private static class PinnedVirtualThreadExecutor implements ExecutorService {
        private final ExecutorService delegate;
        private final Set<Integer> cores;
        private final ThreadLocal<Boolean> pinned = ThreadLocal.withInitial(() -> false);
        
        public PinnedVirtualThreadExecutor(ExecutorService delegate, Set<Integer> cores) {
            this.delegate = delegate;
            this.cores = cores;
        }
        
        @Override
        public void execute(Runnable command) {
            delegate.execute(() -> {
                // Pin the carrier thread (platform thread) that runs the virtual thread
                if (!pinned.get()) {
                    CpuAffinity.pinThread(cores);
                    pinned.set(true);
                }
                command.run();
            });
        }
        
        @Override
        public void shutdown() {
            delegate.shutdown();
        }
        
        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }
        
        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }
        
        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }
        
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }
        
        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(() -> {
                if (!pinned.get()) {
                    CpuAffinity.pinThread(cores);
                    pinned.set(true);
                }
                return task.call();
            });
        }
        
        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(() -> {
                if (!pinned.get()) {
                    CpuAffinity.pinThread(cores);
                    pinned.set(true);
                }
                task.run();
            }, result);
        }
        
        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(() -> {
                if (!pinned.get()) {
                    CpuAffinity.pinThread(cores);
                    pinned.set(true);
                }
                task.run();
            });
        }
        
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }
        
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) 
                throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }
        
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }
        
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) 
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }
    }
    
    /**
     * Shutdown all pinned executors
     */
    public void shutdown() {
        for (ExecutorService executor : pinnedExecutors.values()) {
            executor.shutdown();
        }
        pinnedExecutors.clear();
    }
}

