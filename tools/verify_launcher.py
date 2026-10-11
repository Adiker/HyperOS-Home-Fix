#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only exact DEX declaration check. Does not decompile or copy vendor code."""
import hashlib
import json
import struct
import sys
import zipfile
from pathlib import Path


def declarations(data):
    if not data.startswith(b"dex\n"):
        raise ValueError("Unsupported DEX container")
    u32 = lambda offset: struct.unpack_from("<I", data, offset)[0]

    def uleb(offset):
        value = shift = 0
        while True:
            byte = data[offset]
            offset += 1
            value |= (byte & 127) << shift
            if not byte & 128:
                return value, offset
            shift += 7
            if shift > 28:
                raise ValueError("Invalid ULEB128")

    strings = []
    for index in range(u32(56)):
        _, offset = uleb(u32(u32(60) + index * 4))
        strings.append(data[offset:data.index(b"\0", offset)].decode("utf-8", errors="replace"))
    types = [strings[u32(u32(68) + index * 4)] for index in range(u32(64))]
    protos = []
    for index in range(u32(72)):
        offset = u32(76) + index * 12
        parameters = u32(offset + 8)
        args = [] if not parameters else [types[struct.unpack_from("<H", data, parameters + 4 + n * 2)[0]]
                                         for n in range(u32(parameters))]
        protos.append("(" + "".join(args) + ")" + types[u32(offset + 4)])
    methods = []
    for index in range(u32(88)):
        owner, proto, name = struct.unpack_from("<HHI", data, u32(92) + index * 8)
        methods.append(types[owner] + "->" + strings[name] + protos[proto])
    fields = []
    for index in range(u32(80)):
        owner, kind, name = struct.unpack_from("<HHI", data, u32(84) + index * 8)
        fields.append(types[owner] + "->" + strings[name] + ":" + types[kind])
    defined = set()
    for index in range(u32(96)):
        offset = u32(u32(100) + index * 32 + 24)
        if not offset:
            continue
        counts = []
        for _ in range(4):
            count, offset = uleb(offset)
            counts.append(count)
        for group, count in enumerate(counts):
            member = 0
            for _ in range(count):
                difference, offset = uleb(offset)
                member += difference
                _, offset = uleb(offset)  # access flags
                if group >= 2:
                    _, offset = uleb(offset)  # code offset
                defined.add((fields if group < 2 else methods)[member])
    return defined


def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python3 tools/verify_launcher.py /path/to/PocoLauncher.apk")
    apk = Path(sys.argv[1])
    profile = json.loads((Path(__file__).parent / "launcher-profile.json").read_text())
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    if digest != profile["sha256"]:
        raise SystemExit("FAIL: APK digest is outside the analyzed profile")
    defined = set()
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if name.startswith("classes") and name.endswith(".dex"):
                defined.update(declarations(archive.read(name)))
    missing = set(profile["declarations"]) - defined
    if missing:
        raise SystemExit("FAIL: Missing declarations:\n" + "\n".join(sorted(missing)))
    print(f"PASS: exact SHA-256 and {len(profile['declarations'])} DEX declarations")


if __name__ == "__main__":
    main()
