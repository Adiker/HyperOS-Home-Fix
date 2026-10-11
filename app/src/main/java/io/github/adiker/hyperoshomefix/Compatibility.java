/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

final class Compatibility {
    static final String PACKAGE = "com.mi.android.globallauncher";
    static final String VERSION = "RELEASE-7.50.06.2592-09011644";
    static final long VERSION_CODE = 750062592L;
    static final String SHA256 = "aba8d486b2116aba1e32143e5d2a2598460965bc5b1aba3db60714a432b11d23";

    static boolean supported(int sdk, String device, String version, long code, String digest) {
        return sdk == 36 && "myron".equals(device) && VERSION.equals(version)
                && code == VERSION_CODE && SHA256.equals(digest);
    }

    static String digest(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int length;
            while ((length = input.read(buffer)) != -1) md.update(buffer, 0, length);
        }
        StringBuilder result = new StringBuilder(64);
        for (byte b : md.digest()) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }
}
