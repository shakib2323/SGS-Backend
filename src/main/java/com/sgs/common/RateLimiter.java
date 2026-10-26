package com.sgs.common;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Tiny in-memory sliding-window limiter. Fine for a single API instance (our deployment).
 * If you ever run several instances behind a load balancer, move this to Redis/DB.
 */
public class RateLimiter {
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final LongSupplier nowMillis;

    public RateLimiter() {
        this(System::currentTimeMillis);
    }

    public RateLimiter(LongSupplier nowMillis) {
        this.nowMillis = nowMillis;
    }

    /** Records a hit and returns true if it is within {@code max} hits per {@code window}. */
    public boolean tryAcquire(String key, int max, Duration window) {
        Deque<Long> d = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (d) {
            evict(d, window);
            if (d.size() >= max) return false;
            d.addLast(nowMillis.getAsLong());
        }
        maybePurge(window);
        return true;
    }

    public void hit(String key, Duration window) {
        Deque<Long> d = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (d) {
            evict(d, window);
            d.addLast(nowMillis.getAsLong());
        }
        maybePurge(window);
    }

    public int count(String key, Duration window) {
        Deque<Long> d = hits.get(key);
        if (d == null) return 0;
        synchronized (d) {
            evict(d, window);
            return d.size();
        }
    }

    public void clear(String key) {
        hits.remove(key);
    }

    private void evict(Deque<Long> d, Duration window) {
        long cutoff = nowMillis.getAsLong() - window.toMillis();
        while (!d.isEmpty() && d.peekFirst() <= cutoff) d.pollFirst();
    }

    private void maybePurge(Duration window) {
        if (hits.size() < 10_000) return;
        Iterator<Map.Entry<String, Deque<Long>>> it = hits.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<Long>> e = it.next();
            synchronized (e.getValue()) {
                evict(e.getValue(), window);
                if (e.getValue().isEmpty()) it.remove();
            }
        }
    }
}
