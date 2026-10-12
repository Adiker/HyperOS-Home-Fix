/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;
import org.junit.Test;
import static org.junit.Assert.*;

public class HomeIntentPolicyTest {
    @Test public void mainHomeIsAccepted() {
        assertTrue(HomeIntentPolicy.accepts("android.intent.action.MAIN", true, false, false, false));
    }
    @Test public void otherLaunchesAreExcluded() {
        assertFalse(HomeIntentPolicy.accepts("android.intent.action.MAIN", false, false, false, false));
        assertFalse(HomeIntentPolicy.accepts("android.intent.action.SHOW_WORK_APPS", true, false, false, false));
        assertFalse(HomeIntentPolicy.accepts("android.intent.action.VIEW", true, false, false, false));
        assertFalse(HomeIntentPolicy.accepts(null, true, false, false, false));
    }
    @Test public void nativeSpecialIntentsAreExcluded() {
        assertFalse(HomeIntentPolicy.accepts("android.intent.action.MAIN", true, true, false, false));
        assertFalse(HomeIntentPolicy.accepts("android.intent.action.MAIN", true, false, true, false));
    }
    @Test public void nativeHomeToHomeGestureFlagDoesNotRejectHome() {
        assertTrue(HomeIntentPolicy.accepts("android.intent.action.MAIN", true, false, false, true));
    }
    @Test public void nativeFocusFlagCannotTurnOtherIntentsIntoHome() {
        assertFalse(HomeIntentPolicy.accepts("android.intent.action.VIEW", true, false, false, true));
        assertFalse(HomeIntentPolicy.accepts("android.intent.action.MAIN", false, false, false, true));
        assertFalse(HomeIntentPolicy.accepts("android.intent.action.MAIN", true, true, false, true));
        assertFalse(HomeIntentPolicy.accepts("android.intent.action.MAIN", true, false, true, true));
    }
}
