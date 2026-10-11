/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

final class HomeIntentPolicy {
    static boolean accepts(String action, boolean homeCategory, boolean locating,
                           boolean fromFeed, boolean ignoreBringToFront) {
        return "android.intent.action.MAIN".equals(action) && homeCategory
                && !locating && !fromFeed && !ignoreBringToFront;
    }
}
