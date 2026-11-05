"""规则包底座（ADR-0015）：白名单校验、清单完整性、版本匹配、脚本元数据迁移。"""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tool"))

import rulepack
from fixtures import load_tool


def good_pack(**over):
    pack = {
        "pack_id": "oppo-pjd110",
        "schema_version": 1,
        "brand": "oppo",
        "models": ["PJD110"],
        "build_patterns": ["AP3A.240905.015"],
        "segments": [{"name": "浏览器", "kind": "recorded", "safe_mode": "unverified"}],
        "evidence": {"recipe": "recipes/oppo-pjd110/recipe.py"},
    }
    pack.update(over)
    return pack


class ValidateTests(unittest.TestCase):
    def test_good_pack_passes_and_defaults_fill_in(self):
        pack = rulepack.validate(good_pack())
        seg = pack["segments"][0]
        self.assertEqual(seg["targets"], [])
        self.assertEqual(seg["keep"], [])
        self.assertEqual(seg["note"], "")

    def test_unknown_field_is_refused(self):
        with self.assertRaisesRegex(rulepack.RulePackError, "未知字段"):
            rulepack.validate(good_pack(exec=lambda s: __import__("os").system(s)))

    def test_missing_field_is_refused(self):
        pack = good_pack()
        del pack["evidence"]
        with self.assertRaisesRegex(rulepack.RulePackError, "缺少字段"):
            rulepack.validate(pack)

    def test_schema_version_is_pinned(self):
        with self.assertRaisesRegex(rulepack.RulePackError, "schema_version"):
            rulepack.validate(good_pack(schema_version=2))

    def test_pack_id_shape(self):
        with self.assertRaisesRegex(rulepack.RulePackError, "pack_id"):
            rulepack.validate(good_pack(pack_id="; rm -rf /"))

    def test_lists_must_be_strings(self):
        with self.assertRaisesRegex(rulepack.RulePackError, "models"):
            rulepack.validate(good_pack(models=[123]))
        with self.assertRaisesRegex(rulepack.RulePackError, "build_patterns"):
            rulepack.validate(good_pack(build_patterns="AP3A"))

    def test_segment_shape(self):
        with self.assertRaisesRegex(rulepack.RulePackError, "kind"):
            rulepack.validate(good_pack(segments=[{"name": "浏览器", "kind": "magic",
                                                   "safe_mode": "on"}]))
        with self.assertRaisesRegex(rulepack.RulePackError, "safe_mode"):
            rulepack.validate(good_pack(segments=[{"name": "浏览器", "kind": "recorded",
                                                   "safe_mode": "maybe"}]))
        with self.assertRaisesRegex(rulepack.RulePackError, "未知字段"):
            rulepack.validate(good_pack(segments=[{"name": "s", "kind": "recorded",
                                                   "safe_mode": "on", "py": "import os"}]))

    def test_empty_models_refused(self):
        with self.assertRaisesRegex(rulepack.RulePackError, "models"):
            rulepack.validate(good_pack(models=[]))


class ManifestTests(unittest.TestCase):
    def setUp(self):
        d = tempfile.TemporaryDirectory()
        self.addCleanup(d.cleanup)
        self.dir = Path(d.name)
        (self.dir / "pack.json").write_text(json.dumps(good_pack(), ensure_ascii=False), encoding="utf-8")

    def test_manifest_roundtrip(self):
        mf = rulepack.write_manifest([self.dir / "pack.json"], self.dir, self.dir / "MANIFEST.sha256")
        rulepack.verify_manifest(mf, self.dir)      # 不抛即通过
        pack = rulepack.load(self.dir / "pack.json", mf)
        self.assertEqual(pack["pack_id"], "oppo-pjd110")

    def test_tampered_pack_is_refused(self):
        mf = rulepack.write_manifest([self.dir / "pack.json"], self.dir, self.dir / "MANIFEST.sha256")
        (self.dir / "pack.json").write_text('{"pack_id": "evil"}', encoding="utf-8")
        with self.assertRaisesRegex(rulepack.RulePackError, "校验值不匹配"):
            rulepack.load(self.dir / "pack.json", mf)

    def test_path_escape_is_refused(self):
        (self.dir / "MANIFEST.sha256").write_text(
            f"{'0' * 64}  ../elsewhere/pack.json\n", encoding="utf-8")
        with self.assertRaisesRegex(rulepack.RulePackError, "越界路径"):
            rulepack.verify_manifest(self.dir / "MANIFEST.sha256", self.dir)

    def test_path_escape_is_refused_in_windows_forms(self):
        """v0.3.3 CI 实测：按 os.sep 切会把反斜杠/盘符写法放走。两种形式都要拦。"""
        for rel in ("..\\elsewhere\\pack.json", "..\\../elsewhere/pack.json",
                    "C:/elsewhere/pack.json"):
            (self.dir / "MANIFEST.sha256").write_text(f"{'0' * 64}  {rel}\n", encoding="utf-8")
            with self.assertRaisesRegex(rulepack.RulePackError, "越界路径"):
                rulepack.verify_manifest(self.dir / "MANIFEST.sha256", self.dir)

    def test_missing_pack_file_is_refused(self):
        (self.dir / "MANIFEST.sha256").write_text(f"{'0' * 64}  gone.json\n", encoding="utf-8")
        with self.assertRaisesRegex(rulepack.RulePackError, "读不到"):
            rulepack.verify_manifest(self.dir / "MANIFEST.sha256", self.dir)

    def test_bad_json_is_refused(self):
        (self.dir / "broken.json").write_text("{", encoding="utf-8")
        with self.assertRaisesRegex(rulepack.RulePackError, "JSON"):
            rulepack.load(self.dir / "broken.json")


