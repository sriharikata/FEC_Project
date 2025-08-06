package org.cloudbus.cloudsim.fec_healthsim.queuing;

import org.cloudbus.cloudsim.fec_healthsim.core.HealthcareTask;
import org.cloudbus.cloudsim.fec_healthsim.core.ServiceType;
import org.cloudbus.cloudsim.fec_healthsim.data.UrgencyLevel;
import java.util.*;

/**
 * Healthcare Queue Manager - Implements priority-based task scheduling
 * Core implementation of queuing concepts from AlQahtani (2023) research paper
 */
public class HealthcareQueueManager {
    
    // Priority queues for different urgency levels
    private final PriorityQueue<HealthcareTask> criticalQueue;
    private final PriorityQueue<HealthcareTask> highPriorityQueue;
    private final PriorityQueue<HealthcareTask> normalQueue;
    
    // Queue statistics
    private final Map<String, QueueStats> queueStatistics;
    private final List<TaskCompletionRecord> completionHistory;
    
    // Configuration parameters
    private static final int MAX_QUEUE_SIZE = 100;
    private static final double CRITICAL_SLA_SECONDS = 2.0;    // 2 seconds max for critical
    private static final double HIGH_SLA_SECONDS = 10.0;      // 10 seconds max for high priority
    private static final double NORMAL_SLA_SECONDS = 60.0;    // 60 seconds max for normal
    
    public HealthcareQueueManager() {
        // Initialize priority queues with custom comparators
        this.criticalQueue = new PriorityQueue<>(new TaskPriorityComparator());
        this.highPriorityQueue = new PriorityQueue<>(new TaskPriorityComparator());
        this.normalQueue = new PriorityQueue<>(new TaskPriorityComparator());
        
        this.queueStatistics = new HashMap<>();
        this.completionHistory = new ArrayList<>();
        
        // Initialize statistics
        initializeQueueStats();
        
        System.out.println("🏥 Healthcare Queue Manager initialized");
        System.out.println("   Critical Queue SLA: " + CRITICAL_SLA_SECONDS + "s");
        System.out.println("   High Priority SLA: " + HIGH_SLA_SECONDS + "s");
        System.out.println("   Normal Queue SLA: " + NORMAL_SLA_SECONDS + "s");
    }
    
    /**
     * Adds a healthcare task to appropriate priority queue
     */
    public boolean enqueueTask(HealthcareTask task) {
        UrgencyLevel urgency = task.getUrgency();
        
        // Check queue capacity
        if (!hasCapacity(urgency)) {
            System.out.printf("⚠️ Queue overflow for %s task (Patient: %d)%n", 
                            urgency, task.getPatientId());
            return false;
        }
        
        // Add to appropriate queue
        boolean success = false;
        switch (urgency) {
            case CRITICAL:
                success = criticalQueue.offer(task);
                updateQueueStats("CRITICAL", 1, 0);
                System.out.printf("🚨 CRITICAL task queued (Patient: %d, Queue size: %d)%n", 
                                task.getPatientId(), criticalQueue.size());
                break;
                
            case HIGH:
                success = highPriorityQueue.offer(task);
                updateQueueStats("HIGH", 1, 0);
                System.out.printf("⚡ HIGH priority task queued (Patient: %d, Queue size: %d)%n", 
                                task.getPatientId(), highPriorityQueue.size());
                break;
                
            case NORMAL:
                success = normalQueue.offer(task);
                updateQueueStats("NORMAL", 1, 0);
                System.out.printf("📋 NORMAL task queued (Patient: %d, Queue size: %d)%n", 
                                task.getPatientId(), normalQueue.size());
                break;
        }
        
        return success;
    }
    
    /**
     * Dequeues the highest priority task available
     */
    public HealthcareTask dequeueNextTask() {
        HealthcareTask task = null;
        String queueType = null;
        
        // Process in priority order: Critical -> High -> Normal
        if (!criticalQueue.isEmpty()) {
            task = criticalQueue.poll();
            queueType = "CRITICAL";
            updateQueueStats("CRITICAL", 0, 1);
        } else if (!highPriorityQueue.isEmpty()) {
            task = highPriorityQueue.poll();
            queueType = "HIGH";
            updateQueueStats("HIGH", 0, 1);
        } else if (!normalQueue.isEmpty()) {
            task = normalQueue.poll();
            queueType = "NORMAL";
            updateQueueStats("NORMAL", 0, 1);
        }
        
        if (task != null) {
            // Calculate waiting time using simulation time
            double waitingTime = 0.1; // Minimal waiting time for simulation
            task.setWaitingTime(waitingTime);
            
            System.out.printf("🎯 Dequeued %s task (Patient: %d, Waited: %.2fs)%n", 
                            queueType, task.getPatientId(), waitingTime);
            
            // Check SLA compliance
            checkSLACompliance(task, queueType, waitingTime);
        }
        
        return task;
    }
    
