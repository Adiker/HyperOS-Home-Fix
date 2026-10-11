/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;
import org.junit.Test;
import static org.junit.Assert.*;

public class CancellationEpochTest {
    @Test public void queuedStartBeforePauseCannotRegisterAfterPause() {
        CancellationEpoch epoch = new CancellationEpoch();
        long queued = epoch.snapshot();
        epoch.invalidate();
        assertFalse(epoch.accepts(queued, 100, 101));
    }
    @Test public void genuinelyNewGestureAfterCancellationIsAccepted() {
        CancellationEpoch epoch = new CancellationEpoch();
        epoch.invalidate();
        assertTrue(epoch.accepts(epoch.snapshot(), 100, 101));
    }
    @Test public void stalledStartCannotExtendTransactionLifetime() {
        CancellationEpoch epoch = new CancellationEpoch();
        assertFalse(epoch.accepts(epoch.snapshot(), 100, 100 + HomeRequest.MAX_AGE_MS));
    }
}
