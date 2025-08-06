package org.cloudbus.cloudsim.fec_healthsim.data;


import java.util.*;

/**
 * Healthcare Data Loader - Generates realistic IoT healthcare sensor data
 * This class simulates real healthcare datasets with patient vitals and emergency scenarios
 */
public class HealthcareDataLoader {
    private static final Random random = new Random();
    
    // Normal vital sign ranges
    private static final int NORMAL_HEART_RATE_MIN = 60;
    private static final int NORMAL_HEART_RATE_MAX = 100;
    private static final int NORMAL_BP_SYSTOLIC_MIN = 110;
    private static final int NORMAL_BP_SYSTOLIC_MAX = 140;
    private static final int NORMAL_TEMP_MIN = 36;
    private static final int NORMAL_TEMP_MAX = 38;
    private static final int NORMAL_SPO2_MIN = 95;
    private static final int NORMAL_SPO2_MAX = 100;
    
    /**
     * Generates a list of healthcare IoT data packets
     */
    public static List<HealthcareDataPacket> generateHealthcareData(int patientCount, int readingsPerPatient) {
        List<HealthcareDataPacket> dataPackets = new ArrayList<>();
        
        System.out.println("🔬 Generating healthcare IoT data...");
        System.out.printf("   Patients: %d, Readings per patient: %d%n", patientCount, readingsPerPatient);
        
        for (int patientId = 1; patientId <= patientCount; patientId++) {
            PatientProfile profile = generatePatientProfile(patientId);
            
            for (int reading = 0; reading < readingsPerPatient; reading++) {
                HealthcareDataPacket packet = generateDataPacket(profile, reading);
                dataPackets.add(packet);
            }
        }
        
        // Add some emergency scenarios
        addEmergencyScenarios(dataPackets);
        
        System.out.printf("✅ Generated %d healthcare data packets%n", dataPackets.size());
        return dataPackets;
    }
    
    /**
     * Generates a patient profile with baseline health parameters
     */
    private static PatientProfile generatePatientProfile(int patientId) {
        String[] conditions = {"Healthy", "Hypertension", "Diabetes", "Cardiac", "Post-Surgery"};
        String condition = conditions[random.nextInt(conditions.length)];
        
        int age = 25 + random.nextInt(65); // Age 25-90
        
        return new PatientProfile(patientId, age, condition);
    }
    
    /**
     * Generates a single healthcare data packet for a patient
     */
    private static HealthcareDataPacket generateDataPacket(PatientProfile profile, int sequenceNum) {
        VitalSigns vitals = generateVitalSigns(profile);
        long timestamp = System.currentTimeMillis() + (sequenceNum * 30000); // 30 sec intervals
        
        // Determine urgency based on vital signs
        UrgencyLevel urgency = determineUrgency(vitals);
        
        return new HealthcareDataPacket(
            profile.getPatientId(),
            timestamp,
            vitals,
            urgency,
            profile.getCondition()
        );
    }
    
    /**
     * Generates realistic vital signs based on patient profile
     */
    private static VitalSigns generateVitalSigns(PatientProfile profile) {
        int heartRate = generateHeartRate(profile);
        int systolicBP = generateBloodPressure(profile);
        double temperature = generateTemperature(profile);
        int spO2 = generateSpO2(profile);
        
        return new VitalSigns(heartRate, systolicBP, temperature, spO2);
    }
    
    /**
     * Generate heart rate based on patient condition
     */
    private static int generateHeartRate(PatientProfile profile) {
        int baseRate = NORMAL_HEART_RATE_MIN + random.nextInt(NORMAL_HEART_RATE_MAX - NORMAL_HEART_RATE_MIN);
        
        // Adjust based on condition
        switch (profile.getCondition()) {
            case "Cardiac":
                baseRate += random.nextInt(40); // Can be elevated
                break;
            case "Post-Surgery":
                baseRate += random.nextInt(30); // Slightly elevated
                break;
            case "Hypertension":
                baseRate += random.nextInt(20);
                break;
        }
        
        // 5% chance of emergency reading
        if (random.nextDouble() < 0.05) {
            baseRate = 140 + random.nextInt(60); // Emergency range
        }
        
        return Math.min(200, baseRate);
    }
    
