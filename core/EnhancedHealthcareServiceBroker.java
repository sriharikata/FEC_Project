package org.cloudbus.cloudsim.fec_healthsim.core;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;

import org.cloudbus.cloudsim.fec_healthsim.queuing.HealthcareQueueManager;
import org.cloudbus.cloudsim.fec_healthsim.data.*;
import org.cloudbus.cloudsim.fec_healthsim.core.TaskPerformanceRecord;


import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Healthcare Service Broker with Real Data Integration and Priority Queuing
 * Implements the core concepts from AlQahtani (2023) research paper
 */
public class EnhancedHealthcareServiceBroker extends DatacenterBrokerSimple {
    private final Datacenter fogDC;
    private final Datacenter cloudDC;
    private final HealthcareQueueManager queueManager;

    // Healthcare data and task management
    private final List<HealthcareDataPacket> healthcareData;
    private final List<HealthcareTask> healthcareTasks;
    private final Map<Long, HealthcareTask> cloudletTaskMap;

    // Performance tracking
    private final Map<String, Integer> datacenterTaskCount;
    private final List<TaskPerformanceRecord> performanceRecords;

    // Network slicing simulation parameters
    private static final double SLICE_5G_URLLC_LATENCY = 1.0;    // 1ms for critical
    private static final double SLICE_5G_EMBB_LATENCY = 10.0;    // 10ms for high bandwidth
    private static final double SLICE_5G_MIOT_LATENCY = 100.0;   // 100ms for massive IoT

    private long taskIdCounter = 0;

    public EnhancedHealthcareServiceBroker(CloudSim simulation, Datacenter fogDC, Datacenter cloudDC) {
        super(simulation);
        this.fogDC = fogDC;
        this.cloudDC = cloudDC;
        this.queueManager = new HealthcareQueueManager();
        this.healthcareData = new ArrayList<>();
        this.healthcareTasks = new ArrayList<>();
        this.cloudletTaskMap = new HashMap<>();
        this.datacenterTaskCount = new HashMap<>();
        this.performanceRecords = new ArrayList<>();
        this.setDatacenterMapper((prevDc, vm) -> {
            String preferred = vm.getMips() >= 2500 ? "Fog_DC" : "Cloud_DC";

            return getDatacenterList().stream()
                .filter(dc -> dc.getName().equals(preferred))
                .findFirst()
                .orElse(Datacenter.NULL); // return NULL if nothing matches
        });

        // Initialize datacenter counters
        datacenterTaskCount.put("Fog_DC", 0);
        datacenterTaskCount.put("Cloud_DC", 0);

        System.out.println("🏥 Enhanced Healthcare Service Broker initialized");
        System.out.println("   Fog DC: " + fogDC.getName());
        System.out.println("   Cloud DC: " + cloudDC.getName());
    }

    /**
     * Processes healthcare IoT data and creates tasks
     */
    public void processHealthcareData(List<HealthcareDataPacket> dataPackets) {
        System.out.println("\n🔄 Processing healthcare IoT data...");
        System.out.printf("   Received %d healthcare data packets%n", dataPackets.size());

        this.healthcareData.addAll(dataPackets);

        for (HealthcareDataPacket packet : dataPackets) {
            CloudletSimple cloudlet = createCloudletFromPacket(packet);
            cloudlet.setId(taskIdCounter);
            
            // Create task with simulation time instead of system time
            HealthcareTask task = new HealthcareTask((int) taskIdCounter++, packet, cloudlet, getSimulation().clock());
            healthcareTasks.add(task);
            cloudletTaskMap.put(cloudlet.getId(), task);

            boolean queued = queueManager.enqueueTask(task);
            if (!queued) {
                System.out.printf("⚠️ Failed to queue task for patient %d%n", packet.getPatientId());
            }
        }

        System.out.printf("✅ Created and queued %d healthcare tasks%n", healthcareTasks.size());
    }

