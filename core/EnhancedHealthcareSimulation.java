package org.cloudbus.cloudsim.fec_healthsim.core;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;

import org.cloudbus.cloudsim.fec_healthsim.data.HealthcareDataLoader;
import org.cloudbus.cloudsim.fec_healthsim.data.HealthcareDataPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Healthcare Simulation - Day 1 Implementation
 * Demonstrates fog-cloud healthcare system with real IoT data and priority queuing
 */
public class EnhancedHealthcareSimulation {

    public static void main(String[] args) {
        System.out.println("🏥 Starting Enhanced Healthcare Fog-Edge Simulation");
        System.out.println("   Research Paper: AlQahtani (2023) - 5G IoT Healthcare System");
        System.out.println("=".repeat(70));

        // Step 1: Create CloudSim instance
        CloudSim simulation = new CloudSim();

        // Step 2: Create Fog and Cloud datacenters with realistic configurations
        Datacenter fogDC = createDatacenter(simulation, "Fog_DC", 2, 15000, true);
        Datacenter cloudDC = createDatacenter(simulation, "Cloud_DC", 4, 10000, false);

        // Step 3: Create enhanced healthcare service broker
        EnhancedHealthcareServiceBroker broker = new EnhancedHealthcareServiceBroker(simulation, fogDC, cloudDC);

        // Step 4: Create and submit VMs with varied configurations
        List<Vm> vmList = createVMs();
        broker.submitVmList(vmList);
        System.out.printf("🖥️ Created %d VMs across fog and cloud datacenters%n", vmList.size());
        // Step 4: Create and submit VMs with explicit datacenter assignment
//        createAndSubmitVMs(broker, fogDC, cloudDC);

        // Step 5: Generate realistic healthcare IoT data
        System.out.println("\n📊 Generating healthcare IoT data scenarios...");
        List<HealthcareDataPacket> healthcareData = HealthcareDataLoader.generateHealthcareData(8, 2);

        // Step 6: Process healthcare data through broker and queue
        broker.processHealthcareData(healthcareData);

        // Step 7: Submit queued tasks for intelligent assignment
        System.out.println("\n🎯 Processing queued tasks through intelligent orchestration...");
        broker.submitQueuedTasks();

        // Step 8: Start simulation
        System.out.println("\n🚀 Starting CloudSim simulation...");
        long startTime = System.currentTimeMillis();
        simulation.start();
        long endTime = System.currentTimeMillis();

        // Step 9: Process results and generate reports
        System.out.printf("\n✅ Simulation completed in %d ms%n", (endTime - startTime));
        broker.processCompletedCloudlets();

        System.out.println("\n🎉 Enhanced Healthcare Fog-Edge Simulation Complete!");
    }

    /**
     * Creates a datacenter with healthcare-specific configurations
     */
    private static Datacenter createDatacenter(CloudSim simulation, String name, int hostCount,
                                               int hostMips, boolean isFog) {
        List<Host> hostList = new ArrayList<>();
        System.out.printf("🏗️ Creating %s with %d hosts (%d MIPS each)%n", name, hostCount, hostMips);

        for (int i = 0; i < hostCount; i++) {
            List<Pe> peList = new ArrayList<>();
            int coreCount = isFog ? 2 : 4;
            for (int j = 0; j < coreCount; j++) {
                peList.add(new PeSimple(hostMips));
            }

            long ram = isFog ? 16384 : 32768;
            long storage = isFog ? 1000000 : 2000000;
            long bw = isFog ? 100000 : 50000;

            Host host = new HostSimple(ram, bw, storage, peList);
            host.setVmScheduler(new VmSchedulerTimeShared());
            hostList.add(host);
        }

        Datacenter datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        datacenter.setName(name);
        System.out.printf("   ✓ %s created with %d hosts, %d total cores%n",
                name, hostCount, hostCount * (isFog ? 2 : 4));
        return datacenter;
    }

    /**
     * Creates VMs with healthcare workload-optimized configurations
     */
    private static List<Vm> createVMs() {
        List<Vm> vmList = new ArrayList<>();
        System.out.println("🖥️ Creating healthcare-optimized VMs...");

        // Critical processing VMs (fog)
        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(3000 + (i * 200), 2);
            vm.setRam(4096).setBw(10000).setSize(50000);
            vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
            vmList.add(vm);
            if (i < 2)
                System.out.printf("   Critical VM %d: %d MIPS, 2 cores (Fog-optimized)%n", i, 3000 + (i * 200));
        }

        // Monitoring VMs (balanced)
        for (int i = 4; i < 8; i++) {
            Vm vm = new VmSimple(2000 + (i * 100), 2);
            vm.setRam(2048).setBw(8000).setSize(40000);
            vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
            vmList.add(vm);
        }

        // Consultation VMs (cloud)
        for (int i = 8; i < 12; i++) {
            Vm vm = new VmSimple(1500 + (i * 50), 1);
            vm.setRam(1024).setBw(5000).setSize(30000);
            vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
            vmList.add(vm);
            if (i >= 10)
                System.out.printf("   Consultation VM %d: %d MIPS, 1 core (Cloud-optimized)%n", i, 1500 + (i * 50));
        }

        return vmList;
    }
    
    /**
    * Creates and submits VMs with explicit datacenter assignment
    */
   private static void createAndSubmitVMs(EnhancedHealthcareServiceBroker broker, 
                                        Datacenter fogDC, Datacenter cloudDC) {
       System.out.println("🖥️ Creating healthcare-optimized VMs...");
       
       // Critical processing VMs for FOG datacenter (VMs 0-3)
       List<Vm> fogVMs = new ArrayList<>();
       for (int i = 0; i < 4; i++) {
           Vm vm = new VmSimple(3000 + (i * 200), 2);
           vm.setRam(4096).setBw(10000).setSize(50000);
           vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
           fogVMs.add(vm);
           if (i < 2) {
               System.out.printf("   Critical VM %d: %d MIPS, 2 cores (Fog-optimized)%n", i, 3000 + (i * 200));
           }
       }
       
       // Submit FOG VMs to specific datacenter
       broker.submitVmList(fogVMs, fogDC);
       
       // Cloud processing VMs for CLOUD datacenter (VMs 4-11)
       List<Vm> cloudVMs = new ArrayList<>();
       
       // Monitoring VMs (balanced) for cloud
       for (int i = 4; i < 8; i++) {
           Vm vm = new VmSimple(2000 + (i * 100), 2);
           vm.setRam(2048).setBw(8000).setSize(40000);
           vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
           cloudVMs.add(vm);
       }

       // Consultation VMs (cloud)
       for (int i = 8; i < 12; i++) {
           Vm vm = new VmSimple(1500 + (i * 50), 1);
           vm.setRam(1024).setBw(5000).setSize(30000);
           vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
           cloudVMs.add(vm);
           if (i >= 10) {
               System.out.printf("   Consultation VM %d: %d MIPS, 1 core (Cloud-optimized)%n", i, 1500 + (i * 50));
           }
       }
       
       // Submit CLOUD VMs to specific datacenter
       broker.submitVmList(cloudVMs, cloudDC);
       
       System.out.printf("🖥️ Created %d VMs: %d in Fog, %d in Cloud%n", 
                        fogVMs.size() + cloudVMs.size(), fogVMs.size(), cloudVMs.size());
   }
}
