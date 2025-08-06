package org.cloudbus.cloudsim.fec_healthsim.core;


import org.cloudbus.cloudsim.cloudlets.Cloudlet;

import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.fec_healthsim.utils.DatacenterUtils;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.stream.Collectors;


public class HealthcareSimulation {
	private static long cloudletIdCounter = 0;

    public static void main(String[] args) {
        System.out.println("🏥 Starting Healthcare Fog-Edge Simulation");
        System.out.println("=".repeat(50));
        
        CloudSim simulation = new CloudSim();
        
        // Create datacenters with different characteristics
        Datacenter fogDC = DatacenterUtils.createDatacenter(simulation, 3000, 2, "Fog_DC");
        Datacenter cloudDC = DatacenterUtils.createDatacenter(simulation, 2000, 4, "Cloud_DC");
        
        HealthcareServiceBroker broker = new HealthcareServiceBroker(simulation, fogDC, cloudDC);
        
        // Create VMs
        List<Vm> fogVMs = DatacenterUtils.createVMs(simulation, 2, 3000);
        List<Vm> cloudVMs = DatacenterUtils.createVMs(simulation, 6, 2000);
        
        List<Vm> allVMs = new ArrayList<>();
        allVMs.addAll(fogVMs);
        allVMs.addAll(cloudVMs);
        
        broker.submitVmList(allVMs);
        
        // Create healthcare workloads
        List<Cloudlet> cloudletList = new ArrayList<>();
        
        // Critical surgery tasks
        cloudletList.addAll(createCloudlets(broker, 3, 5000, 100, ServiceType.SURGERY));
        
        // Monitoring tasks
        cloudletList.addAll(createCloudlets(broker, 8, 8000, 250, ServiceType.MONITORING));
        
        // Consultation tasks
        cloudletList.addAll(createCloudlets(broker, 4, 25000, 1000, ServiceType.CONSULTATION));
        
        broker.submitCloudletList(cloudletList);
        
        System.out.printf("📋 Created %d cloudlets across %d service types%n", 
                         cloudletList.size(), ServiceType.values().length);
        System.out.println("🚀 Starting simulation...\n");
        
        long startTime = System.currentTimeMillis();
        simulation.start();
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.printf("✅ Simulation completed in %d ms%n", (endTime - startTime));
        
        // Process finished cloudlets with custom logging
        for (Cloudlet cloudlet : broker.getCloudletFinishedList()) {
            broker.onCloudletFinish(cloudlet);
        }
        
        // Simple analysis
        analyzeResults(broker);
    }
    
    /**
     * Creates cloudlets with healthcare service parameters
     */
    private static List<Cloudlet> createCloudlets(HealthcareServiceBroker broker, int count, 
                                                long length, int size, ServiceType type) {
        List<Cloudlet> list = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            CloudletSimple cloudlet = new CloudletSimple(length, 1);
            long cloudletId = broker.getCloudletList().size() + list.size();
            cloudlet.setFileSize(size);
            cloudlet.setId(cloudletIdCounter++);
            cloudlet.setOutputSize(size / 2);
            cloudlet.setUtilizationModel(new UtilizationModelFull());
            
            // Store service type using our custom method
            broker.setCloudletServiceType(cloudlet, type);
            
            list.add(cloudlet);
        }
        
        return list;
    }
    
    /**
     * Simple analysis of results
     */
    private static void analyzeResults(HealthcareServiceBroker broker) {
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        
        System.out.println("\n📊 PERFORMANCE ANALYSIS");
        System.out.println("=".repeat(50));
        
        // Group results by service type
        Map<ServiceType, List<Cloudlet>> cloudletsByType = finishedCloudlets.stream()
                .collect(Collectors.groupingBy(broker::getCloudletServiceType));
        
        // Group results by datacenter
        Map<String, List<Cloudlet>> cloudletsByDC = finishedCloudlets.stream()
                .collect(Collectors.groupingBy(c -> 
                    c.getVm().getHost().getDatacenter().getName()));
        
        System.out.println("\n🔍 Analysis by Service Type:");
        for (ServiceType type : ServiceType.values()) {
            List<Cloudlet> cloudlets = cloudletsByType.getOrDefault(type, new ArrayList<>());
            if (cloudlets.isEmpty()) continue;
            
            long fogCount = cloudlets.stream()
                    .filter(c -> c.getVm().getHost().getDatacenter().getName().equals("Fog_DC"))
                    .count();
            
            double avgExecutionTime = cloudlets.stream()
                    .mapToDouble(Cloudlet::getActualCpuTime)
                    .average().orElse(0.0);
            
            System.out.printf("  %s: %d tasks, Avg Execution: %.2f s, Fog: %d/%d%n",
                             type, cloudlets.size(), avgExecutionTime, fogCount, cloudlets.size());
        }
        
        System.out.println("\n🏢 Analysis by Datacenter:");
        for (String dcName : cloudletsByDC.keySet()) {
            List<Cloudlet> cloudlets = cloudletsByDC.get(dcName);
            
            double totalExecutionTime = cloudlets.stream()
                    .mapToDouble(Cloudlet::getActualCpuTime)
                    .sum();
            
            System.out.printf("  %s: %d tasks, Total CPU Time: %.2f s%n",
                             dcName, cloudlets.size(), totalExecutionTime);
        }
        
        System.out.println("\n🎯 Research Objectives Validation:");
        
        // Critical tasks in Fog
        List<Cloudlet> surgeryTasks = cloudletsByType.getOrDefault(ServiceType.SURGERY, new ArrayList<>());
        long surgeryInFog = surgeryTasks.stream()
                .filter(c -> c.getVm().getHost().getDatacenter().getName().equals("Fog_DC"))
                .count();
        
        System.out.printf("  ✓ Critical tasks in Fog: %d/%d (%.1f%%)%n", 
                         surgeryInFog, surgeryTasks.size(), 
                         (surgeryInFog * 100.0) / Math.max(1, surgeryTasks.size()));
        
        // Non-urgent tasks in Cloud
        List<Cloudlet> consultationTasks = cloudletsByType.getOrDefault(ServiceType.CONSULTATION, new ArrayList<>());
        long consultationInCloud = consultationTasks.stream()
                .filter(c -> c.getVm().getHost().getDatacenter().getName().equals("Cloud_DC"))
                .count();
        
        System.out.printf("  ✓ Non-urgent tasks in Cloud: %d/%d (%.1f%%)%n", 
                         consultationInCloud, consultationTasks.size(),
                         (consultationInCloud * 100.0) / Math.max(1, consultationTasks.size()));
    }
}