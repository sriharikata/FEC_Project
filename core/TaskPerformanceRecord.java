package org.cloudbus.cloudsim.fec_healthsim.core;

import org.cloudbus.cloudsim.fec_healthsim.data.UrgencyLevel;

public class TaskPerformanceRecord {
    final public int taskId;
    final public int patientId;
    final public UrgencyLevel urgency;
    final public ServiceType serviceType;
    final public String datacenter;
    final public double waitingTime;
    final public double executionTime;
    final public double totalResponseTime;
    final public double expectedSla;
    final public boolean slaCompliant;

    TaskPerformanceRecord(int taskId, int patientId, UrgencyLevel urgency, ServiceType serviceType,
                          String datacenter, double waitingTime, double executionTime,
                          double totalResponseTime, double expectedSla, boolean slaCompliant) {
        this.taskId = taskId;
        this.patientId = patientId;
        this.urgency = urgency;
        this.serviceType = serviceType;
        this.datacenter = datacenter;
        this.waitingTime = waitingTime;
        this.executionTime = executionTime;
        this.totalResponseTime = totalResponseTime;
        this.expectedSla = expectedSla;
        this.slaCompliant = slaCompliant;
    }
}