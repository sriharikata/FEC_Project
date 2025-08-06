package org.cloudbus.cloudsim.fec_healthsim.data;


public class PatientProfile {
    private final int patientId;
    private final int age;
    private final String condition;

    public PatientProfile(int patientId, int age, String condition) {
        this.patientId = patientId;
        this.age = age;
        this.condition = condition;
    }

    public int getPatientId() { return patientId; }
    public int getAge() { return age; }
    public String getCondition() { return condition; }
}

