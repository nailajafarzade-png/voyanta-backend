package com.voyanta.common.ratelimit;

import java.time.Duration;

public interface RateLimiter {
    /**
     * @return true — sorğuya icazə var (limit aşılmayıb), false — limit dolub
     */
    boolean tryConsume(String key, int limit, Duration window);
}