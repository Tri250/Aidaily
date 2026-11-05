"""规则包底座（ADR-0015，2.0.0 M-02 的地基）。

RulePack 是纯数据：机型入口、目标开关、保留 / 受保护清单、安全模式与证据。
包里不允许出现可执行内容——schema 按字段白名单校验，未知字段直接拒绝，
类型也收死在标量 / 字符串列表，任何"夹带代码"都过不了加载。

本版只提供三层能力，引擎不改用（FR-01~05 的行为对拍在 2.0.0 正式版）：
1. validate()：schema 校验；
2. verify_manifest()：SHA-256 清单完整性校验（分发链路上的篡改检测，
   非对称签名等有凭据后再接进来）；
3. load()：校验 + 构造 RulePack；任何失败都抛 RulePackError，
   由调用方回落到内建机型脚本——脚本永远保留，规则包是叠加不是替换。
"""
import hashlib
import json
import os
import re

SCHEMA_VERSION = 1

# 字段白名单：顶层的键一个都不能多。加新字段先改这里和测试。
PACK_FIELDS = {"pack_id", "schema_version", "brand", "models", "build_patterns",
               "segments", "evidence"}
SEGMENT_FIELDS = {"name", "kind", "safe_mode", "targets", "keep", "protected", "note"}

_KINDS = {"recorded", "general", "safety"}
_SAFE_MODES = {"off", "unverified", "on"}

_ID = re.compile(r"^[a-z0-9][a-z0-9._-]{1,80}$", re.I)


class RulePackError(Exception):
    """包不合法：加载失败，回落到内建机型脚本，而不是带病运行。"""


def _bad(msg):
    return RulePackError(msg)


def _str_list(value, field, allow_empty=False):
    if not isinstance(value, list) or not all(isinstance(x, str) for x in value):
        raise _bad(f"{field} 必须是字符串列表")
    if not allow_empty and not value:
        raise _bad(f"{field} 不能为空")
    if not allow_empty and not all(x.strip() for x in value):
        raise _bad(f"{field} 有空条目")
    return value


def validate(pack):
    """按字段白名单校验。任何不确定都拒绝——包是别人递来的数据，不是自家的代码。"""
    if not isinstance(pack, dict):
        raise _bad("规则包必须是对象")
    unknown = set(pack) - PACK_FIELDS
    if unknown:
        raise _bad(f"未知字段：{sorted(unknown)}")
    missing = PACK_FIELDS - set(pack)
    if missing:
        raise _bad(f"缺少字段：{sorted(missing)}")
    if pack["schema_version"] != SCHEMA_VERSION:
        raise _bad(f"schema_version 只支持 {SCHEMA_VERSION}，收到 {pack['schema_version']}")
    if not isinstance(pack["pack_id"], str) or not _ID.match(pack["pack_id"]):
        raise _bad(f"pack_id 必须匹配 {_ID.pattern}")
    pack["brand"] = pack["brand"] or ""
    if not isinstance(pack["brand"], str):
        raise _bad("brand 必须是字符串")
    _str_list(pack["models"], "models")
    _str_list(pack["build_patterns"], "build_patterns", allow_empty=True)
    if not isinstance(pack["segments"], list) or not pack["segments"]:
        raise _bad("segments 必须是非空列表")
    if not isinstance(pack["evidence"], dict):
        raise _bad("evidence 必须是对象")
    for i, seg in enumerate(pack["segments"]):
        seg = _validate_segment(seg, f"segments[{i}]")
        pack["segments"][i] = seg
    return pack


