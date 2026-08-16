package com.eopis.common.chaos;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;

@Aspect
@Component
public class ChaosFaultInjectorAspect {

    private static final Logger log = LoggerFactory.getLogger(ChaosFaultInjectorAspect.class);
    private final ChaosProperties chaosProperties;
    private final Random random = new Random();

    public ChaosFaultInjectorAspect(ChaosProperties chaosProperties) {
        this.chaosProperties = chaosProperties;
    }

    @Around("execution(* com.eopis..service.*.*(..))")
    public Object interceptServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!chaosProperties.isEnabled() || chaosProperties.getFaults() == null) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName().toLowerCase();
        String methodName = joinPoint.getSignature().getName().toLowerCase();

        // Check for specific fault configurations targeted to this service/method
        for (var entry : chaosProperties.getFaults().entrySet()) {
            String faultKey = entry.getKey().toLowerCase();
            ChaosProperties.FaultConfig faultConfig = entry.getValue();

            if (!faultConfig.isEnabled()) {
                continue;
            }

            // Scope the fault to matching service domain
            boolean isTargetMatch = isTargetedMatch(faultKey, className, methodName);
            if (!isTargetMatch) {
                continue;
            }

            // Latency injection
            if (faultConfig.getLatencyMs() != null && faultConfig.getLatencyMs() > 0) {
                try {
                    log.warn("[CHAOS-AOP] Injecting {}ms latency into {}.{} (Fault: {})",
                            faultConfig.getLatencyMs(), joinPoint.getTarget().getClass().getSimpleName(),
                            joinPoint.getSignature().getName(), entry.getKey());
                    Thread.sleep(faultConfig.getLatencyMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Intermittent failure injection
            if (faultConfig.getFailureRatePercent() != null && faultConfig.getFailureRatePercent() > 0) {
                double roll = random.nextDouble() * 100.0;
                if (roll < faultConfig.getFailureRatePercent()) {
                    log.error("[CHAOS-AOP] Injecting simulated failure into {}.{} (Fault: {})",
                            joinPoint.getTarget().getClass().getSimpleName(),
                            joinPoint.getSignature().getName(), entry.getKey());
                    throw new RuntimeException("Simulated Chaos Failure triggered by " + entry.getKey());
                }
            }
        }

        return joinPoint.proceed();
    }

    private boolean isTargetedMatch(String faultKey, String className, String methodName) {
        // If fault is domain-scoped by keyword
        if (faultKey.contains("payment") && !className.contains("payment")) {
            return false;
        }
        if (faultKey.contains("inventory") && !className.contains("inventory")) {
            return false;
        }
        if (faultKey.contains("order") && !className.contains("order")) {
            return false;
        }
        if (faultKey.contains("product") && !className.contains("product")) {
            return false;
        }
        if (faultKey.contains("shipment") && !className.contains("shipment")) {
            return false;
        }
        return true;
    }
}
