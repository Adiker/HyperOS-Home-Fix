/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;
import java.util.concurrent.atomic.AtomicLong;

/** Invalidates queued gesture starts as well as already-registered tokens. */
final class CancellationEpoch {
    private final AtomicLong value = new AtomicLong();
    long snapshot() { return value.get(); }
    void invalidate() { value.incrementAndGet(); }
    boolean accepts(long epoch, long born, long now) {
        return epoch == value.get() && now >= born && now - born < HomeRequest.MAX_AGE_MS;
    }
}
