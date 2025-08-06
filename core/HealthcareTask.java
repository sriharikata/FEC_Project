package org.cloudbus.cloudsim.fec_healthsim.core;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.fec_healthsim.data.*;

/**
 * Healthcare Task - Represents a medical processing task with IoT data
 * Links CloudSim cloudlets with real healthcare data packets
 */
public class HealthcareTask {
    private final int taskId;
    private final int patientId;
    private final HealthcareDataPacket dataPacket;
    private final UrgencyLevel urgency;
    private final ServiceType serviceType;
    private final double arrivalTime; // Use simulation time instead of system time
    private final Cloudlet cloudlet;
    
    private double waitingTime = 0.0;
    private String assignedDatacenter = null;
    
    public HealthcareTask(int taskId, HealthcareDataPacket dataPacket, Cloudlet cloudlet, double simTime) {
        this.taskId = taskId;
        this.patientId = dataPacket.getPatientId();
        this.dataPacket = dataPacket;
        this.urgency = dataPacket.getUrgency();
        this.serviceType = mapUrgencyToServiceType(urgency);
        this.arrivalTime = simTime; // Use CloudSim simulation time
        this.cloudlet = cloudlet;
    }
    
    /**
     * Maps urgency level to service type for compatibility
     */
    private ServiceType mapUrgencyToServiceType(UrgencyLevel urgency) {
        switch (urgency) {
            case CRITICAL:
                return ServiceType.SURGERY;  // Critical tasks = Surgery priority
            case HIGH:
                return ServiceType.MONITORING;  // High priority = Monitoring
            case NORMAL:
            default:
                return ServiceType.CONSULTATION;  // Normal = Consultation
        }
    }
    
    // Getters
    public int getTaskId() { return taskId; }
    public int getPatientId() { return patientId; }
    public HealthcareDataPacket getDataPacket() { return dataPacket; }
    public UrgencyLevel getUrgency() { return urgency; }
    public ServiceType getServiceType() { return serviceType; }
    public double getArrivalTime() { return arrivalTime; }
    public Cloudlet getCloudlet() { return cloudlet; }
    public double getWaitingTime() { return waitingTime; }
    public String getAssignedDatacenter() { return assignedDatacenter; }
    
    // Setters
    public void setWaitingTime(double waitingTime) { this.waitingTime = waitingTime; }
    public void setAssignedDatacenter(String datacenter) { 
        this.assignedDatacenter = datacenter;
        System.out.printf("   🎯 Task %d assigned to datacenter: %s%n", taskId, datacenter);
    }
    
    /**
     * Gets computational complexity based on urgency and vital signs
     */
    public long getComputationalComplexity() {
        VitalSigns vitals = dataPacket.getVitals();
        long baseComplexity = 1000; // Base MI (Million Instructions)
        
        switch (urgency) {
            case CRITICAL:
                // Critical tasks require complex analysis and immediate processing
                baseComplexity = 5000;
                // Additional complexity for abnormal vitals
                if (vitals.getHeartRate() > 150 || vitals.getSpO2() < 85) {
                    baseComplexity += 2000;
                }
                break;
                
            case HIGH:
                // High priority monitoring with moderate complexity
                baseComplexity = 8000;
                // Pattern analysis and trend detection
                if (vitals.getSystolicBP() > 160 || vitals.getTemperature() > 38.5) {
                    baseComplexity += 1000;
                }
                break;
                
            case NORMAL:
                // Standard consultation processing
                baseComplexity = 25000;
                // Comprehensive analysis for non-urgent cases
                break;
        }
        
        return baseComplexity;
    }
    
    /**
     * Gets data size based on task type and complexity
     */
    public int getDataSize() {
        switch (urgency) {
            case CRITICAL:
                return 100; // Small, focused critical data
            case HIGH:
                return 250; // Moderate monitoring data
            case NORMAL:
            default:
                return 1000; // Large consultation datasets
        }
    }
    
    /**
     * Determines if task requires fog processing based on urgency and latency requirements
     */
    public boolean requiresFogProcessing() {
        return urgency == UrgencyLevel.CRITICAL || 
               (urgency == UrgencyLevel.HIGH && hasAbnormalVitals());
    }
    
