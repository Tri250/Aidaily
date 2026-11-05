"""把机型脚本的元数据迁出 RulePack 草案（ADR-0015）。

只迁数据不迁行为：MODELS / BUILD / @step 清单变成包字段，脚本本体仍是唯一
被执行的来源。草案标 safe_mode=unverified——没在真机上对拍之前，它只是
待验证的数据，不是适配成果（AGENTS.md 对 adapt gen 的同一立场）。

用法：
    python tool/rulepack_migrate.py [--out 输出目录] [--manifest 清单路径]

--out 写出草案 JSON；不给 --out 就只做体检，一个文件都不写。
给出了 --out 时，同时产出 MANIFEST.sha256（rulepack.verify_manifest 可核）。
"""
import argparse
import json
import os
import sys

import rulepack

try:
    import run            # recipe_meta 的唯一来源，不在这里复制正则
except ImportError:       # 被当作顶层脚本直接运行时（tool/ 已在 sys.path）
    pass


FAMILY = {"oppo": ["oppo", "oneplus", "realme"], "xiaomi": ["xiaomi", "redmi", "poco"],
          "honor": ["honor"], "vivo": ["vivo", "iqoo"], "meizu": ["meizu"]}


def brand_of(dirname):
    name = dirname.lower()
    head = name.split("-")[0]
    for brand, aliases in FAMILY.items():
        if head in aliases:
            return brand
    return ""


def draft_for(recipe_path, root):
    """一份机型脚本 → 一份 RulePack 草案。返回 (pack, 问题列表)。"""
    meta = run.recipe_meta(recipe_path)
    dirname = os.path.basename(os.path.dirname(recipe_path))
    problems = []
    models = [m for m in meta["models"] if m]
    if not models and "-" in dirname:
        models = [dirname.rsplit("-", 1)[-1]]
    if not models:
        problems.append("没找到型号（MODELS 与目录名都没有）")
    build = meta["build"]
    if not build:
        problems.append("没记录 BUILD 录制版本")
    rel = os.path.relpath(recipe_path, root).replace(os.sep, "/")
    pack = {
        "pack_id": dirname.lower().replace(" ", "_")[:80],
        "schema_version": rulepack.SCHEMA_VERSION,
        "brand": brand_of(dirname),
        "models": models,
        "build_patterns": [build] if build else [],
        "segments": [{"name": step, "kind": "recorded", "safe_mode": "unverified",
                      "targets": [], "note": ""} for step in meta["steps"]],
        "evidence": {"recipe": rel, "steps": len(meta["steps"]),
                     "note": "由机型脚本元数据迁移的草案：行为未经真机对拍，不代表适配成果"},
    }
    return pack, problems


def migrate(recipes_root, out_root=None):
    """迁移整个 recipes/ 目录。返回 (写出数, 体检问题列表)。out_root 为 None 只体检。"""
    written, problems = 0, []
    for dirpath in sorted(os.listdir(recipes_root)):
        recipe = os.path.join(recipes_root, dirpath, "recipe.py")
        if not os.path.isfile(recipe):
            continue
        pack, issues = draft_for(recipe, os.path.dirname(recipes_root))
        problems += [f"{dirpath}: {msg}" for msg in issues]
        try:
            rulepack.validate(pack)
        except rulepack.RulePackError as e:
            problems.append(f"{dirpath}: 草案不合法（{e}）")
            continue
        if out_root:
            os.makedirs(out_root, exist_ok=True)
            fn = os.path.join(out_root, f"{pack['pack_id']}.json")
            with open(fn, "w", encoding="utf-8") as f:
                json.dump(pack, f, ensure_ascii=False, indent=1)
            written += 1
    return written, problems


def write_manifest(out_root):
    """为草案目录生成 MANIFEST.sha256，rulepack.verify_manifest(out, dir) 可核。"""
    paths = [os.path.join(out_root, n) for n in sorted(os.listdir(out_root))
             if n.endswith(".json")]
    if not paths:
        return None
    return rulepack.write_manifest(paths, out_root,
                                   os.path.join(out_root, "MANIFEST.sha256"))


def main():
    ap = argparse.ArgumentParser(description="机型脚本元数据 → RulePack 草案")
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    ap.add_argument("--recipes", default=os.path.join(repo, "recipes"))
    ap.add_argument("--out", default=None, help="草案输出目录；不给就只做体检")
    args = ap.parse_args()
    written, problems = migrate(args.recipes, args.out)
    if args.out and written:
        write_manifest(args.out)
        print(f"写出 {written} 份草案到 {args.out}（含 MANIFEST.sha256）")
    else:
        print(f"体检完成：{written} 份草案可用")
    if problems:
        print(f"\n待人工确认 {len(problems)} 项：")
        for p in problems:
            print(f"- {p}")
        return 1
    print("没有待人工确认的问题")
    return 0


if __name__ == "__main__":
    sys.exit(main())
