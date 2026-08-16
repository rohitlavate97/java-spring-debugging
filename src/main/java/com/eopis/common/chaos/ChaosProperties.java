package com.eopis.common.chaos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "eopis.chaos")
public class ChaosProperties {

    private boolean enabled = false;
    private Map<String, FaultConfig> faults = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, FaultConfig> getFaults() {
        return faults;
    }

    public void setFaults(Map<String, FaultConfig> faults) {
        this.faults = faults;
    }

    public static class FaultConfig {
        private boolean enabled = false;
        private Long latencyMs;
        private Double failureRatePercent;
        private Map<String, Object> parameters = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Long latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Double getFailureRatePercent() {
            return failureRatePercent;
        }

        public void setFailureRatePercent(Double failureRatePercent) {
            this.failureRatePercent = failureRatePercent;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}