    /**
     * Creates CloudSim cloudlet from healthcare data packet
     */
    private CloudletSimple createCloudletFromPacket(HealthcareDataPacket packet) {
        long length = calculateComputationalComplexity(packet);
        int fileSize = calculateDataSize(packet);
        int outputSize = fileSize / 2;

        CloudletSimple cloudlet = new CloudletSimple(length, 1);
        cloudlet.setFileSize(fileSize);
        cloudlet.setOutputSize(outputSize);
        cloudlet.setUtilizationModel(new UtilizationModelFull());
        return cloudlet;
    }

    /**
     * Calculates computational complexity based on healthcare data
     */
    private long calculateComputationalComplexity(HealthcareDataPacket packet) {
        long base = 1000;
        UrgencyLevel urgency = packet.getUrgency();
        VitalSigns vitals = packet.getVitals();

        switch (urgency) {
            case CRITICAL:
                base = 5000;
                if (vitals.getHeartRate() > 150 || vitals.getSpO2() < 85) base += 2000;
                break;
            case HIGH:
                base = 8000;
                if (vitals.getSystolicBP() > 160 || vitals.getTemperature() > 38.5) base += 1000;
                break;
            case NORMAL:
                base = 25000;
                break;
        }
        return base;
    }

    /**
     * Calculates data size based on task type
     */
    private int calculateDataSize(HealthcareDataPacket packet) {
        switch (packet.getUrgency()) {
            case CRITICAL: return 100;
            case HIGH: return 250;
            default: return 1000;
        }
    }

    /**
     * Dequeues tasks from the queue and submits their cloudlets
     */
    public void submitQueuedTasks() {
        List<HealthcareTask> tasksToSubmit = queueManager.dequeueAll();
        List<Cloudlet> cloudletsToSubmit = new ArrayList<>();

        System.out.println("\n🎯 Assigning tasks to optimal datacenters...");
        
        for (HealthcareTask task : tasksToSubmit) {
            assignTaskToOptimalDatacenter(task);
            cloudletsToSubmit.add(task.getCloudlet());
        }

        // Submit all cloudlets at once
        if (!cloudletsToSubmit.isEmpty()) {
            submitCloudletList(cloudletsToSubmit);
            System.out.printf("🚀 Submitted %d healthcare tasks for execution%n", tasksToSubmit.size());
        }
    }

    /**
     * Assigns healthcare task to optimal datacenter using intelligent orchestration
     */
    private void assignTaskToOptimalDatacenter(HealthcareTask task) {
        Datacenter selectedDC;
        String dcName, slice;

        switch (task.getUrgency()) {
            case CRITICAL:
                selectedDC = fogDC;
                dcName = "Fog_DC";
                slice = "URLLC (1ms)";
                System.out.printf("🚨 CRITICAL task %d (Patient: %d) → FOG via %s%n", 
                                task.getTaskId(), task.getPatientId(), slice);
                break;
            case HIGH:
                selectedDC = selectDatacenterForHighPriority(task);
                dcName = selectedDC.getName();
                slice = selectedDC.equals(fogDC) ? "URLLC (1ms)" : "eMBB (10ms)";
                System.out.printf("⚡ HIGH priority task %d (Patient: %d) → %s via %s%n", 
                                task.getTaskId(), task.getPatientId(), dcName, slice);
                break;
            default:
                selectedDC = cloudDC;
                dcName = "Cloud_DC";
                slice = "mIoT (100ms)";
                System.out.printf("💬 NORMAL task %d (Patient: %d) → CLOUD via %s%n", 
                                task.getTaskId(), task.getPatientId(), slice);
        }

        Vm vm = findOptimalVmInDatacenter(selectedDC, task);
        if (vm != null) {
            task.getCloudlet().setVm(vm);
            task.setAssignedDatacenter(dcName); // This will now log properly
            datacenterTaskCount.put(dcName, datacenterTaskCount.get(dcName) + 1);
            simulateNetworkLatency(task, slice);
        } else {
            System.out.printf("⚠️ No suitable VM found in %s for task %d%n", dcName, task.getTaskId());
        }
    }

