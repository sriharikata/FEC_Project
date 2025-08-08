package org.cloudbus.cloudsim.fec_healthsim.utils;

import org.cloudbus.cloudsim.fec_healthsim.data.*;
import org.cloudbus.cloudsim.fec_healthsim.core.TaskPerformanceRecord;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PerformanceAnalyzer - Advanced analytical tools for healthcare system evaluation
 * Provides detailed statistical analysis and visualization-ready data exports
 */
public class PerformanceAnalyzer {
    
    private final List<TaskPerformanceRecord> performanceData;
    private final Map<String, Object> analysisResults;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    public PerformanceAnalyzer(List<TaskPerformanceRecord> performanceData) {
        this.performanceData = new ArrayList<>(performanceData);
        this.analysisResults = new HashMap<>();
    }
    
    /**
     * Performs comprehensive statistical analysis
     */
    public void performComprehensiveAnalysis() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔬 ADVANCED PERFORMANCE ANALYSIS");
        System.out.println("=".repeat(80));
        
        performLatencyStatisticalAnalysis();
        performThroughputAnalysis();
        performResourceUtilizationAnalysis();
        performScalabilityAnalysis();
        performQualityOfServiceAnalysis();
        performFogCloudComparisonAnalysis();
        generateVisualizationData();
        //generateDetailedReportHTML();
        
