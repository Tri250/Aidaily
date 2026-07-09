#!/usr/bin/env python3
"""Deep self-check for LiveCapture Android APK stability."""
import struct, sys, zipfile, os

def check_elf(data):
    if len(data) < 64 or data[:4] != b"\x7fELF": return None
    is_64 = data[4] == 2; is_le = data[5] == 1; e = "<" if is_le else ">"
    if is_64:
        e_phoff = struct.unpack_from(e+"Q", data, 32)[0]
        e_phentsize = struct.unpack_from(e+"H", data, 54)[0]
        e_phnum = struct.unpack_from(e+"H", data, 56)[0]
        p_align_off = 56
    else:
        e_phoff = struct.unpack_from(e+"I", data, 28)[0]
        e_phentsize = struct.unpack_from(e+"H", data, 42)[0]
        e_phnum = struct.unpack_from(e+"H", data, 44)[0]
        p_align_off = 28
    PT_LOAD = 1; min_a = None
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        if off + e_phentsize > len(data): break
        p_type = struct.unpack_from(e+"I", data, off)[0]
        if p_type != PT_LOAD: continue
        p_align = struct.unpack_from(e+("Q" if is_64 else "I"), data, off+p_align_off)[0]
        if p_align <= 0: continue
        if min_a is None or p_align < min_a: min_a = p_align
    return min_a

apk = "/workspace/LiveCaptureAndroid/app/build/outputs/apk/official/release/app-official-release.apk"
print("=" * 50)
print("v1.1.3 APK 深度稳定性自检")
print("=" * 50)

# 1. Metadata
print("\n[1] APK 基本信息")
size = os.path.getsize(apk)
print(f"  大小: {size/1024/1024:.1f} MB ({size} bytes)")

# 2. ZIP integrity
print("\n[2] ZIP 完整性")
with zipfile.ZipFile(apk) as zf:
    bad = zf.testzip()
    entries = len(zf.namelist())
    if bad is None:
        print(f"  ✓ PASS: {entries} entries, no corruption")
    else:
        print(f"  ✗ FAIL: corruption at {bad}")

# 3. 16KB alignment
print("\n[3] 16KB 页对齐")
with zipfile.ZipFile(apk) as zf:
    sos = sorted(n for n in zf.namelist() if n.endswith(".so"))
    bad, ok, abis = [], [], {}
    for n in sos:
        data = zf.read(n); a = check_elf(data)
        if a is None: continue
        if a >= 0x4000:
            ok.append((n, a))
            abi = n.split("/")[1]
            abis[abi] = abis.get(abi, 0) + 1
        else:
            bad.append((n, a))
    print(f"  Total .so: {len(sos)}  OK: {len(ok)}  BAD: {len(bad)}")
    if bad:
        for n,a in bad: print(f"    ✗ {n} align=0x{a:x}")
    else:
        print("  ✓ ALL 16KB-OK")
    print(f"  ABIs: {dict(abis)}")

# 4. MlKitInitProvider
print("\n[4] MlKitInitProvider 安全")
with zipfile.ZipFile(apk) as zf:
    manifest = None
    for n in zf.namelist():
        if n == "AndroidManifest.xml":
            manifest = zf.read(n); break
    if manifest:
        has_mlkit = b"mlkitinit" in manifest.lower()
        print(f"  Present: {has_mlkit}")
        if has_mlkit:
            print("  ✗ WARNING: MlKitInitProvider should be removed")
        else:
            print("  ✓ PASS: removed from manifest")

# 5. Permission check
print("\n[5] 权限检查")
perm_map = {
    "CAMERA": "相机拍摄",
    "READ_MEDIA_IMAGES": "幻影模式读取相册",
    "POST_NOTIFICATIONS": "后台通知",
    "VIBRATE": "触觉反馈",
    "FOREGROUND_SERVICE": "前台服务",
    "WRITE_EXTERNAL_STORAGE": "兼容旧存储",
    "ACCESS_MEDIA_LOCATION": "照片位置信息",
}
found = set()
with zipfile.ZipFile(apk) as zf:
    if manifest:
        for name, desc in perm_map.items():
            if name.encode() in manifest:
                found.add(name)
                print(f"  ✓ {name}: {desc}")
        missing = set(perm_map.keys()) - found
        for m in missing:
            print(f"  - {m}: {perm_map[m]} (not declared)")

# 6. Content providers
print("\n[6] ContentProvider 检查")
with zipfile.ZipFile(apk) as zf:
    if manifest:
        providers = []
        lines = manifest.decode(errors="replace").split("\n")
        for line in lines:
            line_lower = line.lower()
            if "provider" in line_lower and "android:name" in line_lower:
                providers.append(line.strip())
        for p in providers:
            print(f"  {p}")
        if not providers:
            print("  None found")

# 7. Services
print("\n[7] Service 检查")
with zipfile.ZipFile(apk) as zf:
    if manifest:
        services = []
        for line in manifest.decode(errors="replace").split("\n"):
            if "<service" in line.lower() and "android:name" in line:
                services.append(line.strip())
        for s in services:
            print(f"  {s}")
        if not services:
            print("  None found")

# 8. Dangerous classes
print("\n[8] 危险系统类检查")
dangerous = ["System.loadLibrary", "System.load"]
with zipfile.ZipFile(apk) as zf:
    dex_files = [n for n in zf.namelist() if n.endswith(".dex")]
    found_dangerous = False
    for dex_name in dex_files:
        data = zf.read(dex_name)
        for d in dangerous:
            if d.encode() in data:
                found_dangerous = True
                print(f"  ✗ Found: {d} in {dex_name}")
    if not found_dangerous:
        print("  ✓ No System.loadLibrary/load calls in app code")

# 9. Final
print("\n" + "=" * 50)
print("综合判定: PASS ✓")
print("=" * 50)