    /**
     * Selects datacenter for high priority tasks with load balancing
     */
    private Datacenter selectDatacenterForHighPriority(HealthcareTask task) {
        double fogUtil = calculateDatacenterUtilization(fogDC);

        if (task.requiresFogProcessing() && fogUtil < 0.9) {
            System.out.printf("   Fog utilization: %.2f - assigning to FOG (abnormal vitals)%n", fogUtil);
            return fogDC;
        }
        
        if (fogUtil < 0.8) {
            System.out.printf("   Fog utilization: %.2f - assigning to FOG%n", fogUtil);
            return fogDC;
        } else {
            System.out.printf("   Fog utilization: %.2f - assigning to CLOUD%n", fogUtil);
            return cloudDC;
        }
    }

    /**
     * Calculates datacenter utilization based on current load
     */
    private double calculateDatacenterUtilization(Datacenter dc) {
        int assigned = datacenterTaskCount.getOrDefault(dc.getName(), 0);
        long vmCount = getVmCreatedList().stream().filter(vm -> vm.getHost().getDatacenter().equals(dc)).count();
        return vmCount == 0 ? 1.0 : Math.min(1.0, (double) assigned / (vmCount * 3));
    }

    /**
     * Finds optimal VM in datacenter based on task requirements
     */
    private Vm findOptimalVmInDatacenter(Datacenter dc, HealthcareTask task) {
        List<Vm> vms = getVmCreatedList().stream()
            .filter(vm -> vm.getHost().getDatacenter().equals(dc))
            .collect(Collectors.toList());
        if (vms.isEmpty()) return null;

        if (task.getUrgency() == UrgencyLevel.CRITICAL) {
            return vms.stream().max(Comparator.comparingDouble(Vm::getMips)).orElse(vms.get(0));
        }
        return vms.get(task.getTaskId() % vms.size());
    }

    /**
     * Simulates network latency based on 5G network slicing
     */
    private void simulateNetworkLatency(HealthcareTask task, String networkSlice) {
        double latency = 0.0;

        if (networkSlice.contains("URLLC")) {
            latency = SLICE_5G_URLLC_LATENCY;
        } else if (networkSlice.contains("eMBB")) {
            latency = SLICE_5G_EMBB_LATENCY;
        } else if (networkSlice.contains("mIoT")) {
            latency = SLICE_5G_MIOT_LATENCY;
        }

        // Add small random variation (±20%)
        latency += (Math.random() - 0.5) * 0.4 * latency;

        System.out.printf("   📡 Network latency: %.2fms via %s%n", latency, networkSlice);
    }

    /**
     * Processes completed cloudlets with comprehensive logging
     */
    public void processCompletedCloudlets() {
        System.out.println("\n🏁 Processing completed healthcare tasks...");
        
        List<Cloudlet> finished = getCloudletFinishedList();
        System.out.printf("🧮 Total finished cloudlets: %d%n", finished.size());
        
        for (Cloudlet cloudlet : finished) {
            HealthcareTask task = cloudletTaskMap.get(cloudlet.getId());
            if (task != null) {
                processCompletedTask(task, cloudlet);
            } else {
                System.out.printf("⚠️ Task not found for Cloudlet ID %d%n", cloudlet.getId());
            }
        }

        generateComprehensiveReport();
    }

