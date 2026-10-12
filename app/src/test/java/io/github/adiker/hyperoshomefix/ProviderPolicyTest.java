/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProviderPolicyTest {
    @Test public void unrelatedAppsCannotReadSettingsOrForgeStatus() {
        assertFalse(ProviderPolicy.mayReadSettings(55, 10, 20));
        assertFalse(ProviderPolicy.mayReport(55, 20));
        assertFalse(ProviderPolicy.mayReadStatus(55, 10));
    }
    @Test public void launcherMayReadSwitchesAndReportButNotReadStatus() {
        assertTrue(ProviderPolicy.mayReadSettings(20, 10, 20));
        assertTrue(ProviderPolicy.mayReport(20, 20));
        assertFalse(ProviderPolicy.mayReadStatus(20, 10));
    }
    @Test public void settingsAppMayReadOwnSettingsAndStatus() {
        assertTrue(ProviderPolicy.mayReadSettings(10, 10, 20));
        assertTrue(ProviderPolicy.mayReadStatus(10, 10));
        assertFalse(ProviderPolicy.mayReport(10, 20));
    }
    @Test public void missingLauncherDoesNotGrantAccessToNegativeUid() {
        assertFalse(ProviderPolicy.mayReadSettings(-1, 10, -1));
        assertFalse(ProviderPolicy.mayReport(-1, -1));
        assertTrue(ProviderPolicy.mayReadSettings(10, 10, -1));
    }
}
