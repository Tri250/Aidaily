"""渠道扫描的判定理由（FR-07 计划可解释）：勾与不勾都要说得出依据。"""
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tool"))
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from fixtures import FakeRunner
from gui import app

SCAN = "\n".join([
    "com.a\tVPushChannel_0\t运营消息\tvivo商店",       # 厂商营销分类号 + 关键词
    "com.b\tTencentAdNetCH1\t通知\t桌面",              # 广告网络渠道 ID
    "com.c\tpromo\t活动优惠\t购物",                    # 营销关键词
    "com.d\tdrive_mode\t行车助手\t地图",               # 服务类：默认不勾
    "com.e\tmisc\t天气提醒\t天气",                     # 没命中任何特征：默认勾，理由要直说
    "com.f\tcam1\t找机位\t相机",                       # 受保护：不该出现在候选里
]) + "\n"


class ChanReasonTests(unittest.TestCase):
    def rows(self):
        with tempfile.TemporaryDirectory() as d:
            api = app.Api(d, runner=FakeRunner(runs=(SCAN, "", 0)))
            return api.chan_scan("S1")["chans"]

    def test_every_row_carries_a_reason(self):
        rows = self.rows()
        reasons = {r["pkg"]: r["reason"] for r in rows}
        self.assertTrue(rows)
        for pkg, reason in reasons.items():
            self.assertTrue(reason.strip(), f"{pkg} 的判定理由是空的")

    def test_vendor_marketing_id_and_keyword_hit_together(self):
        reasons = {r["pkg"]: r["reason"] for r in self.rows()}
        self.assertEqual(reasons["com.a"], "厂商营销分类号；营销关键词")

    def test_ad_network_id_is_not_counted_as_keyword(self):
        reasons = {r["pkg"]: r["reason"] for r in self.rows()}
        self.assertEqual(reasons["com.b"], "广告网络渠道 ID")

    def test_keyword_only(self):
        reasons = {r["pkg"]: r["reason"] for r in self.rows()}
        self.assertEqual(reasons["com.c"], "营销关键词")

    def test_service_rows_are_unchecked_and_explained(self):
        rows = {r["pkg"]: r for r in self.rows()}
        self.assertFalse(rows["com.d"]["checked"])
        self.assertIn("默认不勾", rows["com.d"]["reason"])

    def test_unknown_rows_say_so(self):
        reasons = {r["pkg"]: r["reason"] for r in self.rows()}
        self.assertEqual(reasons["com.e"], "名称与分类号都没命中已知特征，默认勾选")

    def test_protected_channels_never_become_candidates(self):
        self.assertNotIn("com.f", {r["pkg"] for r in self.rows()})


if __name__ == "__main__":
    unittest.main()
