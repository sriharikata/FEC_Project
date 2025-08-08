package org.cloudbus.cloudsim.fec_healthsim.utils;

//import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple; //Commenting this to use the new custom policy
import org.cloudbus.cloudsim.fec_healthsim.core.HealthcareVmAllocationPolicy;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.List;

public class DatacenterUtils {

    public static Datacenter createDatacenter(CloudSim simulation, int mips, int numHosts, String name) {
        List<Host> hostList = new ArrayList<>();

        for (int i = 0; i < numHosts; i++) {
            List<Pe> peList = List.of(new PeSimple(mips)); // 1 processing element (core)

            Host host = new HostSimple(2048, 10000, 1000000, peList) // RAM, BW, Storage
                    .setVmScheduler(new VmSchedulerTimeShared());

            hostList.add(host);
        }

//        Datacenter datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        HealthcareVmAllocationPolicy policy = new HealthcareVmAllocationPolicy(name);
        Datacenter datacenter = new DatacenterSimple(simulation, hostList, policy);
        datacenter.setName(name);
        return datacenter;
    }

    public static List<Vm> createVMs(CloudSim simulation, int count, int mips) {
        List<Vm> vmList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Vm vm = new VmSimple(mips, 1) // 1 core
                    .setRam(512)
                    .setBw(1000)
                    .setSize(10000);
            vmList.add(vm);
        }

        return vmList;
    }
}
