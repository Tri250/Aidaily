"""干净度估算分（2.0 第一批）：`run.py -s S score` 只读聚合，不写文件、不点击。

分数是估算，权重写死在 run.SCORE_WEIGHTS；这里断言分类计数、减分与封顶，
以及扫描失败时「本项不计分」的兜底。全部替身化，不碰真机也不起子进程。
"""
import contextlib
import io
import sys
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tool"))
sys.path.insert(0, str(ROOT / "tests"))
import run

INFO = {"brand": "oppo", "model": "X", "build": "C", "android": "16", "name": "X", "os": "C"}
QUICKAPPS = [{"name": "快游戏", "closed": False, "keep": False},
             {"name": "快应用平台", "closed": False, "keep": True}]


def scan_ok(stdout=""):
    return SimpleNamespace(returncode=0, stdout=stdout, stderr="")


def scan_fail(stderr="boom"):
    return SimpleNamespace(returncode=1, stdout="", stderr=stderr)


class ScoreTests(unittest.TestCase):
    def setUp(self):
        patcher = patch.object(run, "adb", return_value="adb")
        patcher.start()
        self.addCleanup(patcher.stop)

    def score(self, *, packages, quickapps=QUICKAPPS, scan=None, suspended=False, serial="S"):
        scan = scan_ok() if scan is None else scan
        with patch.object(run, "info", return_value=INFO), \
             patch.object(run, "ad_packages", return_value=packages), \
             patch.object(run, "installed", return_value=True), \
             patch.object(run, "suspended", return_value=suspended), \
             patch.object(run, "quickapps", return_value=quickapps), \
             patch.object(run.subprocess, "run", return_value=scan), \
             contextlib.redirect_stdout(io.StringIO()) as out:
            self.assertEqual(run.main(["-s", serial, "score"]), 0)
        return out.getvalue()

    def test_mixed_exposure_scores_each_category(self):
        out = self.score(packages=["com.example.ads", "com.example.gone"],
                         scan=scan_ok("渠道1\n渠道2\n"))
        self.assertIn("营销通知候选：2 个（-4，上限 40", out)
        self.assertIn("广告组件未挂起：2 个（-30）：com.example.ads、com.example.gone", out)
        self.assertIn("快应用未关闭：1 个（-10）：快游戏", out)      # 保留项不算
        self.assertIn("干净度评分（估算）：56 / 100", out)
        self.assertIn("App 内开关不参与计分", out)

    def test_a_clean_device_scores_a_hundred(self):
        out = self.score(packages=["com.example.ads"], suspended=True,
                         quickapps=[{"name": "快应用平台", "closed": True, "keep": False}])
        self.assertIn("广告组件未挂起：0 个（-0）", out)
        self.assertIn("干净度评分（估算）：100 / 100", out)

    def test_a_failed_scan_skips_that_category_without_deduction(self):
        out = self.score(packages=[], quickapps=[], scan=scan_fail("读不到通知服务"))
        self.assertIn("营销通知候选：扫描未完成，本项不计分（读不到通知服务）", out)
        self.assertIn("干净度评分（估算）：100 / 100", out)

    def test_channel_deduction_is_capped(self):
        out = self.score(packages=[], quickapps=[],
                         scan=scan_ok("\n".join(f"渠道{i}" for i in range(40)) + "\n"))
        self.assertIn("营销通知候选：40 个（-40，上限 40", out)
        self.assertIn("干净度评分（估算）：60 / 100", out)


if __name__ == "__main__":
    unittest.main()
