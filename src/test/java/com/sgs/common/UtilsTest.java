package com.sgs.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void safeUrlAcceptsOnlyHttpAndHttps() {
        assertTrue(UrlRules.isSafeHttpUrl(null, 500));
        assertTrue(UrlRules.isSafeHttpUrl("   ", 500));
        assertTrue(UrlRules.isSafeHttpUrl("https://www.instagram.com/super_glazing_system", 500));
        assertTrue(UrlRules.isSafeHttpUrl("http://example.com/a?b=c", 500));
        assertFalse(UrlRules.isSafeHttpUrl("javascript:alert(1)", 500));
        assertFalse(UrlRules.isSafeHttpUrl("JaVaScRiPt:alert(1)", 500));
        assertFalse(UrlRules.isSafeHttpUrl("data:text/html;base64,AAAA", 500));
        assertFalse(UrlRules.isSafeHttpUrl("ftp://example.com", 500));
        assertFalse(UrlRules.isSafeHttpUrl("//example.com", 500));
        assertFalse(UrlRules.isSafeHttpUrl("https://exa mple.com", 500));
        assertFalse(UrlRules.isSafeHttpUrl("https://", 500));
        assertFalse(UrlRules.isSafeHttpUrl("https://example.com/" + "a".repeat(600), 500));
    }

    @Test
    void slugify() {
        assertEquals("high-corporate", Slugs.slugify("High Corporate"));
        assertEquals("aluminum-glass-railing", Slugs.slugify("  Aluminum   Glass Railing!! "));
        assertEquals("cafe", Slugs.slugify("Café"));
        assertEquals("item", Slugs.slugify("!!!"));
        assertEquals("item", Slugs.slugify(null));
    }

    @Test
    void uniqueSlugAddsSuffix() {
        Set<String> taken = new HashSet<>(Set.of("spider-glazing", "spider-glazing-2"));
        assertEquals("spider-glazing-3", Slugs.unique("spider-glazing", taken::contains));
        assertEquals("skylights", Slugs.unique("skylights", taken::contains));
    }

    @Test
    void textUtil() {
        assertEquals("a b", TextUtil.clean("  a b \u0000\u0007 "));
        assertEquals("line1\nline2", TextUtil.clean("line1\nline2"));
        assertEquals(null, TextUtil.blankToNull("   "));
        assertEquals("100\\%\\_x", TextUtil.escapeLike("100%_x"));
    }

    @Test
    void rateLimiterSlidingWindow() {
        AtomicLong now = new AtomicLong(1_000_000);
        RateLimiter rl = new RateLimiter(now::get);
        Duration w = Duration.ofMinutes(10);
        assertTrue(rl.tryAcquire("ip", 3, w));
        assertTrue(rl.tryAcquire("ip", 3, w));
        assertTrue(rl.tryAcquire("ip", 3, w));
        assertFalse(rl.tryAcquire("ip", 3, w));
        assertTrue(rl.tryAcquire("other", 3, w));
        now.addAndGet(Duration.ofMinutes(11).toMillis());
        assertTrue(rl.tryAcquire("ip", 3, w));
    }

    @Test
    void rateLimiterCountAndClear() {
        AtomicLong now = new AtomicLong(5_000);
        RateLimiter rl = new RateLimiter(now::get);
        Duration w = Duration.ofMinutes(15);
        rl.hit("k", w);
        rl.hit("k", w);
        assertEquals(2, rl.count("k", w));
        rl.clear("k");
        assertEquals(0, rl.count("k", w));
        assertEquals(0, rl.count("missing", w));
    }
}