        System.out.println("\n✅ Advanced analysis complete!");
    }
    
    /**
     * Performs detailed latency statistical analysis
     */
    private void performLatencyStatisticalAnalysis() {
        System.out.println("\n📊 LATENCY STATISTICAL ANALYSIS:");
        System.out.println("-".repeat(60));
        
        // Calculate percentiles for response time
        List<Double> responseTimes = performanceData.stream()
            .mapToDouble(record -> record.totalResponseTime)
            .sorted()
            .boxed()
            .collect(Collectors.toList());
            
        if (!responseTimes.isEmpty()) {
            double p50 = calculatePercentile(responseTimes, 50);
            double p90 = calculatePercentile(responseTimes, 90);
            double p95 = calculatePercentile(responseTimes, 95);
            double p99 = calculatePercentile(responseTimes, 99);
            
            System.out.printf("RESPONSE TIME PERCENTILES:%n");
            System.out.printf("   P50 (Median): %.2f seconds%n", p50);
            System.out.printf("   P90: %.2f seconds%n", p90);
            System.out.printf("   P95: %.2f seconds%n", p95);
            System.out.printf("   P99: %.2f seconds%n", p99);
            
            // Store results for export
            analysisResults.put("latency_p50", p50);
            analysisResults.put("latency_p90", p90);
            analysisResults.put("latency_p95", p95);
            analysisResults.put("latency_p99", p99);
        }
        
        // Latency analysis by urgency with detailed statistics
        System.out.printf("%nLATENCY DISTRIBUTION BY URGENCY:%n");
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            List<Double> urgencyLatencies = performanceData.stream()
                .filter(record -> record.urgency == urgency)
                .mapToDouble(record -> record.totalResponseTime)
                .boxed()
                .collect(Collectors.toList());
                
            if (!urgencyLatencies.isEmpty()) {
                Collections.sort(urgencyLatencies);
                double mean = urgencyLatencies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double stdDev = calculateStandardDeviation(urgencyLatencies, mean);
                double median = calculatePercentile(urgencyLatencies, 50);
                
                System.out.printf("   %s (%d tasks):%n", urgency, urgencyLatencies.size());
                System.out.printf("      Mean: %.2fs, Median: %.2fs, StdDev: %.2fs%n", mean, median, stdDev);
                System.out.printf("      Min: %.2fs, Max: %.2fs%n", 
                    urgencyLatencies.get(0), urgencyLatencies.get(urgencyLatencies.size()-1));
                
                // Store detailed urgency stats
                analysisResults.put(urgency + "_mean_latency", mean);
                analysisResults.put(urgency + "_median_latency", median);
                analysisResults.put(urgency + "_stddev_latency", stdDev);
            }
        }
    }
    
    /**
     * Performs detailed throughput analysis
     */
    private void performThroughputAnalysis() {
        System.out.println("\n🚀 THROUGHPUT ANALYSIS:");
        System.out.println("-".repeat(60));
        
        // Calculate total simulation time
        double maxFinishTime = performanceData.stream()
            .mapToDouble(record -> record.totalResponseTime)
            .max().orElse(1.0);
            
        int totalTasks = performanceData.size();
        double overallThroughput = totalTasks / maxFinishTime;
        
        System.out.printf("SYSTEM THROUGHPUT METRICS:%n");
        System.out.printf("   Total Tasks: %d%n", totalTasks);
        System.out.printf("   Simulation Time: %.2f seconds%n", maxFinishTime);
        System.out.printf("   Overall Throughput: %.2f tasks/second%n", overallThroughput);
        System.out.printf("   Peak Capacity: %.0f tasks/hour%n", overallThroughput * 3600);
        
        // Throughput by datacenter with efficiency metrics
        System.out.printf("%nDATACENTER THROUGHPUT ANALYSIS:%n");
        Map<String, List<TaskPerformanceRecord>> dcGroups = performanceData.stream()
        	    .collect(Collectors.groupingBy(record -> (String) record.datacenter));

            
        dcGroups.forEach((datacenter, tasks) -> {
            double dcThroughput = tasks.size() / maxFinishTime;
            double avgExecTime = tasks.stream().mapToDouble(t -> t.executionTime).average().orElse(0.0);
            
            System.out.printf("   %s:%n", datacenter);
            System.out.printf("      Tasks: %d, Throughput: %.2f tasks/sec%n", tasks.size(), dcThroughput);
            System.out.printf("      Avg Execution Time: %.2fs%n", avgExecTime);
            System.out.printf("      Efficiency Ratio: %.2f%n", dcThroughput / Math.max(avgExecTime, 0.01));
            
            analysisResults.put(datacenter + "_throughput", dcThroughput);
            analysisResults.put(datacenter + "_avg_exec_time", avgExecTime);
        });
        
        // Peak hours analysis (simulated time windows)
        analyzeTimeWindowPerformance(maxFinishTime);
    }
    
    /**
     * Analyzes performance across different time windows
     */
    private void analyzeTimeWindowPerformance(double maxTime) {
        System.out.printf("%nTIME WINDOW PERFORMANCE ANALYSIS:%n");
        
        int windowCount = Math.min(5, (int)(maxTime / 10) + 1); // 10-second windows
        double windowSize = maxTime / windowCount;
        
        for (int i = 0; i < windowCount; i++) {
            double windowStart = i * windowSize;
            double windowEnd = (i + 1) * windowSize;
            
            List<TaskPerformanceRecord> windowTasks = performanceData.stream()
                .filter(record -> {
                    double taskTime = record.totalResponseTime - record.executionTime; // Approximate start time
                    return taskTime >= windowStart && taskTime < windowEnd;
                })
                .collect(Collectors.toList());
                
            if (!windowTasks.isEmpty()) {
                double windowThroughput = windowTasks.size() / windowSize;
                double avgLatency = windowTasks.stream()
                    .mapToDouble(t -> t.totalResponseTime)
                    .average().orElse(0.0);
                    
                System.out.printf("   Window %d (%.1f-%.1fs): %d tasks, %.2f tasks/sec, %.2fs avg latency%n",
                    i+1, windowStart, windowEnd, windowTasks.size(), windowThroughput, avgLatency);
            }
        }
    }
    
    /**
     * Performs resource utilization analysis
     */
    private void performResourceUtilizationAnalysis() {
        System.out.println("\n⚡ RESOURCE UTILIZATION ANALYSIS:");
        System.out.println("-".repeat(60));
        
        // Datacenter load distribution
        Map<String, Long> dcTaskCounts = performanceData.stream()
            .collect(Collectors.groupingBy(record -> record.datacenter, Collectors.counting()));
            
        long totalTasks = performanceData.size();
        System.out.printf("LOAD DISTRIBUTION:%n");
        
        dcTaskCounts.forEach((dc, count) -> {
            double utilization = (count * 100.0) / totalTasks;
            System.out.printf("   %s: %d tasks (%.1f%% utilization)%n", dc, count, utilization);
            analysisResults.put(dc + "_utilization", utilization);
        });
        
        // Resource efficiency by urgency level
        System.out.printf("%nRESOURCE EFFICIENCY BY URGENCY:%n");
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            List<TaskPerformanceRecord> urgencyTasks = performanceData.stream()
                .filter(record -> record.urgency == urgency)
                .collect(Collectors.toList());
                
            if (!urgencyTasks.isEmpty()) {
                double avgExecution = urgencyTasks.stream()
                    .mapToDouble(t -> t.executionTime)
                    .average().orElse(0.0);
                double avgWaiting = urgencyTasks.stream()
                    .mapToDouble(t -> t.waitingTime)
                    .average().orElse(0.0);
                    
                double efficiency = avgExecution / Math.max(avgExecution + avgWaiting, 0.01);
                
                System.out.printf("   %s: %.1f%% efficiency (%.2fs exec / %.2fs total)%n",
                    urgency, efficiency * 100, avgExecution, avgExecution + avgWaiting);
                    
                analysisResults.put(urgency + "_efficiency", efficiency);
            }
        }
        
        // System-wide resource metrics
        calculateSystemResourceMetrics();
    }
    
    /**
     * Calculates system-wide resource metrics
     */
    private void calculateSystemResourceMetrics() {
        double totalExecTime = performanceData.stream().mapToDouble(t -> t.executionTime).sum();
        double totalWaitTime = performanceData.stream().mapToDouble(t -> t.waitingTime).sum();
        double totalResponseTime = performanceData.stream().mapToDouble(t -> t.totalResponseTime).sum();
        
        double systemEfficiency = totalExecTime / Math.max(totalResponseTime, 0.01);
        double avgResourceUtilization = totalExecTime / (totalExecTime + totalWaitTime);
        
        System.out.printf("%nSYSTEM RESOURCE METRICS:%n");
        System.out.printf("   Overall System Efficiency: %.1f%%%n", systemEfficiency * 100);
        System.out.printf("   Average Resource Utilization: %.1f%%%n", avgResourceUtilization * 100);
        System.out.printf("   Total Compute Time: %.2f seconds%n", totalExecTime);
        System.out.printf("   Total Wait Time: %.2f seconds%n", totalWaitTime);
        
        analysisResults.put("system_efficiency", systemEfficiency);
        analysisResults.put("avg_resource_utilization", avgResourceUtilization);
    }
    
    /**
     * Performs scalability analysis
     */
    private void performScalabilityAnalysis() {
        System.out.println("\n📈 SCALABILITY ANALYSIS:");
        System.out.println("-".repeat(60));
        
        // Analyze performance trends across different load levels
        List<TaskPerformanceRecord> sortedTasks = performanceData.stream()
            .sorted(Comparator.comparing(t -> t.totalResponseTime - t.executionTime))
            .collect(Collectors.toList());
            
        int quarterSize = sortedTasks.size() / 4;
        
        for (int quarter = 0; quarter < 4; quarter++) {
            int startIdx = quarter * quarterSize;
            int endIdx = (quarter == 3) ? sortedTasks.size() : (quarter + 1) * quarterSize;
            
            List<TaskPerformanceRecord> quarterTasks = sortedTasks.subList(startIdx, endIdx);
            
            double avgLatency = quarterTasks.stream()
                .mapToDouble(t -> t.totalResponseTime)
                .average().orElse(0.0);
            double avgWaiting = quarterTasks.stream()
                .mapToDouble(t -> t.waitingTime)
                .average().orElse(0.0);
            long slaCompliant = quarterTasks.stream()
                .mapToLong(t -> t.slaCompliant ? 1 : 0)
                .sum();
                
            System.out.printf("   Load Quarter %d (%d tasks):%n", quarter + 1, quarterTasks.size());
            System.out.printf("      Avg Latency: %.2fs, Avg Wait: %.2fs%n", avgLatency, avgWaiting);
            System.out.printf("      SLA Compliance: %.1f%%%n", (slaCompliant * 100.0) / quarterTasks.size());
        }
        
        // Calculate scalability metrics
        calculateScalabilityMetrics(sortedTasks, quarterSize);
    }
    
    /**
     * Calculates detailed scalability metrics
     */
    private void calculateScalabilityMetrics(List<TaskPerformanceRecord> sortedTasks, int quarterSize) {
        // Compare first quarter vs last quarter performance
        List<TaskPerformanceRecord> firstQuarter = sortedTasks.subList(0, quarterSize);
        List<TaskPerformanceRecord> lastQuarter = sortedTasks.subList(3 * quarterSize, sortedTasks.size());
        
        double firstQuarterLatency = firstQuarter.stream().mapToDouble(t -> t.totalResponseTime).average().orElse(0.0);
        double lastQuarterLatency = lastQuarter.stream().mapToDouble(t -> t.totalResponseTime).average().orElse(0.0);
        
        double scalabilityRatio = lastQuarterLatency / Math.max(firstQuarterLatency, 0.01);
        
        System.out.printf("%nSCALABILITY METRICS:%n");
        System.out.printf("   First Quarter Avg Latency: %.2fs%n", firstQuarterLatency);
        System.out.printf("   Last Quarter Avg Latency: %.2fs%n", lastQuarterLatency);
        System.out.printf("   Scalability Ratio: %.2fx%n", scalabilityRatio);
        System.out.printf("   Scalability Assessment: %s%n", 
            scalabilityRatio <= 1.5 ? "✅ EXCELLENT" : 
            scalabilityRatio <= 2.0 ? "✅ GOOD" : "⚠️ NEEDS IMPROVEMENT");
            
        analysisResults.put("scalability_ratio", scalabilityRatio);
    }
    
    /**
     * Performs Quality of Service analysis
     */
    private void performQualityOfServiceAnalysis() {
        System.out.println("\n🎯 QUALITY OF SERVICE ANALYSIS:");
        System.out.println("-".repeat(60));
        
        // SLA compliance detailed analysis
        long totalCompliant = performanceData.stream().mapToLong(t -> t.slaCompliant ? 1 : 0).sum();
        double overallCompliance = (totalCompliant * 100.0) / performanceData.size();
        
        System.out.printf("SLA COMPLIANCE DETAILED BREAKDOWN:%n");
        System.out.printf("   Overall: %.2f%% (%d/%d tasks)%n", overallCompliance, totalCompliant, performanceData.size());
        
        // SLA compliance by urgency and datacenter
        Map<String, Map<UrgencyLevel, List<TaskPerformanceRecord>>> dcUrgencyGroups = performanceData.stream()
            .collect(Collectors.groupingBy(
                t -> t.datacenter,
                Collectors.groupingBy(t -> t.urgency)
            ));
            
        dcUrgencyGroups.forEach((datacenter, urgencyGroups) -> {
            System.out.printf("%n   %s SLA Compliance:%n", datacenter);
            urgencyGroups.forEach((urgency, tasks) -> {
                long compliant = tasks.stream().mapToLong(t -> t.slaCompliant ? 1 : 0).sum();
                double complianceRate = (compliant * 100.0) / tasks.size();
                
                System.out.printf("      %s: %.1f%% (%d/%d)%n", 
                    urgency, complianceRate, compliant, tasks.size());
            });
        });
        
        // Quality metrics calculation
        calculateQualityMetrics();
    }
    
    /**
     * Calculates comprehensive quality metrics
     */
    private void calculateQualityMetrics() {
        System.out.printf("%nQUALITY METRICS:%n");
        
        // Availability simulation (based on successful task completion)
        double availability = (performanceData.size() * 100.0) / performanceData.size(); // Simplified
        System.out.printf("   System Availability: %.2f%%%n", availability);
        
        // Reliability (SLA compliance rate)
        long compliantTasks = performanceData.stream().mapToLong(t -> t.slaCompliant ? 1 : 0).sum();
        double reliability = (compliantTasks * 100.0) / performanceData.size();
        System.out.printf("   System Reliability: %.2f%%%n", reliability);
        
        // Performance index (composite metric)
        double avgLatency = performanceData.stream().mapToDouble(t -> t.totalResponseTime).average().orElse(0.0);
        double performanceIndex = (reliability / 100.0) * (1.0 / Math.max(avgLatency, 0.01)) * 100;
        System.out.printf("   Performance Index: %.2f%n", performanceIndex);
        
        analysisResults.put("availability", availability);
        analysisResults.put("reliability", reliability);
        analysisResults.put("performance_index", performanceIndex);
    }
    
    /**
     * Performs fog vs cloud comparison analysis
     */
    private void performFogCloudComparisonAnalysis() {
        System.out.println("\n🔄 FOG VS CLOUD COMPARISON ANALYSIS:");
        System.out.println("-".repeat(60));
        
        Map<String, List<TaskPerformanceRecord>> dcGroups = performanceData.stream()
            .collect(Collectors.groupingBy(t -> t.datacenter));
            
        List<TaskPerformanceRecord> fogTasks = dcGroups.getOrDefault("Fog_DC", new ArrayList<>());
        List<TaskPerformanceRecord> cloudTasks = dcGroups.getOrDefault("Cloud_DC", new ArrayList<>());
        
        if (!fogTasks.isEmpty() && !cloudTasks.isEmpty()) {
            // Latency comparison
            double fogAvgLatency = fogTasks.stream().mapToDouble(t -> t.totalResponseTime).average().orElse(0.0);
            double cloudAvgLatency = cloudTasks.stream().mapToDouble(t -> t.totalResponseTime).average().orElse(0.0);
            
            System.out.printf("LATENCY COMPARISON:%n");
            System.out.printf("   Fog Average: %.2fs%n", fogAvgLatency);
            System.out.printf("   Cloud Average: %.2fs%n", cloudAvgLatency);
            System.out.printf("   Fog Advantage: %.2fx faster%n", cloudAvgLatency / Math.max(fogAvgLatency, 0.01));
            
            // SLA compliance comparison
            long fogCompliant = fogTasks.stream().mapToLong(t -> t.slaCompliant ? 1 : 0).sum();
            long cloudCompliant = cloudTasks.stream().mapToLong(t -> t.slaCompliant ? 1 : 0).sum();
            
            double fogSLA = (fogCompliant * 100.0) / fogTasks.size();
            double cloudSLA = (cloudCompliant * 100.0) / cloudTasks.size();
            
            System.out.printf("%nSLA COMPLIANCE COMPARISON:%n");
            System.out.printf("   Fog SLA Rate: %.1f%% (%d/%d)%n", fogSLA, fogCompliant, fogTasks.size());
            System.out.printf("   Cloud SLA Rate: %.1f%% (%d/%d)%n", cloudSLA, cloudCompliant, cloudTasks.size());
            
            // Resource efficiency comparison
            double fogEfficiency = fogTasks.stream().mapToDouble(t -> t.executionTime).average().orElse(0.0);
            double cloudEfficiency = cloudTasks.stream().mapToDouble(t -> t.executionTime).average().orElse(0.0);
            
            System.out.printf("%nRESOURCE EFFICIENCY COMPARISON:%n");
            System.out.printf("   Fog Avg Execution: %.2fs%n", fogEfficiency);
            System.out.printf("   Cloud Avg Execution: %.2fs%n", cloudEfficiency);
            
            // Store comparison results
            analysisResults.put("fog_latency_advantage", cloudAvgLatency / Math.max(fogAvgLatency, 0.01));
            analysisResults.put("fog_sla_rate", fogSLA);
            analysisResults.put("cloud_sla_rate", cloudSLA);
        }
    }
    
    /**
     * Generates visualization-ready data exports
     */
    private void generateVisualizationData() {
        System.out.println("\n📊 GENERATING VISUALIZATION DATA:");
        System.out.println("-".repeat(60));
        
        try {
            // Latency distribution data
            exportLatencyDistribution();
            
            // Throughput over time data
            exportThroughputData();
            
            // SLA compliance data
            //exportSLAComplianceData();
            
            // Resource utilization data
            //exportResourceUtilizationData();
            
            System.out.println("   ✅ All visualization data exported successfully!");
            
        } catch (IOException e) {
            System.err.println("   ❌ Error generating visualization data: " + e.getMessage());
        }
    }
    
    /**
     * Exports latency distribution data for visualization
     */
    private void exportLatencyDistribution() throws IOException {
        FileWriter writer = new FileWriter("latency_distribution.csv");
        writer.write("Urgency,Datacenter,ResponseTime,WaitTime,ExecutionTime\n");
        
        for (TaskPerformanceRecord record : performanceData) {
            writer.write(String.format("%s,%s,%.2f,%.2f,%.2f\n",
                record.urgency, record.datacenter, record.totalResponseTime,
                record.waitingTime, record.executionTime));
        }
        writer.close();
        System.out.println("   ✅ Latency distribution exported to: latency_distribution.csv");
    }
    
    /**
     * Exports throughput data for visualization
     */
    private void exportThroughputData() throws IOException {
        FileWriter writer = new FileWriter("throughput_analysis.csv");
        writer.write("TimeWindow,Datacenter,TaskCount,Throughput\n");
        
        double maxTime = performanceData.stream()
            .mapToDouble(r -> r.totalResponseTime)
            .max().orElse(1.0);
            
        int windows = 10;
        double windowSize = maxTime / windows;
        
        for (int i = 0; i < windows; i++) {
            double windowStart = i * windowSize;
            double windowEnd = (i + 1) * windowSize;
            
            Map<String, Long> dcCounts = performanceData.stream()
                .filter(r -> {
                    double taskTime = r.totalResponseTime - r.executionTime;
                    return taskTime >= windowStart && taskTime < windowEnd;
                })
                .collect(Collectors.groupingBy(r -> r.datacenter, Collectors.counting()));
                
            dcCounts.forEach((dc, count) -> {
                try {
                    writer.write(String.format("%.1f-%.1f,%s,%d,%.2f\n",
                        windowStart, windowEnd, dc, count, count / windowSize));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        writer.close();
        System.out.println("   ✅ Throughput analysis exported to: throughput_analysis.csv");
    }
    

    /**
     * Generates performance recommendations based on analysis results
     */
    private List<String> generatePerformanceRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        // Analyze overall system performance
        double avgLatency = performanceData.stream().mapToDouble(r -> r.totalResponseTime).average().orElse(0.0);
        long compliant = performanceData.stream().mapToLong(r -> r.slaCompliant ? 1 : 0).sum();
        double complianceRate = (compliant * 100.0) / performanceData.size();
        
        // Latency-based recommendations
        if (avgLatency > 5.0) {
            recommendations.add("⚡ <strong>High Priority:</strong> Average response time exceeds 5 seconds. Consider implementing edge caching and optimizing task scheduling algorithms.");
        } else if (avgLatency > 2.0) {
            recommendations.add("⏱️ <strong>Medium Priority:</strong> Response time can be improved through load balancing optimization and resource allocation tuning.");
        }
        
        // SLA compliance recommendations
        if (complianceRate < 80) {
            recommendations.add("🎯 <strong>Critical:</strong> SLA compliance below 80%. Implement priority-based task scheduling and increase resource capacity for critical tasks.");
        } else if (complianceRate < 95) {
            recommendations.add("📊 <strong>Medium Priority:</strong> SLA compliance can be improved through better resource prediction and proactive scaling.");
        }
        
        // Resource utilization recommendations
        Map<String, List<TaskPerformanceRecord>> dcGroups = performanceData.stream()
            .collect(Collectors.groupingBy(r -> r.datacenter));
            
        if (dcGroups.size() > 1) {
            // Check load balancing
            long totalTasks = performanceData.size();
            boolean imbalanced = dcGroups.values().stream()
                .anyMatch(tasks -> Math.abs(tasks.size() - (totalTasks / dcGroups.size())) > totalTasks * 0.2);
                
            if (imbalanced) {
                recommendations.add("⚖️ <strong>Load Balancing:</strong> Uneven task distribution detected. Implement dynamic load balancing to optimize resource utilization across datacenters.");
            }
        }
        
        // Urgency-specific recommendations
        Map<UrgencyLevel, List<TaskPerformanceRecord>> urgencyGroups = performanceData.stream()
            .collect(Collectors.groupingBy(r -> r.urgency));
            
        urgencyGroups.forEach((urgency, tasks) -> {
            double urgencyAvgLatency = tasks.stream().mapToDouble(t -> t.totalResponseTime).average().orElse(0.0);
            long urgencyCompliant = tasks.stream().mapToLong(t -> t.slaCompliant ? 1 : 0).sum();
            double urgencyComplianceRate = (urgencyCompliant * 100.0) / tasks.size();
            
            if (urgency == UrgencyLevel.CRITICAL && urgencyAvgLatency > 1.0) {
                recommendations.add("🚨 <strong>Critical Tasks:</strong> Critical urgency tasks averaging " + String.format("%.2fs", urgencyAvgLatency) + ". Implement dedicated fast-track processing for critical healthcare tasks.");
            }
            
            if (urgency == UrgencyLevel.HIGH && urgencyComplianceRate < 90) {
                recommendations.add("⚡ <strong>High Priority Tasks:</strong> High urgency tasks showing " + String.format("%.1f%%", urgencyComplianceRate) + " compliance. Consider resource reservation for high-priority healthcare operations.");
            }
        });
        
        // System efficiency recommendations
        double totalExecTime = performanceData.stream().mapToDouble(t -> t.executionTime).sum();
        double totalResponseTime = performanceData.stream().mapToDouble(t -> t.totalResponseTime).sum();
        double systemEfficiency = (totalExecTime / Math.max(totalResponseTime, 0.01)) * 100;
        
        if (systemEfficiency < 60) {
            recommendations.add("⚡ <strong>System Efficiency:</strong> Low system efficiency (" + String.format("%.1f%%", systemEfficiency) + "). Investigate network latency, optimize data transfer, and consider edge computing deployment.");
        }
        
        // If no major issues found, provide optimization suggestions
        if (recommendations.isEmpty()) {
            recommendations.add("✅ <strong>System Performance:</strong> Overall system performance is good. Consider implementing predictive analytics for proactive resource scaling.");
            recommendations.add("🔧 <strong>Continuous Improvement:</strong> Monitor peak usage patterns and implement auto-scaling policies for handling traffic spikes.");
            recommendations.add("📈 <strong>Future Optimization:</strong> Consider machine learning-based task scheduling for further performance improvements.");
        }
        
        return recommendations;
    }
    
    // Utility methods
    private double calculatePercentile(List<Double> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
    
    private double calculateStandardDeviation(List<Double> values, double mean) {
        if (values.size() <= 1) return 0.0;
        
        double variance = values.stream()
            .mapToDouble(val -> Math.pow(val - mean, 2))
            .average()
            .orElse(0.0);
            
        return Math.sqrt(variance);
    }
    
    public Map<String, Object> getAnalysisResults() {
        return new HashMap<>(analysisResults);
    }
}