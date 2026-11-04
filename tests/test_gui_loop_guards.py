"""主流程闭环审计（2026-11-04 第二轮）：还原入口与完成页回路。

三个闭环缺口：
1. 执行中侧栏「还原改动」可以点：restore 会跟正在跑的任务并发抢同一台手机，
   必须像 ui_check 一样有执行中守卫；
2. 手机断开时 restore("channels") 拿 status 失败的空型号去查备份，而
   Backups.latest("") 的 startswith("") 会匹配任意型号的备份目录 —— 差点把
   别的手机的营销通知清单还原到这台机上；
3. 断连跳过文案承诺「重新连接手机后点『重试』」，完成页却没有回去的入口。
"""
import tempfile
import unittest
from pathlib import Path

from gui import app


class StubRunner:
    def __init__(self, rc=0, out=""):
        self.rc, self.out = rc, out
        self.calls = []

    def run(self, script, *args, timeout=180):
        self.calls.append((script, args, timeout))
        return (self.out, "", self.rc)


def make_api(root, running=False):
    runner = StubRunner()
    api = app.Api(root, adb="/fake/adb", runner=runner)
    if running:
        api.dispatcher.job = app.Job(serial="ABC123", model="OPPO PEXM00", total=1)
        api.dispatcher.job.running = True
    return api, runner


class RestoreLoopTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = self._tmp.name
        self.addCleanup(self._tmp.cleanup)

    def test_restore_is_refused_while_a_job_is_running(self):
        api, runner = make_api(self.root, running=True)
        for what in ("suspend", "quickapp", "channels"):
            with self.subTest(what=what):
                self.assertIn("任务正在执行", api.restore("ABC123", what))
        self.assertEqual(runner.calls, [])

    def test_channels_restore_without_a_model_never_takes_another_models_backup(self):
        api, runner = make_api(self.root)
        (Path(api.backups.root) / "OTHER-20260101-1200").mkdir(parents=True)
        (Path(api.backups.root) / "OTHER-20260101-1200" / "channels.tsv").write_text("# x\n", encoding="utf-8")
        api.status = lambda serial: {"error": "手机和电脑的连接断了"}
        self.assertEqual(api.restore("ABC123", "channels"), "没有找到上次关掉的营销通知清单")
        self.assertEqual(runner.calls, [])

    def test_channels_restore_uses_this_models_own_backup(self):
        api, runner = make_api(self.root)
        d = Path(api.backups.root) / "OPPO PEXM00-20260101-1200"
        d.mkdir(parents=True)
        (d / "channels.tsv").write_text("# x\n", encoding="utf-8")
        api.status = lambda serial: {"info": {"model": "OPPO PEXM00"}}
        api.restore("ABC123", "channels")
        self.assertEqual(runner.calls[0][0], "chan_fast.py")
        self.assertEqual(runner.calls[0][2], 600)


class DonePageLoopTests(unittest.TestCase):
    def test_done_page_offers_a_way_back_to_detect(self):
        html = (Path(__file__).resolve().parents[1] / "gui" / "web" / "index.html").read_text(encoding="utf-8")
        self.assertIn('onclick="retry()"', html)
        self.assertIn("function retry(", html)
        self.assertIn("S.chans = null", html)     # 重进检测前必须丢掉过期的渠道扫描缓存


if __name__ == "__main__":
    unittest.main()
