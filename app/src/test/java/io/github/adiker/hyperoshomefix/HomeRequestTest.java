/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;
import org.junit.Test;
import static org.junit.Assert.*;

public class HomeRequestTest {
    @Test public void resumeCannotCreateHomeRequest() {
        HomeRequest request = new HomeRequest();
        request.complete(100);
        assertFalse(request.ready(100));
    }
    @Test public void externalHomeWaitsForCompletion() {
        HomeRequest request = new HomeRequest();
        request.begin(100, false);
        assertTrue(request.active(101));
        assertFalse(request.ready(101));
        request.complete(102);
        assertTrue(request.ready(103));
    }
    @Test public void expiredRequestCannotBeRevivedByLaterAnimation() {
        HomeRequest request = new HomeRequest();
        request.begin(100, false);
        request.complete(100 + HomeRequest.MAX_AGE_MS);
        assertFalse(request.ready(100 + HomeRequest.MAX_AGE_MS));
    }
    @Test public void focusLossAndPauseCancelPendingHome() {
        HomeRequest request = new HomeRequest();
        request.begin(100, true);
        request.cleaned = true;
        request.cancel();
        request.complete(101);
        assertFalse(request.ready(101));
        assertFalse(request.cleaned);
    }
    @Test public void newRequestResetsCleanupAndCompletion() {
        HomeRequest request = new HomeRequest();
        request.begin(100, true);
        request.cleaned = true;
        request.begin(101, false);
        assertFalse(request.ready(102));
        assertFalse(request.cleaned);
    }
}
