/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

final class HomeIntentPolicy {
    static boolean accepts(String action, boolean homeCategory, boolean locating,
                           boolean fromFeed, boolean ignoreBringToFront) {
        // POCO's own home-to-home gesture sets ignore_bring_to_front=true.
        // It controls native focus handling; it does not make MAIN/HOME a different action.
        return "android.intent.action.MAIN".equals(action) && homeCategory
                && !locating && !fromFeed;
    }
}
