/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;
import java.io.File;
import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompatibilityTest {
    @Test public void exactAnalyzedTargetIsAccepted() {
        assertTrue(Compatibility.supported(36, "myron", Compatibility.VERSION,
                Compatibility.VERSION_CODE, Compatibility.SHA256));
    }
    @Test public void eachUnknownIdentityFailsClosed() {
        assertFalse(Compatibility.supported(35, "myron", Compatibility.VERSION, Compatibility.VERSION_CODE, Compatibility.SHA256));
        assertFalse(Compatibility.supported(36, "other", Compatibility.VERSION, Compatibility.VERSION_CODE, Compatibility.SHA256));
        assertFalse(Compatibility.supported(36, "myron", "new-version", Compatibility.VERSION_CODE, Compatibility.SHA256));
        assertFalse(Compatibility.supported(36, "myron", Compatibility.VERSION, 1, Compatibility.SHA256));
        assertFalse(Compatibility.supported(36, "myron", Compatibility.VERSION, Compatibility.VERSION_CODE, "different-apk"));
    }
    @Test public void nullIdentityFailsClosed() {
        assertFalse(Compatibility.supported(36, null, null, 0, null));
    }
    @Test public void digestUsesActualFileBytes() throws Exception {
        File file = File.createTempFile("homefix", ".tmp");
        try {
            Files.write(file.toPath(), new byte[]{97,98,99});
            assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Compatibility.digest(file));
        } finally { Files.deleteIfExists(file.toPath()); }
    }
}
