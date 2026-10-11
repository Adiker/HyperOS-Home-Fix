/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

/** Modern launcher animation requires both UI and surface completion, without cancellation. */
final class GestureCompletion {
    boolean animationEnded;
    boolean surfaceEnded;
    boolean cancelled;
    private boolean delivered;
    void cancel() { cancelled = true; }
    boolean takeCompleted() {
        if (cancelled || delivered || !animationEnded || !surfaceEnded) return false;
        delivered = true;
        return true;
    }
}