def _validate_segment(seg, where):
    if not isinstance(seg, dict):
        raise _bad(f"{where} 必须是对象")
    unknown = set(seg) - SEGMENT_FIELDS
    if unknown:
        raise _bad(f"{where} 有未知字段：{sorted(unknown)}")
    for f in ("name", "kind", "safe_mode"):
        if not isinstance(seg.get(f), str) or not seg.get(f):
            raise _bad(f"{where}.{f} 必须是非空字符串")
    if seg["kind"] not in _KINDS:
        raise _bad(f"{where}.kind 必须是 {'/'.join(sorted(_KINDS))}")
    if seg["safe_mode"] not in _SAFE_MODES:
        raise _bad(f"{where}.safe_mode 必须是 {'/'.join(sorted(_SAFE_MODES))}")
    seg = dict(seg)
    # targets 允许为空：迁移草案只有步骤名没有目标数据；带目标的包由对拍工具产出
    seg["targets"] = _str_list(seg.get("targets", []), f"{where}.targets", allow_empty=True)
    seg["keep"] = _str_list(seg.get("keep", []), f"{where}.keep", allow_empty=True)
    seg["protected"] = _str_list(seg.get("protected", []), f"{where}.protected", allow_empty=True)
    seg["note"] = seg.get("note", "")
    if not isinstance(seg["note"], str):
        raise _bad(f"{where}.note 必须是字符串")
    return seg


def verify_manifest(manifest_path, root):
    """按 MANIFEST 清单核对包文件：`<sha256>  <相对路径>`。清单本身没签名，
    它防的是分发链路上的截断与错包，不防拿到私钥的人——那要等代码签名。"""
    try:
        with open(manifest_path, encoding="utf-8") as f:
            lines = [line.strip() for line in f if line.strip()]
    except OSError as e:
        raise _bad(f"清单读不到：{e}")
    if not lines:
        raise _bad("清单是空的")
    for line in lines:
        parts = line.split(None, 1)
        if len(parts) != 2:
            raise _bad(f"清单行不合法：{line!r}")
        expected, rel = parts
        rel = rel.strip().lstrip("*")
        # 清单一律写正斜杠（write_manifest 归一化过），但 Windows 上 os.sep 是反斜杠，
        # 直接按 os.sep 切会把 "../x" 当成一个片段放走——统一归一成 / 再判越界。
        norm = rel.replace("\\", "/")
        if (os.path.isabs(norm) or norm.startswith(("/", "\\"))
                or ".." in norm.split("/") or (len(norm) >= 2 and norm[1] == ":")):
            raise _bad(f"清单里出现越界路径：{rel!r}")
        fn = os.path.join(root, rel)
        digest = _sha256(fn)
        if digest != expected.lower():
            raise _bad(f"{rel} 校验值不匹配（期望 {expected[:12]}…，实际 {digest[:12]}…）")


def _sha256(fn):
    h = hashlib.sha256()
    try:
        with open(fn, "rb") as f:
            for chunk in iter(lambda: f.read(65536), b""):
                h.update(chunk)
    except OSError as e:
        raise _bad(f"{fn} 读不到：{e}")
    return h.hexdigest()


def write_manifest(paths, root, manifest_path):
    """打包工具用：为包文件生成 MANIFEST（相对路径 + sha256）。"""
    lines = []
    for rel in sorted(os.path.relpath(p, root).replace(os.sep, "/") for p in paths):
        lines.append(f"{_sha256(os.path.join(root, rel))}  {rel}")
    with open(manifest_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    return manifest_path


def load(path, manifest_path=None):
    """读 + （可选）验 + 校验。任何一步失败都抛 RulePackError，调用方回落内建脚本。"""
    if manifest_path:
        root = os.path.dirname(os.path.abspath(manifest_path))
        verify_manifest(manifest_path, root)
        path = os.path.join(root, "pack.json") if not os.path.exists(path) else path
    try:
        with open(path, encoding="utf-8") as f:
            pack = json.load(f)
    except OSError as e:
        raise _bad(f"规则包读不到：{e}")
    except json.JSONDecodeError as e:
        raise _bad(f"规则包不是合法 JSON：{e}")
    return validate(pack)


def build_matches(pack, device_info):
    """这台机是不是这个包的机型：build_patterns 任一命中 build / os 前缀。
    空模式列表表示"不限版本"，与机型脚本 BUILD="" 的语义一致。"""
    patterns = pack.get("build_patterns") or []
    if not patterns:
        return True
    build, os_version = device_info.get("build") or "", device_info.get("os") or ""
    return any(build.startswith(p) or os_version.startswith(p) for p in patterns)


def segments_for(pack, device_info):
    """返回 [(segment, version_matched)]：版本对不上时调用方应按 safe_mode 走。"""
    matched = build_matches(pack, device_info or {})
    return [(seg, matched) for seg in pack["segments"]]
