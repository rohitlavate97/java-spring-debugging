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

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // Check for specific fault configurations
        chaosProperties.getFaults().forEach((faultKey, faultConfig) -> {
            if (faultConfig.isEnabled()) {
                // Latency injection
                if (faultConfig.getLatencyMs() != null && faultConfig.getLatencyMs() > 0) {
                    try {
                        log.warn("[CHAOS-AOP] Injecting {}ms latency into {}.{} (Fault: {})",
                                faultConfig.getLatencyMs(), className, methodName, faultKey);
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
                                className, methodName, faultKey);
                        throw new RuntimeException("Simulated Chaos Failure triggered by " + faultKey);
                    }
                }
            }
        });

        return joinPoint.proceed();
    }
}
