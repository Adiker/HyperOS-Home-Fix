#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Check the actual packaged legacy Xposed module, without a device."""
import re
import subprocess
import sys
import zipfile
from verify_launcher import declarations


def main():
    if len(sys.argv) != 3:
        raise SystemExit("Usage: python3 tools/verify_module.py module.apk /path/to/aapt")
    apk, aapt = sys.argv[1:]
    xml = subprocess.check_output([aapt, "dump", "xmltree", apk, "AndroidManifest.xml"], text=True)
    badging = subprocess.check_output([aapt, "dump", "badging", apk], text=True)
    resources = subprocess.check_output([aapt, "dump", "--values", "resources", apk], text=True)
    assert "name='io.github.adiker.hyperoshomefix'" in badging, "Wrong module package"
    assert "uses-permission" not in badging, "Unexpected Android permissions"
    assert xml.count("E: activity ") == 1, "Unexpected activity exposure"
    assert "io.github.adiker.hyperoshomefix.MainActivity" in xml or '".MainActivity"' in xml
    for name, expected in [("xposedmodule", "0xffffffff"), ("xposedminversion", "0x5d"),
                           ("xposedsharedprefs", "0xffffffff")]:
        match = re.search(r'"' + name + r'"[^\n]*\n[^\n]*android:value[^\n]*' + expected, xml)
        assert match, f"Missing/wrong legacy metadata: {name}"
    assert '"xposedscope"' in xml, "Missing recommended framework scope"
    scope = re.search(r"^\s*resource .*?:array/xposed_scope:.*?(?=\n\s*(?:spec )?resource |\Z)", resources, re.S | re.M)
    assert scope and 'Count=1' in scope[0] and 'com.mi.android.globallauncher' in scope[0], "Scope must contain only POCO Launcher"
    with zipfile.ZipFile(apk) as archive:
        entry = archive.read("assets/xposed_init").decode().strip()
        assert entry == "io.github.adiker.hyperoshomefix.HomeFixModule", "Wrong Xposed entry point"
        all_defined = set()
        for name in archive.namelist():
            assert not name.endswith((".apk", ".jks", ".keystore")), "Unexpected nested APK or signing material"
            if name.startswith("classes") and name.endswith(".dex"):
                defined = declarations(archive.read(name))
                all_defined.update(defined)
                assert all(item.startswith("Lio/github/adiker/hyperoshomefix/") for item in defined), "Bundled framework/vendor implementation"
        assert any("HomeFixModule;->handleLoadPackage(" in item for item in all_defined), "Missing compiled module entry"
    print("PASS: package, zero permissions, one settings activity, legacy metadata/scope/entry, module-only DEX")


if __name__ == "__main__":
    main()