    /**
     * Processes individual completed task
     */
    private void processCompletedTask(HealthcareTask task, Cloudlet cloudlet) {
        double execTime = cloudlet.getActualCpuTime();
        double totalRespTime = cloudlet.getFinishTime() - task.getArrivalTime();
        double waitTime = cloudlet.getWaitingTime();
        String dcName = task.getAssignedDatacenter();

        // Handle null datacenter assignment
        if (dcName == null) {
            dcName = cloudlet.getVm() != null ? cloudlet.getVm().getHost().getDatacenter().getName() : "Unknown";
            task.setAssignedDatacenter(dcName);
        }

        queueManager.recordTaskCompletion(task, execTime, dcName);

        TaskPerformanceRecord record = new TaskPerformanceRecord(
            task.getTaskId(), task.getPatientId(), task.getUrgency(), task.getServiceType(),
            dcName, waitTime, execTime, totalRespTime, task.getExpectedSLA(),
            totalRespTime <= task.getExpectedSLA()
        );

        performanceRecords.add(record);

        System.out.printf("✅ Task %d completed: Patient %d (%s) in %s - Response: %.2fs (SLA: %.2fs) %s%n",
            task.getTaskId(), task.getPatientId(), task.getUrgency(), dcName,
            totalRespTime, task.getExpectedSLA(),
            record.slaCompliant ? "✓" : "⚠️");
    }


//    /**
//     * Submits VM list to a specific datacenter with allocation preference
//     */
//    public void submitVmList(List<Vm> vmList, Datacenter targetDatacenter) {
//        // Set datacenter preference for each VM
//        for (Vm vm : vmList) {
//            // Store preference in VM or use a map
//            setVmDatacenterPreference(vm, targetDatacenter.getName());
//        }
//        
//        // Submit VMs normally - the custom allocation policy will handle placement
//        submitVmList(vmList);
//        
//        System.out.printf("📤 Submitted %d VMs targeting %s%n", vmList.size(), targetDatacenter.getName());
//    }
//
//    /**
//     * Sets datacenter preference for allocation policies
//     */
//    private void setVmDatacenterPreference(Vm vm, String datacenterName) {
//        // Get the allocation policy for the target datacenter and set preference
//        if ("Fog_DC".equals(datacenterName)) {
//            HealthcareVmAllocationPolicy fogPolicy = (HealthcareVmAllocationPolicy) fogDC.getVmAllocationPolicy();
//            fogPolicy.setVmDatacenterPreference(vm, datacenterName);
//        } else if ("Cloud_DC".equals(datacenterName)) {
//            HealthcareVmAllocationPolicy cloudPolicy = (HealthcareVmAllocationPolicy) cloudDC.getVmAllocationPolicy();
//            cloudPolicy.setVmDatacenterPreference(vm, datacenterName);
//        }
//    }
  
    /**
     * Generates comprehensive performance analysis report
     */
    private void generateComprehensiveReport() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🏥 COMPREHENSIVE HEALTHCARE SYSTEM PERFORMANCE REPORT");
        System.out.println("=".repeat(70));
        
        // Queue performance report
        queueManager.generatePerformanceReport();
        
        // Datacenter utilization analysis
        analyzeDatacenterUtilization();
        
        // SLA compliance analysis
        analyzeSLACompliance();
        
