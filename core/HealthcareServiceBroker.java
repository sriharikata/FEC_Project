package org.cloudbus.cloudsim.fec_healthsim.core;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HealthcareServiceBroker extends DatacenterBrokerSimple {
    private final Datacenter fogDC;
    private final Datacenter cloudDC;
    
    // Store cloudlet types since getAttribute might not be available
    private final Map<Long, ServiceType> cloudletTypes = new HashMap<>();
    private final Map<Long, Double> cloudletStartTimes = new HashMap<>();
    private final List<Cloudlet> submittedCloudlets = new ArrayList<>();

    
    public HealthcareServiceBroker(CloudSim simulation, Datacenter fogDC, Datacenter cloudDC) {
        super(simulation);
        this.fogDC = fogDC;
        this.cloudDC = cloudDC;
    }
    
    /**
     * Store the service type for a cloudlet
     */
    public void setCloudletServiceType(Cloudlet cloudlet, ServiceType type) {
        cloudletTypes.put(cloudlet.getId(), type);
        cloudletStartTimes.put(cloudlet.getId(), getSimulation().clock());
    }
    
    /**
     * Get the service type for a cloudlet
     */
    public ServiceType getCloudletServiceType(Cloudlet cloudlet) {
        return cloudletTypes.getOrDefault(cloudlet.getId(), ServiceType.CONSULTATION);
    }
    
    /**
     * Get the start time for a cloudlet
     */
    public double getCloudletStartTime(Cloudlet cloudlet) {
        return cloudletStartTimes.getOrDefault(cloudlet.getId(), 0.0);
    }
    
    @Override
    public DatacenterBroker submitCloudletList(List<? extends Cloudlet> list) {
        // Assign cloudlets to appropriate VMs based on service type before submission
    	submittedCloudlets.addAll(list); 
        for (Cloudlet cloudlet : list) {
            assignCloudletToAppropriateVm(cloudlet);
        }
        return super.submitCloudletList(list);
    }
    
    /**
     * Assigns cloudlet to appropriate VM based on service type
     */
    private void assignCloudletToAppropriateVm(Cloudlet cloudlet) {
        ServiceType type = getCloudletServiceType(cloudlet);
        
        Datacenter targetDC;
        if (type == ServiceType.SURGERY) {
            System.out.printf("🏥 Surgery task %d assigned to FOG (critical priority)%n", cloudlet.getId());
            targetDC = fogDC;
        } else if (type == ServiceType.MONITORING) {
            targetDC = selectDatacenterForMonitoring(cloudlet);
        } else { // CONSULTATION
            System.out.printf("💬 Consultation task %d assigned to CLOUD (non-critical)%n", cloudlet.getId());
            targetDC = cloudDC;
        }
        
        // Find a suitable VM in the target datacenter
        Vm selectedVm = findVmInDatacenter(targetDC);
        if (selectedVm != null) {
            cloudlet.setVm(selectedVm);
        }
    }
    
    /**
     * Finds a suitable VM in the specified datacenter
     */
    private Vm findVmInDatacenter(Datacenter datacenter) {
        return getVmCreatedList().stream()
                .filter(vm -> vm.getHost().getDatacenter().equals(datacenter))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Implements hybrid Fog→Cloud logic for monitoring services
     */
    private Datacenter selectDatacenterForMonitoring(Cloudlet cloudlet) {
        // Simple capacity check
        double fogUtilization = calculateFogUtilization();
        
        if (fogUtilization < 0.8) { // 80% threshold
            System.out.printf("⚡ Monitoring task %d assigned to FOG (utilization: %.2f)%n", 
                            cloudlet.getId(), fogUtilization);
            return fogDC;
        } else {
            System.out.printf("☁️ Monitoring task %d assigned to CLOUD (fog overloaded: %.2f)%n", 
                            cloudlet.getId(), fogUtilization);
            return cloudDC;
        }
    }
    
    /**
     * Simple fog utilization calculation
     */
    private double calculateFogUtilization() {
        long totalCloudlets = getCloudletSubmittedList().size() + getCloudletWaitingList().size();
        long fogVmCount = getVmCreatedList().stream()
                .filter(vm -> vm.getHost().getDatacenter().equals(fogDC))
                .count();
        
        if (fogVmCount == 0) return 1.0; // If no fog VMs, return high utilization
        
        return Math.min(1.0, (double) totalCloudlets / (fogVmCount * 2));
    }
    
    /**
     * Custom method to handle cloudlet completion - called manually
     */
    public void onCloudletFinish(Cloudlet cloudlet) {
        ServiceType type = getCloudletServiceType(cloudlet);
        String dcName = cloudlet.getVm().getHost().getDatacenter().getName();
        double startTime = getCloudletStartTime(cloudlet);
        double responseTime = cloudlet.getFinishTime() - startTime;
        
        System.out.printf("✔ Cloudlet %d (%s) finished at %.2f in %s - Response Time: %.2f s%n",
            cloudlet.getId(),
            type,
            cloudlet.getFinishTime(),
            dcName,
            responseTime
        );
        
        // Log detailed performance metrics
        logPerformanceMetrics(cloudlet, type, dcName, responseTime);
    }
    
    /**
     * Logs detailed performance metrics for analysis
     */
    private void logPerformanceMetrics(Cloudlet cloudlet, ServiceType type, String dcName, double responseTime) {
        double waitingTime = cloudlet.getWaitingTime();
        double executionTime = cloudlet.getActualCpuTime();
        
        System.out.printf("📊 METRICS - ID: %d, Type: %s, DC: %s, Response: %.3f, Wait: %.3f, Exec: %.3f%n",
                         cloudlet.getId(), type, dcName, responseTime, waitingTime, executionTime);
    }
    
    public List<Cloudlet> getCloudletList() {
        return submittedCloudlets;
    }

}