    /**
     * Checks if vital signs are abnormal requiring immediate attention
     */
    private boolean hasAbnormalVitals() {
        VitalSigns vitals = dataPacket.getVitals();
        return vitals.getHeartRate() > 120 || vitals.getHeartRate() < 60 ||
               vitals.getSystolicBP() > 160 || vitals.getSystolicBP() < 100 ||
               vitals.getTemperature() > 38.5 || vitals.getSpO2() < 92;
    }
    
    /**
     * Gets expected SLA response time in seconds
     */
    public double getExpectedSLA() {
        switch (urgency) {
            case CRITICAL:
                return 2.0;  // 2 seconds max for critical
            case HIGH:
                return 10.0; // 10 seconds max for high priority
            case NORMAL:
            default:
                return 60.0; // 60 seconds max for normal
        }
    }
    
    /**
     * Gets task priority score for advanced scheduling
     */
    public double getPriorityScore() {
        double baseScore = 0.0;
        
        switch (urgency) {
            case CRITICAL:
                baseScore = 100.0;
                break;
            case HIGH:
                baseScore = 50.0;
                break;
            case NORMAL:
                baseScore = 10.0;
                break;
        }
        
        // Adjust score based on waiting time (higher waiting time = higher priority)
        double waitingBonus = Math.min(waitingTime * 2, 20.0);
        
        // Adjust score based on patient condition severity
        VitalSigns vitals = dataPacket.getVitals();
        double severityBonus = calculateSeverityBonus(vitals);
        
        return baseScore + waitingBonus + severityBonus;
    }
    
    /**
     * Calculates severity bonus based on vital signs deviation from normal
     */
    private double calculateSeverityBonus(VitalSigns vitals) {
        double bonus = 0.0;
        
        // Heart rate deviation bonus
        if (vitals.getHeartRate() > 150 || vitals.getHeartRate() < 50) {
            bonus += 10.0;
        } else if (vitals.getHeartRate() > 120 || vitals.getHeartRate() < 60) {
            bonus += 5.0;
        }
        
        // Blood pressure deviation bonus
        if (vitals.getSystolicBP() > 180 || vitals.getSystolicBP() < 90) {
            bonus += 10.0;
        } else if (vitals.getSystolicBP() > 160 || vitals.getSystolicBP() < 100) {
            bonus += 5.0;
        }
        
        // Temperature deviation bonus
        if (vitals.getTemperature() > 40.0) {
            bonus += 8.0;
        } else if (vitals.getTemperature() > 38.5) {
            bonus += 4.0;
        }
        
        // SpO2 deviation bonus
        if (vitals.getSpO2() < 85) {
            bonus += 12.0;
        } else if (vitals.getSpO2() < 92) {
            bonus += 6.0;
        }
        
        return bonus;
    }
    
    @Override
    public String toString() {
        return String.format("Task[ID:%d, Patient:%d, %s, %s, Priority:%.1f]", 
                           taskId, patientId, urgency, serviceType, getPriorityScore());
    }
    
    /**
     * Gets detailed task information for logging
     */
    public String getDetailedInfo() {
        return String.format("Healthcare Task Details:\n" +
                           "  Task ID: %d\n" +
                           "  Patient ID: %d\n" +
                           "  Urgency: %s\n" +
                           "  Service Type: %s\n" +
                           "  Vital Signs: %s\n" +
                           "  Condition: %s\n" +
                           "  Computational Complexity: %d MI\n" +
                           "  Data Size: %d KB\n" +
                           "  Expected SLA: %.1f seconds\n" +
                           "  Priority Score: %.2f\n" +
                           "  Requires Fog: %s\n" +
                           "  Waiting Time: %.2f seconds\n" +
                           "  Assigned Datacenter: %s",
                           taskId, patientId, urgency, serviceType,
                           dataPacket.getVitals(), dataPacket.getCondition(),
                           getComputationalComplexity(), getDataSize(),
                           getExpectedSLA(), getPriorityScore(),
                           requiresFogProcessing() ? "Yes" : "No", waitingTime,
                           assignedDatacenter != null ? assignedDatacenter : "Not assigned");
    }
}