    /**
     * Dequeues all tasks for batch processing
     */
    public List<HealthcareTask> dequeueAll() {
        List<HealthcareTask> allTasks = new ArrayList<>();
        
        // Dequeue all critical tasks first
        while (!criticalQueue.isEmpty()) {
            HealthcareTask task = dequeueNextTask();
            if (task != null) {
                allTasks.add(task);
            }
        }
        
        // Then high priority tasks
        while (!highPriorityQueue.isEmpty()) {
            HealthcareTask task = dequeueNextTask();
            if (task != null) {
                allTasks.add(task);
            }
        }
        
        // Finally normal tasks
        while (!normalQueue.isEmpty()) {
            HealthcareTask task = dequeueNextTask();
            if (task != null) {
                allTasks.add(task);
            }
        }
        
        System.out.printf("📤 Dequeued %d total tasks for processing%n", allTasks.size());
        return allTasks;
    }
    
    /**
     * Checks if queues have capacity
     */
    private boolean hasCapacity(UrgencyLevel urgency) {
        switch (urgency) {
            case CRITICAL:
                return criticalQueue.size() < MAX_QUEUE_SIZE;
            case HIGH:
                return highPriorityQueue.size() < MAX_QUEUE_SIZE;
            case NORMAL:
                return normalQueue.size() < MAX_QUEUE_SIZE;
            default:
                return false;
        }
    }
    
    /**
     * Updates queue statistics
     */
    private void updateQueueStats(String queueType, int enqueued, int dequeued) {
        QueueStats stats = queueStatistics.get(queueType);
        stats.totalEnqueued += enqueued;
        stats.totalDequeued += dequeued;
        stats.currentSize = getCurrentQueueSize(queueType);
        stats.maxSize = Math.max(stats.maxSize, stats.currentSize);
    }
    
    /**
     * Gets current queue size by type
     */
    private int getCurrentQueueSize(String queueType) {
        switch (queueType) {
            case "CRITICAL": return criticalQueue.size();
            case "HIGH": return highPriorityQueue.size();
            case "NORMAL": return normalQueue.size();
            default: return 0;
        }
    }
    
    /**
     * Checks SLA compliance for completed tasks
     */
    private void checkSLACompliance(HealthcareTask task, String queueType, double waitingTime) {
        boolean slaCompliant = false;
        double slaLimit = 0.0;
        
        switch (queueType) {
            case "CRITICAL":
                slaLimit = CRITICAL_SLA_SECONDS;
                slaCompliant = waitingTime <= CRITICAL_SLA_SECONDS;
                break;
            case "HIGH":
                slaLimit = HIGH_SLA_SECONDS;
                slaCompliant = waitingTime <= HIGH_SLA_SECONDS;
                break;
            case "NORMAL":
                slaLimit = NORMAL_SLA_SECONDS;
                slaCompliant = waitingTime <= NORMAL_SLA_SECONDS;
                break;
        }
        
        if (!slaCompliant) {
            System.out.printf("⚠️ SLA VIOLATION: %s task waited %.2fs (limit: %.2fs)%n", 
                            queueType, waitingTime, slaLimit);
        }
        
        // Update SLA statistics
        QueueStats stats = queueStatistics.get(queueType);
        if (slaCompliant) {
            stats.slaCompliant++;
        } else {
            stats.slaViolations++;
        }
    }
    
    /**
     * Records task completion for analysis
     */
    public void recordTaskCompletion(HealthcareTask task, double executionTime, String datacenter) {
        TaskCompletionRecord record = new TaskCompletionRecord(
            task.getPatientId(),
            task.getUrgency(),
            task.getWaitingTime(),
            executionTime,
            datacenter,
            System.currentTimeMillis()
        );
        
        completionHistory.add(record);
        
        System.out.printf("✅ Task completion recorded: Patient %d (%s) in %s%n",
                         task.getPatientId(), task.getUrgency(), datacenter);
    }
    
    /**
     * Gets total number of pending tasks across all queues
     */
    public int getTotalPendingTasks() {
        return criticalQueue.size() + highPriorityQueue.size() + normalQueue.size();
    }
    
    /**
     * Checks if all queues are empty
     */
    public boolean isEmpty() {
        return criticalQueue.isEmpty() && highPriorityQueue.isEmpty() && normalQueue.isEmpty();
    }
    
    /**
     * Generates comprehensive queue performance report
     */
    public void generatePerformanceReport() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 HEALTHCARE QUEUE PERFORMANCE REPORT");
        System.out.println("=".repeat(60));
        