class MatchTests(unittest.TestCase):
    def test_empty_patterns_mean_any_version(self):
        pack = rulepack.validate(good_pack(build_patterns=[]))
        self.assertTrue(rulepack.build_matches(pack, {"build": "anything"}))

    def test_prefix_match_on_build_or_os(self):
        pack = rulepack.validate(good_pack(build_patterns=["AP3A.240905"]))
        self.assertTrue(rulepack.build_matches(pack, {"build": "AP3A.240905.015.A1_2345"}))
        self.assertTrue(rulepack.build_matches(pack, {"build": "", "os": "AP3A.240905.015"}))
        self.assertFalse(rulepack.build_matches(pack, {"build": "BP2A.250605", "os": "BP2A"}))

    def test_segments_carry_the_version_verdict(self):
        pack = rulepack.validate(good_pack(build_patterns=["AP3A"]))
        verdicts = dict((seg["name"], ok) for seg, ok in rulepack.segments_for(pack, {"build": "AP3A"}))
        self.assertEqual(verdicts, {"浏览器": True})
        verdicts = dict((seg["name"], ok) for seg, ok in rulepack.segments_for(pack, {"build": "BP2A"}))
        self.assertEqual(verdicts, {"浏览器": False})


class MigrateTests(unittest.TestCase):
    """全部机型脚本的元数据都能迁出成合法草案（2.0.0-alpha 的交付口径）。"""

    @classmethod
    def setUpClass(cls):
        load_tool()
        cls.root = Path(__file__).resolve().parents[1]
        cls.tmp = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.tmp.cleanup)
        cls.out = Path(cls.tmp.name)
        import rulepack_migrate
        cls.written, cls.problems = rulepack_migrate.migrate(cls.root / "recipes", cls.out)

    def test_every_recipe_produces_a_valid_draft(self):
        recipes = sorted((self.root / "recipes").glob("*/recipe.py"))
        self.assertGreater(len(recipes), 30)
        self.assertEqual(self.written, len(recipes))
        names = sorted(p.name for p in self.out.glob("*.json"))
        self.assertEqual(len(names), self.written)
        for name in names:
            pack = rulepack.load(self.out / name)     # 不抛即合法
            self.assertTrue(pack["models"])
            self.assertEqual(pack["schema_version"], 1)

    def test_drafts_are_marked_unverified(self):
        for fn in self.out.glob("*.json"):
            pack = json.loads(fn.read_text(encoding="utf-8"))
            for seg in pack["segments"]:
                self.assertEqual(seg["safe_mode"], "unverified")
            self.assertIn("对拍", pack["evidence"]["note"])

    def test_recipe_with_no_recorded_build_is_reported(self):
        self.assertTrue(any("BUILD" in p for p in self.problems),
                        f"体检要报出没记 BUILD 的脚本，实际：{self.problems}")

    def test_manifest_of_drafts_verifies(self):
        import rulepack_migrate
        mf = rulepack_migrate.write_manifest(self.out)
        rulepack.verify_manifest(mf, self.out)


if __name__ == "__main__":
    unittest.main()
