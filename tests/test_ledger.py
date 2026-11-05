"""变更账本与单条还原（ADR-0014，2.0.0 M-05 底座）。

账本条目只写实际观察到的结论（rc==0 才算生效）；还原命令的参数只从
白名单方法里取；串号精确匹配，坏文件跳过；给前端的账本先脱敏。
"""
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tool"))
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ledger
from fixtures import FakeProc, FakeRunner, job_stub
from gui import app


def chan(title="关闭营销通知", pkg="com.a", cid="promo", name="活动优惠", appname="A"):
    return app.Task(title, "chan_fast.py", uses_ui=True,
                    channels=[{"pkg": pkg, "id": cid, "name": name, "app": appname}])


class BuildTests(unittest.TestCase):
    def test_channels_closed_when_rc_zero_and_undo_rows_carry_the_tsv_columns(self):
        rec = ledger.build([chan()], [{"title": "关闭营销通知", "rc": 0, "sec": 2}],
                           serial="S1", model="M1", version="0.3.2", t0=0, elapsed=2)
        self.assertEqual([i["after"] for i in rec["items"]], ["已关"])
        item = rec["items"][0]
        self.assertEqual(item["undo"], {"method": "chan_on", "row": ["com.a", "promo", "活动优惠", "A"]})
        self.assertEqual(item["before"], "开")

    def test_tab_in_app_name_cannot_break_the_undo_tsv(self):
        rec = ledger.build([chan(appname="淘\t宝")], [{"title": "关闭营销通知", "rc": 0}],
                           serial="S1", model="M1", version="0.3.2", t0=0, elapsed=0)
        self.assertEqual(rec["items"][0]["undo"]["row"][3], "淘 宝")

    def test_rc_not_zero_records_unconfirmed_not_success(self):
        rec = ledger.build([chan()], [{"title": "关闭营销通知", "rc": 1}],
                           serial="S1", model="M1", version="0.3.2", t0=0, elapsed=0)
        self.assertEqual(rec["items"][0]["after"], "未确认")

    def test_suspend_task_records_each_package(self):
        task = app.Task("挂起广告组件", "run.py", ["-s", "S1", "suspend", "com.msa", "com.ad"])
        rec = ledger.build([task], [{"title": "挂起广告组件", "rc": 0}],
                           serial="S1", model="M1", version="0.3.2", t0=0, elapsed=0)
        self.assertEqual([i["pkg"] for i in rec["items"]], ["com.msa", "com.ad"])
        self.assertEqual(rec["items"][0]["undo"], {"method": "unsuspend", "pkg": "com.msa"})

    def test_quickapp_task_is_not_mistaken_for_suspend(self):
        task = app.Task("关闭快应用", "run.py", ["-s", "S1", "quickapp", "off"])
        rec = ledger.build([task], [{"title": "关闭快应用", "rc": 0}],
                           serial="S1", model="M1", version="0.3.2", t0=0, elapsed=0)
        self.assertEqual(rec["items"], [])

    def test_skipped_and_stopped_tasks_have_no_items(self):
        rec = ledger.build([chan()], [{"title": "关闭营销通知", "rc": None, "skipped": True}],
                           serial="S1", model="M1", version="0.3.2", t0=0, elapsed=0)
        self.assertEqual(rec["items"], [])
        rec = ledger.build([chan()], [{"title": "关闭营销通知", "rc": None, "stopped": True}],
                           serial="S1", model="M1", version="0.3.2", t0=0, elapsed=0)
        self.assertEqual(rec["items"], [])

    def test_tasks_and_batch_shape(self):
        rec = ledger.build([chan()], [{"title": "关闭营销通知", "rc": 0, "sec": 2}],
                           serial="S1", model="M1", version="0.3.2", t0=0, elapsed=2)
        self.assertEqual(rec["tasks"], [{"title": "关闭营销通知", "rc": 0, "sec": 2,
                                         "skipped": False, "stopped": False}])
        self.assertEqual(rec["serial"], "S1")
        self.assertEqual(rec["model"], "M1")
        self.assertEqual(rec["version"], "0.3.2")
        self.assertTrue(rec["batch"])


class UndoPlanTests(unittest.TestCase):
    def test_chan_rows_and_packages(self):
        items = [{"undo": {"method": "chan_on", "row": ["com.a", "promo", "活动优惠", "A"]}},
                 {"undo": {"method": "unsuspend", "pkg": "com.msa"}},
                 {"undo": {"method": "unsuspend", "pkg": "com.msa"}}]
        plan = ledger.undo_plan(items)
        self.assertEqual(plan, {"chan_rows": [["com.a", "promo", "活动优惠", "A"]],
                                "pkgs": ["com.msa"]})    # 重复包名去重，只还原一次

    def test_entries_without_undo_data_are_skipped(self):
        plan = ledger.undo_plan([{"kind": "channel"}, None, {"undo": {}}])
        self.assertEqual(plan, {"chan_rows": [], "pkgs": []})

    def test_unknown_method_is_refused(self):
        with self.assertRaises(ledger.LedgerError):
            ledger.undo_plan([{"undo": {"method": "shell", "cmd": "reboot"}}])

    def test_incomplete_chan_row_is_refused(self):
        plan = ledger.undo_plan([{"undo": {"method": "chan_on", "row": ["com.a"]}}])
        self.assertEqual(plan["chan_rows"], [])


