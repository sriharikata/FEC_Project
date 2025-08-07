package org.cloudbus.cloudsim.fec_healthsim.core;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A custom VM allocation policy for a healthcare fog-cloud system in CloudSim Plus 6.2.7.
 * This policy allocates VMs to hosts based on a preferred datacenter, ensuring VMs are only
 * placed in the intended datacenter.
 */
public class HealthcareVmAllocationPolicy extends VmAllocationPolicyAbstract {

    private final Map<Long, String> vmDatacenterMap;
    private final String datacenterName;

    /**
     * Creates a new HealthcareVmAllocationPolicy for a specific datacenter.
     *
     * @param datacenterName the name of the datacenter this policy is associated with
     */
    public HealthcareVmAllocationPolicy(String datacenterName) {
        super();
        this.datacenterName = datacenterName != null ? datacenterName : "";
        this.vmDatacenterMap = new HashMap<>();
    }

    /**
     * Sets the preferred datacenter for a VM.
     *
     * @param vm the VM to set the preference for
     * @param intendedDatacenter the name of the intended datacenter
     */
    public void setVmDatacenterPreference(Vm vm, String intendedDatacenter) {
        if (vm == null || intendedDatacenter == null) {
            System.out.printf("⚠️ Invalid VM or datacenter provided for preference setting%n");
            return;
        }
        vmDatacenterMap.put(vm.getId(), intendedDatacenter);
        System.out.printf("🎯 VM %d preference set to: %s%n", vm.getId(), intendedDatacenter);
    }

    /**
     * Allocates a host for the given VM if the datacenter matches the VM's preference.
     *
     * @param vm the VM to allocate a host for
     * @return true if a suitable host was found and the VM was allocated, false otherwise
     */
    @Override
    public boolean allocateHostForVm(Vm vm) {
        if (vm == null) {
            System.out.printf("❌ Null VM provided for allocation%n");
            return false;
        }

        String intendedDC = vmDatacenterMap.getOrDefault(vm.getId(), "");
        if (!intendedDC.equals(datacenterName)) {
            System.out.printf("🚫 VM %d intended for %s, this is %s — skipping allocation%n",
                    vm.getId(), intendedDC, datacenterName);
            return false;
        }

        for (Host host : getHostList()) {
            if (host == null) {
                continue;
            }
            HostSuitability suitability = host.isSuitableForVm(vm);
            if (suitability.isSuitable()) {
                suitability = host.createVm(vm);
                if (suitability.isSuitable()) {
                    System.out.printf("✅ VM %d allocated to Host %d in %s%n",
                            vm.getId(), host.getId(), datacenterName);
                    return true;
                }
            }
        }

        System.out.printf("❌ No suitable host found for VM %d in %s%n", vm.getId(), datacenterName);
        return false;
    }

    /**
     * Allocates a specific host for the given VM if the datacenter matches and the host is suitable.
     *
     * @param vm the VM to allocate
     * @param host the host to allocate the VM to
     * @return true if the VM was successfully allocated to the host, false otherwise
     */
    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        if (vm == null || host == null) {
            System.out.printf("❌ Null VM or Host provided for allocation%n");
            return false;
        }

        String intendedDC = vmDatacenterMap.getOrDefault(vm.getId(), "");
        if (!intendedDC.equals(datacenterName)) {
            System.out.printf("🚫 VM %d intended for %s, this is %s — skipping allocation%n",
                    vm.getId(), intendedDC, datacenterName);
            return false;
        }

        HostSuitability suitability = host.isSuitableForVm(vm);
        if (suitability.isSuitable()) {
            suitability = host.createVm(vm);
            if (suitability.isSuitable()) {
                System.out.printf("✅ VM %d allocated to Host %d in %s%n",
                        vm.getId(), host.getId(), datacenterName);
                return true;
            }
        }

        System.out.printf("❌ Host %d not suitable for VM %d in %s%n",
                host.getId(), vm.getId(), datacenterName);
        return false;
    }

    /**
     * Deallocates the host for the given VM.
     *
     * @param vm the VM to deallocate
     */
    @Override
    public void deallocateHostForVm(Vm vm) {
        if (vm == null) {
            System.out.printf("❌ Null VM provided for deallocation%n");
            return;
        }
        Host host = vm.getHost();
        if (host != null && host != Host.NULL) {
            host.destroyVm(vm);
            System.out.printf("🗑️ VM %d deallocated from %s%n", vm.getId(), datacenterName);
        }
    }

    /**
     * Finds a suitable host for the given VM.
     *
     * @param vm the VM to find a host for
     * @return an Optional containing a suitable host, or empty if none is found
     */
    @Override
    protected Optional<Host> defaultFindHostForVm(Vm vm) {
        if (vm == null) {
            return Optional.empty();
        }
        for (Host host : getHostList()) {
            if (host != null && host.isSuitableForVm(vm).isSuitable()) {
                return Optional.of(host);
            }
        }
        return Optional.empty();
    }
}