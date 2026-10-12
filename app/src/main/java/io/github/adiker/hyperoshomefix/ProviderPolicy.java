/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

final class ProviderPolicy {
    static boolean mayReadSettings(int caller, int owner, int launcher) {
        return caller == owner || (launcher >= 0 && caller == launcher);
    }
    static boolean mayReadStatus(int caller, int owner) { return caller == owner; }
    static boolean mayReport(int caller, int launcher) { return launcher >= 0 && caller == launcher; }
}