    /**
     * Generate blood pressure
     */
    private static int generateBloodPressure(PatientProfile profile) {
        int baseBP = NORMAL_BP_SYSTOLIC_MIN + random.nextInt(NORMAL_BP_SYSTOLIC_MAX - NORMAL_BP_SYSTOLIC_MIN);
        
        switch (profile.getCondition()) {
            case "Hypertension":
                baseBP += 20 + random.nextInt(30);
                break;
            case "Post-Surgery":
                // Can be low due to medication
                if (random.nextBoolean()) {
                    baseBP -= random.nextInt(20);
                }
                break;
        }
        
        // 3% chance of hypertensive crisis
        if (random.nextDouble() < 0.03) {
            baseBP = 180 + random.nextInt(40);
        }
        
        return Math.max(80, Math.min(250, baseBP));
    }
    
    /**
     * Generate body temperature
     */
    private static double generateTemperature(PatientProfile profile) {
        double baseTemp = NORMAL_TEMP_MIN + (random.nextDouble() * (NORMAL_TEMP_MAX - NORMAL_TEMP_MIN));
        
        // Post-surgery patients might have fever
        if ("Post-Surgery".equals(profile.getCondition()) && random.nextDouble() < 0.15) {
            baseTemp += 1 + random.nextDouble() * 2; // Fever
        }
        
        // 2% chance of high fever
        if (random.nextDouble() < 0.02) {
            baseTemp = 39 + random.nextDouble() * 2;
        }
        
        return Math.round(baseTemp * 10.0) / 10.0;
    }
    
    /**
     * Generate SpO2 levels
     */
    private static int generateSpO2(PatientProfile profile) {
        int baseSpO2 = NORMAL_SPO2_MIN + random.nextInt(NORMAL_SPO2_MAX - NORMAL_SPO2_MIN);
        
        // Cardiac patients might have lower SpO2
        if ("Cardiac".equals(profile.getCondition()) && random.nextDouble() < 0.1) {
            baseSpO2 -= random.nextInt(10);
        }
        
        // 3% chance of critical low SpO2
        if (random.nextDouble() < 0.03) {
            baseSpO2 = 80 + random.nextInt(10);
        }
        
        return Math.max(70, Math.min(100, baseSpO2));
    }
    
    /**
     * Determines urgency level based on vital signs
     */
    private static UrgencyLevel determineUrgency(VitalSigns vitals) {
        // Critical conditions
        if (vitals.getHeartRate() > 150 || vitals.getHeartRate() < 50 ||
            vitals.getSystolicBP() > 180 || vitals.getSystolicBP() < 90 ||
            vitals.getTemperature() > 40.0 ||
            vitals.getSpO2() < 85) {
            return UrgencyLevel.CRITICAL;
        }
        
        // High priority conditions
        if (vitals.getHeartRate() > 120 || vitals.getHeartRate() < 60 ||
            vitals.getSystolicBP() > 160 || vitals.getSystolicBP() < 100 ||
            vitals.getTemperature() > 38.5 ||
            vitals.getSpO2() < 92) {
            return UrgencyLevel.HIGH;
        }
        
        return UrgencyLevel.NORMAL;
    }
    
    /**
     * Adds some emergency scenarios to the dataset
     */
    private static void addEmergencyScenarios(List<HealthcareDataPacket> dataPackets) {
        int emergencyCount = Math.max(1, dataPackets.size() / 20); // 5% emergency scenarios
        
        for (int i = 0; i < emergencyCount; i++) {
            int randomIndex = random.nextInt(dataPackets.size());
            HealthcareDataPacket packet = dataPackets.get(randomIndex);
            
            // Create emergency vital signs
            VitalSigns emergencyVitals = new VitalSigns(
                180 + random.nextInt(20), // Very high heart rate
                200 + random.nextInt(30), // Hypertensive crisis
                41.0 + random.nextDouble() * 2, // High fever
                75 + random.nextInt(10)   // Low SpO2
            );
            
            // Replace with emergency packet
            HealthcareDataPacket emergencyPacket = new HealthcareDataPacket(
                packet.getPatientId(),
                packet.getTimestamp(),
                emergencyVitals,
                UrgencyLevel.CRITICAL,
                "Emergency"
            );
            
            dataPackets.set(randomIndex, emergencyPacket);
        }
        
        System.out.printf("🚨 Added %d emergency scenarios%n", emergencyCount);
    }
}
