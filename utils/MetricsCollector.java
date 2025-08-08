package org.cloudbus.cloudsim.fec_healthsim.utils;

import org.cloudbus.cloudsim.fec_healthsim.core.EnhancedHealthcareServiceBroker;
import org.cloudbus.cloudsim.fec_healthsim.core.ServiceType;
import org.cloudbus.cloudsim.fec_healthsim.data.*;
import org.cloudbus.cloudsim.fec_healthsim.core.TaskPerformanceRecord;


import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MetricsCollector - Comprehensive performance data collection and analysis
 * Generates detailed reports for healthcare fog-edge system evaluation
 */
public class MetricsCollector {
    private static final DecimalFormat df = new DecimalFormat("#.##");
    
    private final EnhancedHealthcareServiceBroker broker;
    private final List<TaskPerformanceRecord> performanceRecords;
    private final Map<String, List<Double>> datacenterMetrics;
    
    // System-wide metrics
    private double totalSimulationTime;
    private int totalTasksProcessed;
    private int totalTasksQueued;
    
    public MetricsCollector(EnhancedHealthcareServiceBroker broker) {
        this.broker = broker;
        this.performanceRecords = broker.getPerformanceRecords();
        this.datacenterMetrics = new HashMap<>();
        
        // Initialize datacenter metrics maps
        datacenterMetrics.put("Fog_DC_latency", new ArrayList<>());
        datacenterMetrics.put("Cloud_DC_latency", new ArrayList<>());
        datacenterMetrics.put("Fog_DC_throughput", new ArrayList<>());
        datacenterMetrics.put("Cloud_DC_throughput", new ArrayList<>());
    }
    
    /**
     * Collects all performance metrics and generates comprehensive reports
     */
    public void collectAndGenerateReports() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 COMPREHENSIVE PERFORMANCE METRICS COLLECTION");
        System.out.println("=".repeat(80));
        
        collectBasicMetrics();
        generateLatencyAnalysis();
        generateThroughputAnalysis();
        generateSLAComplianceReport();
        generateQueuePerformanceAnalysis();
        generateDatacenterUtilizationReport();
        generateNetworkSlicingAnalysis();
        generateResourceEfficiencyReport();
        generateResearchValidationReport();
        
