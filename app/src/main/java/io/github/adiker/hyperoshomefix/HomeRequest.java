/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

/** One bounded HOME transaction; lifecycle callbacks cannot create a request. */
final class HomeRequest {
    static final long MAX_AGE_MS = 3000;
    private long deadline;
    private boolean pending;
    private boolean ready;
    boolean cleaned;
    boolean settingsRequested;

    void begin(long now, boolean completed) {
        deadline = now + MAX_AGE_MS;
        pending = true;
        ready = completed;
        cleaned = false;
        settingsRequested = false;
    }

    boolean active(long now) {
        if (pending && now >= deadline) cancel();
        return pending;
    }

    void complete(long now) { if (active(now)) ready = true; }
    boolean ready(long now) { return active(now) && ready; }
    void cancel() { pending = false; ready = false; cleaned = false; settingsRequested = false; }
}