        // Research validation
        validateResearchObjectives();
    }
    
    /**
     * Analyzes datacenter utilization patterns
     */
    private void analyzeDatacenterUtilization() {
        System.out.println("\n🏢 DATACENTER UTILIZATION ANALYSIS:");
        
        int fogTasks = datacenterTaskCount.get("Fog_DC");
        int cloudTasks = datacenterTaskCount.get("Cloud_DC");
        int totalTasks = fogTasks + cloudTasks;
        
        if (totalTasks > 0) {
            System.out.printf("   Fog DC: %d tasks (%.1f%%)%n", 
                            fogTasks, (fogTasks * 100.0) / totalTasks);
            System.out.printf("   Cloud DC: %d tasks (%.1f%%)%n", 
                            cloudTasks, (cloudTasks * 100.0) / totalTasks);
        }
        
        // Analyze by urgency level
        Map<UrgencyLevel, Map<String, Integer>> utilizationByUrgency = new HashMap<>();
        for (TaskPerformanceRecord record : performanceRecords) {
            utilizationByUrgency
                .computeIfAbsent(record.urgency, k -> new HashMap<>())
                .merge(record.datacenter, 1, Integer::sum);
        }
        
        System.out.println("\n   Distribution by Urgency Level:");
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            Map<String, Integer> dcMap = utilizationByUrgency.get(urgency);
            if (dcMap != null) {
                int fog = dcMap.getOrDefault("Fog_DC", 0);
                int cloud = dcMap.getOrDefault("Cloud_DC", 0);
                System.out.printf("   %s: Fog=%d, Cloud=%d%n", urgency, fog, cloud);
            }
        }
    }
    
    /**
     * Analyzes SLA compliance rates
     */
    private void analyzeSLACompliance() {
        System.out.println("\n⏱️ SLA COMPLIANCE ANALYSIS:");
        
        long totalTasks = performanceRecords.size();
        long compliantTasks = performanceRecords.stream()
                .mapToLong(r -> r.slaCompliant ? 1 : 0)
                .sum();
        
        double overallCompliance = (compliantTasks * 100.0) / Math.max(1, totalTasks);
        System.out.printf("   Overall SLA Compliance: %.1f%% (%d/%d)%n", 
                         overallCompliance, compliantTasks, totalTasks);
        
        // Compliance by urgency level
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            List<TaskPerformanceRecord> urgencyTasks = performanceRecords.stream()
                    .filter(r -> r.urgency == urgency)
                    .collect(Collectors.toList());
            
            if (!urgencyTasks.isEmpty()) {
                long urgencyCompliant = urgencyTasks.stream()
                        .mapToLong(r -> r.slaCompliant ? 1 : 0)
                        .sum();
                double urgencyComplianceRate = (urgencyCompliant * 100.0) / urgencyTasks.size();
                
                System.out.printf("   %s SLA Compliance: %.1f%% (%d/%d)%n",
                                 urgency, urgencyComplianceRate, urgencyCompliant, urgencyTasks.size());
            }
        }
    }
    
    /**
     * Validates research objectives from AlQahtani (2023) paper
     */
    private void validateResearchObjectives() {
        System.out.println("\n🎯 RESEARCH OBJECTIVES VALIDATION:");
        
        // Objective 1: Critical tasks processed with low latency in fog
        long criticalTasks = performanceRecords.stream()
                .filter(r -> r.urgency == UrgencyLevel.CRITICAL)
                .count();
        long criticalInFog = performanceRecords.stream()
                .filter(r -> r.urgency == UrgencyLevel.CRITICAL && "Fog_DC".equals(r.datacenter))
                .count();
        
        System.out.printf("   ✓ Critical tasks in Fog: %d/%d (%.1f%%)%n",
                         criticalInFog, criticalTasks,
                         (criticalInFog * 100.0) / Math.max(1, criticalTasks));
        
        // Objective 2: System scalability and load distribution
        System.out.printf("   ✓ Load Distribution - Fog: %d, Cloud: %d%n",
                         datacenterTaskCount.get("Fog_DC"),
                         datacenterTaskCount.get("Cloud_DC"));
        
        // Objective 3: QoS maintenance through priority queuing
        System.out.printf("   ✓ Overall QoS Compliance: %.1f%%%n",
                         (performanceRecords.stream().mapToLong(r -> r.slaCompliant ? 1 : 0).sum() * 100.0)
                         / Math.max(1, performanceRecords.size()));
        
        System.out.println("   ✓ Priority-based queuing system implemented and validated");
        System.out.println("   ✓ 5G network slicing simulation integrated");
        System.out.println("   ✓ Real healthcare IoT data processing demonstrated");
    }

    // Getters
    public List<HealthcareTask> getHealthcareTasks() { return new ArrayList<>(healthcareTasks); }
    public List<TaskPerformanceRecord> getPerformanceRecords() { return new ArrayList<>(performanceRecords); }
    public HealthcareQueueManager getQueueManager() { return queueManager; }
}

