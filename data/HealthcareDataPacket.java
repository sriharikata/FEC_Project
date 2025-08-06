package org.cloudbus.cloudsim.fec_healthsim.data;

 
public class HealthcareDataPacket {
    private final int patientId;
    private final long timestamp;
    private final VitalSigns vitals;
    private final UrgencyLevel urgency;
    private final String condition;

    public HealthcareDataPacket(int patientId, long timestamp, VitalSigns vitals,
                               UrgencyLevel urgency, String condition) {
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.vitals = vitals;
        this.urgency = urgency;
        this.condition = condition;
    }

    public int getPatientId() { return patientId; }
    public long getTimestamp() { return timestamp; }
    public VitalSigns getVitals() { return vitals; }
    public UrgencyLevel getUrgency() { return urgency; }
    public String getCondition() { return condition; }

    @Override
    public String toString() {
        return String.format("Patient:%d [%s] %s - %s",
                patientId, urgency, vitals, condition);
    }
}
