package com.eopis.common.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);
    private final StringRedisTemplate redisTemplate;
    
    // In-memory fallback for local testing environments without active Redis container
    private final ConcurrentHashMap<String, String> localLocks = new ConcurrentHashMap<>();

    public DistributedLockService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String acquireLock(String lockKey, Duration leaseTime) {
        String lockValue = UUID.randomUUID().toString();
        
        if (redisTemplate != null) {
            try {
                Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, leaseTime);
                if (Boolean.TRUE.equals(success)) {
                    log.debug("Acquired Redis distributed lock for key: {}", lockKey);
                    return lockValue;
                }
                return null;
            } catch (Exception e) {
                log.warn("Redis unavailable, falling back to in-memory lock: {}", e.getMessage());
            }
        }

        // In-memory fallback
        String existing = localLocks.putIfAbsent(lockKey, lockValue);
        return existing == null ? lockValue : null;
    }

    public boolean releaseLock(String lockKey, String lockValue) {
        if (lockValue == null) {
            return false;
        }

        if (redisTemplate != null) {
            try {
                String currentValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentValue)) {
                    redisTemplate.delete(lockKey);
                    log.debug("Released Redis distributed lock for key: {}", lockKey);
                    return true;
                }
                return false;
            } catch (Exception e) {
                log.warn("Redis unavailable during release: {}", e.getMessage());
            }
        }

        return localLocks.remove(lockKey, lockValue);
    }
}
