package org.cloudbus.cloudsim.fec_healthsim.data;


public class VitalSigns {
    private final int heartRate;
    private final int systolicBP;
    private final double temperature;
    private final int spO2;

    public VitalSigns(int heartRate, int systolicBP, double temperature, int spO2) {
        this.heartRate = heartRate;
        this.systolicBP = systolicBP;
        this.temperature = temperature;
        this.spO2 = spO2;
    }

    public int getHeartRate() { return heartRate; }
    public int getSystolicBP() { return systolicBP; }
    public double getTemperature() { return temperature; }
    public int getSpO2() { return spO2; }

    @Override
    public String toString() {
        return String.format("HR:%d, BP:%d, Temp:%.1f°C, SpO2:%d%%",
                heartRate, systolicBP, temperature, spO2);
    }
}
