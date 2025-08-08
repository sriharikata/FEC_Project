package org.cloudbus.cloudsim.fec_healthsim.core;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom VM Allocation Policy for Healthcare Fog-Cloud System
 * Ensures VMs are allocated according to intended datacenter assignments
 */
public class HealthcareVmAllocationPolicy extends VmAllocationPolicyAbstract {
    
    // Map to store intended datacenter for each VM
    private final Map<Long, String> vmDatacenterMap;
    private final String datacenterName;
    
    public HealthcareVmAllocationPolicy(String datacenterName) {
        super();
        this.datacenterName = datacenterName;
        this.vmDatacenterMap = new HashMap<>();
    }
    
    /**
     * Sets the intended datacenter for a VM
     */
    public void setVmDatacenterPreference(Vm vm, String intendedDatacenter) {
        vmDatacenterMap.put(vm.getId(), intendedDatacenter);
        System.out.printf("🎯 VM %d preference set to: %s%n", vm.getId(), intendedDatacenter);
    }
    
    @Override
    public HostSuitability allocateHostForVm(Vm vm) {
        // Check if this VM should be allocated to this datacenter
        String intendedDC = vmDatacenterMap.get(vm.getId());
        System.out.println("🧩 VM Pref Map in 1: " + vmDatacenterMap);
        
        if (intendedDC != null && !intendedDC.equals(datacenterName)) {
            System.out.printf("🚫 Rejecting VM %d allocation (intended for %s, this is %s)%n", 
                            vm.getId(), intendedDC, datacenterName);
            return new HostSuitability("VM is intended for " + intendedDC + " but this is " + datacenterName); // Reject allocation
        }
        
        // Find suitable host in this datacenter
        Optional<Host> hostOptional = findSuitableHost(vm);
        if (hostOptional.isPresent()) {
            Host host = hostOptional.get();
            HostSuitability suitability = host.createVm(vm);
//            boolean allocated = host.createVm(vm);
            if (suitability.fully()) {
                System.out.printf("✅ VM %d successfully allocated to %s (Host %d)%n",
                        vm.getId(), datacenterName, host.getId());
                return suitability;
            }
        }
        
        System.out.printf("❌ No suitable host found for VM %d in %s%n", vm.getId(), datacenterName);
        return new HostSuitability("VM is intended for " + intendedDC + " but this is " + datacenterName);
    }
    
    /**
     * Finds the most suitable host for VM allocation
     */
    private Optional<Host> findSuitableHost(Vm vm) {
        return getHostList().stream()
        		.filter(host -> host.isSuitableForVm(vm))
            .min(Comparator.comparingDouble(host -> {
                // Prefer less loaded hosts
            	return host.getVmList().size(); // Simple load metric
            }));
    }
    
    @Override
    public HostSuitability allocateHostForVm(Vm vm, Host host) {
        // Check datacenter preference first
        String intendedDC = vmDatacenterMap.get(vm.getId());
        System.out.println("🧩 VM Pref Map in 2: " + vmDatacenterMap);
        if (intendedDC != null && !intendedDC.equals(datacenterName)) {
            return new HostSuitability("VM is intended for " + intendedDC + " but this is " + datacenterName);
        }

        HostSuitability suitability = host.createVm(vm);
        if (suitability.fully()) {
            System.out.printf("✅ VM %d allocated to specific host %d in %s%n",
                    vm.getId(), host.getId(), datacenterName);
        } else {
            System.out.printf("❌ Allocation failed for VM %d at host %d (%s)%n",
                    vm.getId(), host.getId(), suitability.toString());
        }

        return suitability;
    }

    
    @Override
    public void deallocateHostForVm(Vm vm) {
        Host host = vm.getHost();
        if (host != null) {
            host.destroyVm(vm);
            System.out.printf("🗑️ VM %d deallocated from %s%n", vm.getId(), datacenterName);
        }
    }
    
    
    @Override
    protected Optional<Host> defaultFindHostForVm(Vm vm) {
        // Check datacenter preference first
        String intendedDC = vmDatacenterMap.get(vm.getId());
        if (intendedDC != null && !intendedDC.equals(datacenterName)) {
            return Optional.empty(); // Don't find host if VM doesn't belong here
        }
        
        return findSuitableHost(vm);
    }
    
    public Map<Long, String> getVmDatacenterMap() {
        return vmDatacenterMap;
    }

}