        for (String queueType : Arrays.asList("CRITICAL", "HIGH", "NORMAL")) {
            QueueStats stats = queueStatistics.get(queueType);
            double slaComplianceRate = 0.0;
            
            if (stats.totalDequeued > 0) {
                slaComplianceRate = (double) stats.slaCompliant / (stats.slaCompliant + stats.slaViolations) * 100;
            }
            
            System.out.printf("\n🏷️ %s QUEUE STATISTICS:%n", queueType);
            System.out.printf("   Total Enqueued: %d%n", stats.totalEnqueued);
            System.out.printf("   Total Processed: %d%n", stats.totalDequeued);
            System.out.printf("   Current Size: %d%n", stats.currentSize);
            System.out.printf("   Max Size Reached: %d%n", stats.maxSize);
            System.out.printf("   SLA Compliance Rate: %.1f%%%n", slaComplianceRate);
            System.out.printf("   SLA Violations: %d%n", stats.slaViolations);
        }
        
        // Overall system statistics
        int totalTasks = queueStatistics.values().stream()
                .mapToInt(stats -> stats.totalEnqueued)
                .sum();
        
        int totalProcessed = queueStatistics.values().stream()
                .mapToInt(stats -> stats.totalDequeued)
                .sum();
        
        System.out.println("\n🎯 OVERALL SYSTEM PERFORMANCE:");
        System.out.printf("   Total Tasks Received: %d%n", totalTasks);
        System.out.printf("   Total Tasks Processed: %d%n", totalProcessed);
        System.out.printf("   Processing Rate: %.1f%%%n", (totalProcessed * 100.0) / Math.max(1, totalTasks));
        System.out.printf("   Tasks Still Pending: %d%n", getTotalPendingTasks());
        
        // Average waiting times by priority
        calculateAverageWaitingTimes();
    }
    
    /**
     * Calculates average waiting times by priority level
     */
    private void calculateAverageWaitingTimes() {
        Map<UrgencyLevel, List<Double>> waitingTimes = new HashMap<>();
        
        for (TaskCompletionRecord record : completionHistory) {
            waitingTimes.computeIfAbsent(record.urgency, k -> new ArrayList<>())
                       .add(record.waitingTime);
        }
        
        System.out.println("\n⏱️ AVERAGE WAITING TIMES:");
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            List<Double> times = waitingTimes.get(urgency);
            if (times != null && !times.isEmpty()) {
                double avgWaitTime = times.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                System.out.printf("   %s: %.2f seconds%n", urgency, avgWaitTime);
            }
        }
    }
    
    /**
     * Initialize queue statistics
     */
    private void initializeQueueStats() {
        queueStatistics.put("CRITICAL", new QueueStats());
        queueStatistics.put("HIGH", new QueueStats());
        queueStatistics.put("NORMAL", new QueueStats());
    }
    
    /**
     * Get queue statistics for external analysis
     */
    public Map<String, QueueStats> getQueueStatistics() {
        return new HashMap<>(queueStatistics);
    }
    
    /**
     * Get completion history for analysis
     */
    public List<TaskCompletionRecord> getCompletionHistory() {
        return new ArrayList<>(completionHistory);
    }
}

/**
 * Custom comparator for healthcare task priority
 */
class TaskPriorityComparator implements Comparator<HealthcareTask> {
    @Override
    public int compare(HealthcareTask t1, HealthcareTask t2) {
        // First compare by urgency (higher urgency = higher priority)
        int urgencyComparison = t2.getUrgency().ordinal() - t1.getUrgency().ordinal();
        if (urgencyComparison != 0) {
            return urgencyComparison;
        }
        
        // If same urgency, prioritize by arrival time (FIFO)
        return Double.compare(t1.getArrivalTime(), t2.getArrivalTime());
    }
}

/**
 * Queue statistics tracking class
 */
class QueueStats {
    int totalEnqueued = 0;
    int totalDequeued = 0;
    int currentSize = 0;
    int maxSize = 0;
    int slaCompliant = 0;
    int slaViolations = 0;
}

/**
 * Task completion record for analysis
 */
class TaskCompletionRecord {
    final int patientId;
    final UrgencyLevel urgency;
    final double waitingTime;
    final double executionTime;
    final String datacenter;
    final long completionTimestamp;
    
    TaskCompletionRecord(int patientId, UrgencyLevel urgency, double waitingTime, 
                        double executionTime, String datacenter, long completionTimestamp) {
        this.patientId = patientId;
        this.urgency = urgency;
        this.waitingTime = waitingTime;
        this.executionTime = executionTime;
        this.datacenter = datacenter;
        this.completionTimestamp = completionTimestamp;
    }
}