class ScrubTests(unittest.TestCase):
    def test_serial_is_scrubbed_but_model_stays(self):
        rec = {"serial": "ABC123", "model": "M1", "items": [{"pkg": "com.a"}]}
        out = ledger.scrub(rec)
        self.assertEqual(out["serial"], "<序列号>")
        self.assertEqual(out["model"], "M1")
        self.assertEqual(rec["serial"], "ABC123")   # 原件不动：本地还原还要用


class StoreTests(unittest.TestCase):
    def test_latest_matches_serial_exactly(self):
        with tempfile.TemporaryDirectory() as d:
            store = ledger.Ledger(Path(d, "ledgers"))
            store.write({"t0": "20260101-0900", "batch": "a", "serial": "S1", "model": "M1"})
            store.write({"t0": "20260101-1000", "batch": "b", "serial": "S2", "model": "M2"})
            self.assertEqual(store.latest(serial="S2")["batch"], "b")
            self.assertEqual(store.latest(serial="S1")["batch"], "a")
            self.assertIsNone(store.latest(serial="S9"))
            self.assertEqual(store.latest()["batch"], "b")   # 不给串号：最近一份

    def test_bad_json_is_skipped_not_fatal(self):
        with tempfile.TemporaryDirectory() as d:
            store = ledger.Ledger(Path(d, "ledgers"))
            store.write({"t0": "20260101-0900", "batch": "a", "serial": "S1", "model": "M1"})
            (Path(d, "ledgers", "20260101-1000-b.json")).write_text("{坏了", encoding="utf-8")
            self.assertEqual(store.latest()["batch"], "a")
            self.assertIsNone(store.read(Path(d, "ledgers", "20260101-1000-b.json")))

    @unittest.skipUnless(os.name == "posix", "POSIX 权限")
    def test_ledger_file_is_owner_only(self):
        with tempfile.TemporaryDirectory() as d:
            store = ledger.Ledger(Path(d, "ledgers"))
            fn = store.write({"t0": "20260101-0900", "batch": "a", "serial": "S1", "model": "M1"})
            self.assertEqual(os.stat(fn).st_mode & 0o777, 0o600)


class GuiLedgerTests(unittest.TestCase):
    def api(self, directory, **runner):
        api = app.Api(directory, runner=FakeRunner(**runner))
        api.job = job_stub(serial="S1", model="M1", running=False)
        return api

    def test_job_end_writes_ledger_with_items_and_scrubbed_view(self):
        with tempfile.TemporaryDirectory() as d:
            api = self.api(d, spawns=[FakeProc("关了 1 个渠道\n"), FakeProc()])
            api._work([chan(), app.Task("收尾", "run.py", ["-s", "S1", "finish"])])
            rec = api.ledger("S1")["record"]
            self.assertEqual(rec["model"], "M1")
            self.assertEqual([(i["kind"], i["after"]) for i in rec["items"]], [("channel", "已关")])
            self.assertEqual([t["title"] for t in rec["tasks"]], ["关闭营销通知", "收尾"])
            self.assertNotIn("S1", json.dumps(rec))    # 给前端的账本序列号脱敏

    def test_ledger_failure_never_breaks_the_job(self):
        with tempfile.TemporaryDirectory() as d:
            api = self.api(d, spawns=[FakeProc()])
            with mock.patch("ledger.Ledger.write", side_effect=OSError("disk full")):
                api._work([chan()])
            self.assertEqual(api.job.results[0]["rc"], 0)   # 执行照常成功
            self.assertTrue(any("变更账本" in line for line in api.job.lines))

    def test_ledger_is_refused_while_job_is_running(self):
        with tempfile.TemporaryDirectory() as d:
            api = self.api(d)
            api.job.running = True
            self.assertIn("error", api.ledger("S1"))
            self.assertIn("请结束后", api.restore_items("S1", []))

    def test_restore_items_writes_single_row_tsv_and_unsuspends_once(self):
        with tempfile.TemporaryDirectory() as d:
            api = app.Api(d, runner=FakeRunner(runs=("", "com.msa 已恢复\n", 0)))
            api.job = job_stub(serial="S1", model="M1", running=False)
            out = api.restore_items("S1", [
                {"undo": {"method": "chan_on", "row": ["com.a", "promo", "活动优惠", "A"]}},
                {"undo": {"method": "unsuspend", "pkg": "com.msa"}},
                {"undo": {"method": "unsuspend", "pkg": "com.msa"}},
            ])
            calls = api.runner.run_calls
            self.assertEqual(calls[0].script, "chan_fast.py")
            self.assertEqual(calls[0].args[:3], ("-s", "S1", "on"))
            self.assertEqual(Path(calls[0].args[3]).read_text(encoding="utf-8"),
                             "com.a\tpromo\t活动优惠\tA\n")
            self.assertEqual(calls[1].script, "run.py")
            self.assertEqual(calls[1].args[2:], ("unsuspend", "com.msa"))   # 去重后只挂一次
            self.assertIn("com.msa", out)

    def test_restore_items_reports_problems_without_running_tools(self):
        with tempfile.TemporaryDirectory() as d:
            api = self.api(d)
            self.assertIn("还原清单有问题", api.restore_items("S1", [{"undo": {"method": "x"}}]))
            self.assertEqual(api.restore_items("S1", []), "没有可以还原的条目")
            self.assertEqual(api.runner.run_calls, [])