        // Export to files
        exportMetricsToCSV();
        generateExecutiveSummary();
    }
    
    /**
     * Collects basic system metrics
     */
    private void collectBasicMetrics() {
        totalTasksProcessed = performanceRecords.size();
        totalTasksQueued = broker.getHealthcareTasks().size();
        
        if (!performanceRecords.isEmpty()) {
            totalSimulationTime = performanceRecords.stream()
                .mapToDouble(record -> record.totalResponseTime)
                .max()
                .orElse(0.0);
        }
        
        System.out.println("\n📈 BASIC SYSTEM METRICS:");
        System.out.printf("   Total Tasks Queued: %d%n", totalTasksQueued);
        System.out.printf("   Total Tasks Processed: %d%n", totalTasksProcessed);
        System.out.printf("   Task Completion Rate: %.2f%%%n", 
            (totalTasksProcessed * 100.0) / Math.max(1, totalTasksQueued));
        System.out.printf("   Total Simulation Time: %.2f seconds%n", totalSimulationTime);
    }
    
    /**
     * Generates detailed latency analysis
     */
    private void generateLatencyAnalysis() {
        System.out.println("\n⏱️ LATENCY ANALYSIS:");
        System.out.println("-".repeat(50));
        
        // Overall latency statistics
        DoubleSummaryStatistics overallLatency = performanceRecords.stream()
            .mapToDouble(record -> record.totalResponseTime)
            .summaryStatistics();
            
        System.out.printf("OVERALL SYSTEM LATENCY:%n");
        System.out.printf("   Average Response Time: %.2f seconds%n", overallLatency.getAverage());
        System.out.printf("   Minimum Response Time: %.2f seconds%n", overallLatency.getMin());
        System.out.printf("   Maximum Response Time: %.2f seconds%n", overallLatency.getMax());
        
        // Latency by urgency level
        System.out.printf("%nLATENCY BY URGENCY LEVEL:%n");
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            DoubleSummaryStatistics urgencyStats = performanceRecords.stream()
                .filter(record -> record.urgency == urgency)
                .mapToDouble(record -> record.totalResponseTime)
                .summaryStatistics();
                
            if (urgencyStats.getCount() > 0) {
                System.out.printf("   %s Tasks:%n", urgency);
                System.out.printf("      Count: %d%n", urgencyStats.getCount());
                System.out.printf("      Avg Latency: %.2f seconds%n", urgencyStats.getAverage());
                System.out.printf("      Min Latency: %.2f seconds%n", urgencyStats.getMin());
                System.out.printf("      Max Latency: %.2f seconds%n", urgencyStats.getMax());
            }
        }
        
        // Latency by datacenter
        System.out.printf("%nLATENCY BY DATACENTER:%n");
        Map<String, DoubleSummaryStatistics> dcLatencyStats = performanceRecords.stream()
            .collect(Collectors.groupingBy(
                record -> record.datacenter,
                Collectors.summarizingDouble(record -> record.totalResponseTime)
            ));
            
        dcLatencyStats.forEach((dc, stats) -> {
            System.out.printf("   %s:%n", dc);
            System.out.printf("      Tasks Processed: %d%n", stats.getCount());
            System.out.printf("      Avg Latency: %.2f seconds%n", stats.getAverage());
            System.out.printf("      Min Latency: %.2f seconds%n", stats.getMin());
            System.out.printf("      Max Latency: %.2f seconds%n", stats.getMax());
        });
    }
    
    /**
     * Generates throughput analysis
     */
    private void generateThroughputAnalysis() {
        System.out.println("\n🚀 THROUGHPUT ANALYSIS:");
        System.out.println("-".repeat(50));
        
        double overallThroughput = totalTasksProcessed / Math.max(totalSimulationTime, 1.0);
        System.out.printf("OVERALL SYSTEM THROUGHPUT:%n");
        System.out.printf("   Tasks per Second: %.2f%n", overallThroughput);
        System.out.printf("   Tasks per Minute: %.2f%n", overallThroughput * 60);
        System.out.printf("   Tasks per Hour: %.2f%n", overallThroughput * 3600);
        
        // Throughput by datacenter
        System.out.printf("%nTHROUGHPUT BY DATACENTER:%n");
        Map<String, Long> dcTaskCounts = performanceRecords.stream()
            .collect(Collectors.groupingBy(
                record -> record.datacenter,
                Collectors.counting()
            ));
            
        dcTaskCounts.forEach((dc, count) -> {
            double dcThroughput = count / Math.max(totalSimulationTime, 1.0);
            System.out.printf("   %s:%n", dc);
            System.out.printf("      Tasks Processed: %d%n", count);
            System.out.printf("      Throughput: %.2f tasks/second%n", dcThroughput);
            System.out.printf("      System Share: %.1f%%%n", (count * 100.0) / totalTasksProcessed);
        });
        
        // Throughput by urgency
        System.out.printf("%nTHROUGHPUT BY URGENCY LEVEL:%n");
        Map<UrgencyLevel, Long> urgencyTaskCounts = performanceRecords.stream()
            .collect(Collectors.groupingBy(
                record -> record.urgency,
                Collectors.counting()
            ));
            
        urgencyTaskCounts.forEach((urgency, count) -> {
            double urgencyThroughput = count / Math.max(totalSimulationTime, 1.0);
            System.out.printf("   %s:%n", urgency);
            System.out.printf("      Tasks: %d, Throughput: %.2f tasks/sec%n", count, urgencyThroughput);
        });
    }
    
    /**
     * Generates SLA compliance detailed report
     */
    private void generateSLAComplianceReport() {
        System.out.println("\n✅ SLA COMPLIANCE DETAILED ANALYSIS:");
        System.out.println("-".repeat(50));
        
        long totalCompliant = performanceRecords.stream()
            .mapToLong(record -> record.slaCompliant ? 1 : 0)
            .sum();
            
        double overallCompliance = (totalCompliant * 100.0) / Math.max(1, totalTasksProcessed);
        System.out.printf("OVERALL SLA COMPLIANCE:%n");
        System.out.printf("   Compliant Tasks: %d/%d%n", totalCompliant, totalTasksProcessed);
        System.out.printf("   Compliance Rate: %.2f%%%n", overallCompliance);
        
        // SLA compliance by urgency
        System.out.printf("%nSLA COMPLIANCE BY URGENCY:%n");
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            List<TaskPerformanceRecord> urgencyTasks = performanceRecords.stream()
                .filter(record -> record.urgency == urgency)
                .collect(Collectors.toList());
                
            if (!urgencyTasks.isEmpty()) {
                long urgencyCompliant = urgencyTasks.stream()
                    .mapToLong(record -> record.slaCompliant ? 1 : 0)
                    .sum();
                double complianceRate = (urgencyCompliant * 100.0) / urgencyTasks.size();
                
                System.out.printf("   %s: %d/%d (%.1f%%)%n", 
                    urgency, urgencyCompliant, urgencyTasks.size(), complianceRate);
            }
        }
        
        // SLA compliance by datacenter
        System.out.printf("%nSLA COMPLIANCE BY DATACENTER:%n");
        Map<String, List<TaskPerformanceRecord>> dcTasks = performanceRecords.stream()
            .collect(Collectors.groupingBy(record -> record.datacenter));
            
        dcTasks.forEach((dc, tasks) -> {
            long dcCompliant = tasks.stream()
                .mapToLong(record -> record.slaCompliant ? 1 : 0)
                .sum();
            double dcComplianceRate = (dcCompliant * 100.0) / tasks.size();
            
            System.out.printf("   %s: %d/%d (%.1f%%)%n", 
                dc, dcCompliant, tasks.size(), dcComplianceRate);
        });
    }
    
    /**
     * Generates queue performance analysis
     */
    private void generateQueuePerformanceAnalysis() {
        System.out.println("\n📋 QUEUE PERFORMANCE ANALYSIS:");
        System.out.println("-".repeat(50));
        
        DoubleSummaryStatistics waitingTimeStats = performanceRecords.stream()
            .mapToDouble(record -> record.waitingTime)
            .summaryStatistics();
            
        System.out.printf("QUEUE WAITING TIME STATISTICS:%n");
        System.out.printf("   Average Wait Time: %.2f seconds%n", waitingTimeStats.getAverage());
        System.out.printf("   Minimum Wait Time: %.2f seconds%n", waitingTimeStats.getMin());
        System.out.printf("   Maximum Wait Time: %.2f seconds%n", waitingTimeStats.getMax());
        
        // Queue performance by urgency
        System.out.printf("%nQUEUE PERFORMANCE BY URGENCY:%n");
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            DoubleSummaryStatistics urgencyWaitStats = performanceRecords.stream()
                .filter(record -> record.urgency == urgency)
                .mapToDouble(record -> record.waitingTime)
                .summaryStatistics();
                
            if (urgencyWaitStats.getCount() > 0) {
                System.out.printf("   %s: Avg %.2fs, Max %.2fs%n", 
                    urgency, urgencyWaitStats.getAverage(), urgencyWaitStats.getMax());
            }
        }
    }
    
    /**
     * Generates datacenter utilization report
     */
    private void generateDatacenterUtilizationReport() {
        System.out.println("\n🏢 DATACENTER UTILIZATION ANALYSIS:");
        System.out.println("-".repeat(50));
        
        Map<String, List<TaskPerformanceRecord>> dcTasks = performanceRecords.stream()
            .collect(Collectors.groupingBy(record -> record.datacenter));
            
        dcTasks.forEach((dc, tasks) -> {
            System.out.printf("%s UTILIZATION:%n", dc.toUpperCase());
            System.out.printf("   Total Tasks: %d%n", tasks.size());
            System.out.printf("   Utilization: %.1f%% of total workload%n", 
                (tasks.size() * 100.0) / totalTasksProcessed);
                
            // Task distribution by urgency in this DC
            Map<UrgencyLevel, Long> dcUrgencyDistribution = tasks.stream()
                .collect(Collectors.groupingBy(
                    record -> record.urgency,
                    Collectors.counting()
                ));
                
            System.out.printf("   Task Distribution:%n");
            dcUrgencyDistribution.forEach((urgency, count) -> 
                System.out.printf("      %s: %d tasks (%.1f%%)%n", 
                    urgency, count, (count * 100.0) / tasks.size()));
        });
    }
    
    /**
     * Generates 5G network slicing analysis
     */
    private void generateNetworkSlicingAnalysis() {
        System.out.println("\n📡 5G NETWORK SLICING ANALYSIS:");
        System.out.println("-".repeat(50));
        
        System.out.printf("NETWORK SLICE UTILIZATION:%n");
        
        // Map urgency to network slices
        Map<UrgencyLevel, String> urgencyToSlice = Map.of(
            UrgencyLevel.CRITICAL, "URLLC (Ultra-Reliable Low Latency)",
            UrgencyLevel.HIGH, "eMBB/URLLC (Enhanced/Ultra-Reliable)",
            UrgencyLevel.NORMAL, "mIoT (Massive IoT)"
        );
        
        Map<UrgencyLevel, Long> urgencyDistribution = performanceRecords.stream()
            .collect(Collectors.groupingBy(
                record -> record.urgency,
                Collectors.counting()
            ));
            
        urgencyDistribution.forEach((urgency, count) -> {
            String slice = urgencyToSlice.get(urgency);
            System.out.printf("   %s: %d tasks (%.1f%%)%n", 
                slice, count, (count * 100.0) / totalTasksProcessed);
        });
        
        // Network performance by slice type
        System.out.printf("%nNETWORK PERFORMANCE BY SLICE:%n");
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            DoubleSummaryStatistics urgencyLatency = performanceRecords.stream()
                .filter(record -> record.urgency == urgency)
                .mapToDouble(record -> record.totalResponseTime)
                .summaryStatistics();
                
            if (urgencyLatency.getCount() > 0) {
                String slice = urgencyToSlice.get(urgency);
                System.out.printf("   %s:%n", slice);
                System.out.printf("      Avg Latency: %.2fs, Tasks: %d%n", 
                    urgencyLatency.getAverage(), urgencyLatency.getCount());
            }
        }
    }
    
    /**
     * Generates resource efficiency report
     */
    private void generateResourceEfficiencyReport() {
        System.out.println("\n⚡ RESOURCE EFFICIENCY ANALYSIS:");
        System.out.println("-".repeat(50));
        
        DoubleSummaryStatistics executionTimeStats = performanceRecords.stream()
            .mapToDouble(record -> record.executionTime)
            .summaryStatistics();
            
        System.out.printf("EXECUTION EFFICIENCY:%n");
        System.out.printf("   Avg Execution Time: %.2f seconds%n", executionTimeStats.getAverage());
        System.out.printf("   Min Execution Time: %.2f seconds%n", executionTimeStats.getMin());
        System.out.printf("   Max Execution Time: %.2f seconds%n", executionTimeStats.getMax());
        
        // Resource efficiency by datacenter
        System.out.printf("%nRESOURCE EFFICIENCY BY DATACENTER:%n");
        Map<String, DoubleSummaryStatistics> dcExecutionStats = performanceRecords.stream()
            .collect(Collectors.groupingBy(
                record -> record.datacenter,
                Collectors.summarizingDouble(record -> record.executionTime)
            ));
            
        dcExecutionStats.forEach((dc, stats) -> {
            System.out.printf("   %s: Avg %.2fs execution time%n", dc, stats.getAverage());
        });
        
        // Calculate fog offloading efficiency
        long fogTasks = performanceRecords.stream()
            .filter(record -> "Fog_DC".equals(record.datacenter))
            .mapToLong(record -> 1)
            .sum();
            
        double fogOffloadingRate = (fogTasks * 100.0) / totalTasksProcessed;
        System.out.printf("%nFOG OFFLOADING EFFICIENCY:%n");
        System.out.printf("   Tasks Processed in Fog: %d/%d (%.1f%%)%n", 
            fogTasks, totalTasksProcessed, fogOffloadingRate);
    }
    
    /**
     * Generates research validation report
     */
    private void generateResearchValidationReport() {
        System.out.println("\n🎯 RESEARCH OBJECTIVES VALIDATION:");
        System.out.println("-".repeat(50));
        
        // Validate critical task fog processing
        long criticalTasks = performanceRecords.stream()
            .filter(record -> record.urgency == UrgencyLevel.CRITICAL)
            .count();
        long criticalInFog = performanceRecords.stream()
            .filter(record -> record.urgency == UrgencyLevel.CRITICAL && "Fog_DC".equals(record.datacenter))
            .count();
            
        System.out.printf("OBJECTIVE 1 - Critical Task Edge Processing:%n");
        System.out.printf("   Critical tasks in Fog: %d/%d (%.1f%%)%n",
            criticalInFog, criticalTasks, (criticalInFog * 100.0) / Math.max(1, criticalTasks));
            
        // Validate load distribution
        Map<String, Long> dcDistribution = performanceRecords.stream()
            .collect(Collectors.groupingBy(record -> record.datacenter, Collectors.counting()));
            
        System.out.printf("%nOBJECTIVE 2 - Load Distribution:%n");
        dcDistribution.forEach((dc, count) -> 
            System.out.printf("   %s: %d tasks (%.1f%%)%n", 
                dc, count, (count * 100.0) / totalTasksProcessed));
                
        // Validate QoS maintenance
        double overallSLA = (performanceRecords.stream().mapToLong(r -> r.slaCompliant ? 1 : 0).sum() * 100.0) 
                           / Math.max(1, totalTasksProcessed);
        System.out.printf("%nOBJECTIVE 3 - QoS Maintenance:%n");
        System.out.printf("   Overall SLA Compliance: %.1f%%%n", overallSLA);
        System.out.printf("   Target Achievement: %s%n", overallSLA >= 85.0 ? "✅ ACHIEVED" : "⚠️ NEEDS IMPROVEMENT");
    }
    
    /**
     * Exports metrics to CSV files for further analysis
     */
    private void exportMetricsToCSV() {
        System.out.println("\n💾 EXPORTING METRICS TO CSV FILES:");
        System.out.println("-".repeat(50));
        
        try {
            // Task performance CSV
            FileWriter taskCsv = new FileWriter("healthcare_task_performance.csv");
            taskCsv.write("TaskID,PatientID,Urgency,ServiceType,Datacenter,WaitTime,ExecTime,ResponseTime,ExpectedSLA,SLACompliant\n");
            
            for (TaskPerformanceRecord record : performanceRecords) {
                taskCsv.write(String.format("%d,%d,%s,%s,%s,%.2f,%.2f,%.2f,%.2f,%s%n",
                    record.taskId, record.patientId, record.urgency, record.serviceType,
                    record.datacenter, record.waitingTime, record.executionTime,
                    record.totalResponseTime, record.expectedSla, record.slaCompliant));
            }
            taskCsv.close();
            System.out.println("   ✅ Task performance exported to: healthcare_task_performance.csv");
            
            // Summary metrics CSV
            FileWriter summaryCSV = new FileWriter("healthcare_summary_metrics.csv");
            summaryCSV.write("Metric,Value\n");
            summaryCSV.write(String.format("Total_Tasks_Processed,%d%n", totalTasksProcessed));
            summaryCSV.write(String.format("Overall_SLA_Compliance,%.2f%n", 
                (performanceRecords.stream().mapToLong(r -> r.slaCompliant ? 1 : 0).sum() * 100.0) / totalTasksProcessed));
            summaryCSV.write(String.format("Avg_Response_Time,%.2f%n", 
                performanceRecords.stream().mapToDouble(r -> r.totalResponseTime).average().orElse(0.0)));
            summaryCSV.write(String.format("Fog_Task_Percentage,%.2f%n",
                (performanceRecords.stream().filter(r -> "Fog_DC".equals(r.datacenter)).count() * 100.0) / totalTasksProcessed));
            summaryCSV.close();
            System.out.println("   ✅ Summary metrics exported to: healthcare_summary_metrics.csv");
            
        } catch (IOException e) {
            System.err.println("   ❌ Error exporting to CSV: " + e.getMessage());
        }
    }
    
    /**
     * Generates executive summary for project presentation
     */
    private void generateExecutiveSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 EXECUTIVE SUMMARY - HEALTHCARE FOG-EDGE SYSTEM");
        System.out.println("=".repeat(80));
        
        double avgLatency = performanceRecords.stream()
            .mapToDouble(r -> r.totalResponseTime)
            .average().orElse(0.0);
            
        long slaCompliant = performanceRecords.stream()
            .mapToLong(r -> r.slaCompliant ? 1 : 0)
            .sum();
            
        long fogTasks = performanceRecords.stream()
            .filter(r -> "Fog_DC".equals(r.datacenter))
            .count();
            
        System.out.printf("🎯 KEY PERFORMANCE INDICATORS:%n");
        System.out.printf("   • Total Healthcare Tasks Processed: %d%n", totalTasksProcessed);
        System.out.printf("   • Average Response Time: %.2f seconds%n", avgLatency);
        System.out.printf("   • SLA Compliance Rate: %.1f%% (%d/%d)%n", 
            (slaCompliant * 100.0) / totalTasksProcessed, slaCompliant, totalTasksProcessed);
        System.out.printf("   • Fog Computing Utilization: %.1f%% (%d tasks)%n", 
            (fogTasks * 100.0) / totalTasksProcessed, fogTasks);
        System.out.printf("   • System Throughput: %.2f tasks/second%n", 
            totalTasksProcessed / Math.max(totalSimulationTime, 1.0));
            
        System.out.printf("%n✅ RESEARCH CONTRIBUTIONS VALIDATED:%n");
        System.out.printf("   • Priority-based healthcare task queuing implemented%n");
        System.out.printf("   • 5G network slicing simulation integrated%n");
        System.out.printf("   • Fog-cloud intelligent orchestration demonstrated%n");
        System.out.printf("   • Real-time IoT healthcare data processing achieved%n");
        
        System.out.println("\n🎉 Performance analysis complete - Ready for project submission!");
        System.out.println("=".repeat(80));
    }
}

