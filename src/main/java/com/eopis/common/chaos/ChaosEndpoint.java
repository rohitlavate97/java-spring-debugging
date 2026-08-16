package com.eopis.common.chaos;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "chaos")
public class ChaosEndpoint {

    private final ChaosProperties chaosProperties;

    public ChaosEndpoint(ChaosProperties chaosProperties) {
        this.chaosProperties = chaosProperties;
    }

    @ReadOperation
    public Map<String, Object> getChaosStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("chaosEnabled", chaosProperties.isEnabled());
        
        Map<String, Object> activeFaults = new HashMap<>();
        if (chaosProperties.getFaults() != null) {
            chaosProperties.getFaults().forEach((key, config) -> {
                if (config != null && config.isEnabled()) {
                    Map<String, Object> details = new HashMap<>();
                    if (config.getLatencyMs() != null) {
                        details.put("latencyMs", config.getLatencyMs());
                    }
                    if (config.getFailureRatePercent() != null) {
                        details.put("failureRatePercent", config.getFailureRatePercent());
                    }
                    activeFaults.put(key, details);
                }
            });
        }
        
        response.put("activeFaults", activeFaults);
        response.put("totalConfiguredFaults", chaosProperties.getFaults() != null ? chaosProperties.getFaults().size() : 0);
        return response;
    }
}
