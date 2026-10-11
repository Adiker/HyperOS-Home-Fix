/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;
import org.junit.Test;
import static org.junit.Assert.*;

public class GestureCompletionTest {
    @Test public void requiresBothNativeCompletions() {
        GestureCompletion state = new GestureCompletion();
        state.surfaceEnded = true;
        assertFalse(state.takeCompleted());
        state.animationEnded = true;
        assertTrue(state.takeCompleted());
        assertFalse(state.takeCompleted());
    }
    @Test public void oppositeCompletionOrderIsSupported() {
        GestureCompletion state = new GestureCompletion();
        state.animationEnded = true;
        assertFalse(state.takeCompleted());
        state.surfaceEnded = true;
        assertTrue(state.takeCompleted());
    }
    @Test public void cancellationCallingBothNativeCallbacksCannotTriggerHome() {
        GestureCompletion state = new GestureCompletion();
        state.cancelled = true;
        state.animationEnded = true;
        state.surfaceEnded = true;
        assertFalse(state.takeCompleted());
    }
    @Test public void activityInvalidationBetweenCallbacksCannotReviveHome() {
        GestureCompletion state = new GestureCompletion();
        state.animationEnded = true;
        state.cancel();
        state.surfaceEnded = true;
        assertFalse(state.takeCompleted());
    }
}
