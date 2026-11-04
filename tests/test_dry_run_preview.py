"""执行前预览（2.0 第一批）：off() 在 dry 下只算拨动计划、不点击；CLI `dry` 命令与 `run --dry` 传参。

预览的承诺：正式执行会点哪些，预览就列哪些 —— 标题与 want_state 判定写死在页面里，不重算实现。
页面结构照 test_phone_interface.py 的行式开关（容器 clickable + 文字 + Switch）。
"""
import io
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tool"))
sys.path.insert(0, str(ROOT / "tests"))
import fast
import fixtures
from fixtures import fake_phone, install_clock

# 两个开着的广告开关 + 一个关着的保护类开关 + 一个已在目标态的普通开关：
# dry 计划应为「关 2 项、打开（保护类）1 项」，点击数为 0。
PAGE = '''<hierarchy rotation="0">
  <node package="com.android.settings" clickable="true" bounds="[0,300][1080,380]">
    <node text="个性化推荐" bounds="[60,310][400,370]"/>
    <node class="android.widget.Switch" checkable="true" checked="true" bounds="[950,300][1060,380]"/>
  </node>
  <node package="com.android.settings" clickable="true" bounds="[0,500][1080,580]">
    <node text="猜你喜欢" bounds="[60,510][400,570]"/>
    <node class="android.widget.Switch" checkable="true" checked="true" bounds="[950,500][1060,580]"/>
  </node>
  <node package="com.android.settings" clickable="true" bounds="[0,700][1080,780]">
    <node text="限制广告跟踪" bounds="[60,710][400,770]"/>
    <node class="android.widget.Switch" checkable="true" checked="false" bounds="[950,700][1060,780]"/>
  </node>
  <node package="com.android.settings" clickable="true" bounds="[0,900][1080,980]">
    <node text="红包助手" bounds="[60,910][400,970]"/>
    <node class="android.widget.Switch" checkable="true" checked="false" bounds="[950,900][1060,980]"/>
  </node>
</hierarchy>'''

# 全部开关都已在目标态：预览应说明「本页无需改动」
IDLE_PAGE = '''<hierarchy rotation="0">
  <node package="com.android.settings" clickable="true" bounds="[0,900][1080,980]">
    <node text="红包助手" bounds="[60,910][400,970]"/>
    <node class="android.widget.Switch" checkable="true" checked="false" bounds="[950,900][1060,980]"/>
  </node>
</hierarchy>'''

# 灰开关照 #333 的卡片结构：预览不算目标，但要点名「被上级开关禁用」
GREY_PAGE = '''<hierarchy rotation="0">
  <node class="android.widget.LinearLayout" checkable="true" clickable="true" bounds="[0,500][1080,760]">
    <node text="摇一摇广告" bounds="[60,520][600,600]"/>
    <node class="android.widget.Switch" checkable="true" checked="true" enabled="false" bounds="[900,520][1000,600]"/>
  </node>
</hierarchy>'''


class DryPreviewTests(unittest.TestCase):
    """off() 的 dry 分支：计划与正式执行同源（plan_switches），但一下都不点。"""

    def phone(self, page=PAGE, **fields):
        fields.setdefault("dry", True)                # 默认测 dry 分支；对照用例显式传 dry=False
        self.clock = fixtures.Clock()
        install_clock(fast, self.clock)               # 让 wait.until 的重试走假时钟，不真睡
        self.lines = []
        return fake_phone(clock=self.clock, stages=[(0.0, page)],
                          say=lambda *a, **k: self.lines.append(a[0]), **fields)

    def test_dry_off_lists_the_plan_without_clicking(self):
        p = self.phone()
        self.assertEqual(p.off(page="测试页"), 3)
        self.assertEqual(self.clock.clicks, 0)
        self.assertEqual(p.dry_plan, [("测试页", "个性化推荐", False),
                                      ("测试页", "猜你喜欢", False),
                                      ("测试页", "限制广告跟踪", True)])
        self.assertEqual(p.changed, [])               # 没有把任何开关记成「已关」
        self.assertEqual(self.lines, ["  · 测试页：预览：将关 2 项：个性化推荐、猜你喜欢；"
                                      "将打开（保护类）1 项：限制广告跟踪"])

    def test_the_same_page_for_real_still_clicks(self):
        p = self.phone(dry=False)
        p.off(page="测试页")
        self.assertGreater(self.clock.clicks, 0)      # 对照：同一页在正式执行下会点

    def test_an_idle_page_says_so_with_the_checked_count(self):
        p = self.phone(IDLE_PAGE)
        self.assertEqual(p.off(page="闲页"), 0)
        self.assertEqual(self.clock.clicks, 0)
        self.assertEqual(self.lines, ["  · 闲页：预览：本页无需改动（核对 1 项）"])

    def test_a_greyed_switch_is_reported_not_planned(self):
        p = self.phone(GREY_PAGE)
        self.assertEqual(p.off(page="灰页"), 0)
        self.assertEqual(self.clock.clicks, 0)
        self.assertEqual(self.lines, ["  · 灰页：预览：1 个被上级开关禁用（执行时同样不点）"])

    def test_the_same_switch_still_visible_after_a_scroll_is_listed_once(self):
        p = self.phone()
        p.scroll_more = lambda *a, **k: True          # 翻页后同一行还留在视野里：按标题去重
        self.assertEqual(p.off(page="测试页"), 3)
        self.assertEqual(self.clock.clicks, 0)
        self.assertEqual([t for _, t, _ in p.dry_plan], ["个性化推荐", "猜你喜欢", "限制广告跟踪"])


class DryCliTests(unittest.TestCase):
    """`fast.py -s S dry`：读当前页、报计划、退出码 0；`run --dry/--ref` 传到 Phone 上。"""

    def test_dry_command_reports_the_plan_and_exit_zero(self):
        clock = fixtures.Clock()
        install_clock(fast, clock)
        p = fake_phone(clock=clock, stages=[(0.0, PAGE)])
        with redirect_stdout(io.StringIO()) as out:
            self.assertEqual(fast.execute(p, "dry", []), None)
        self.assertEqual(clock.clicks, 0)
        self.assertIn("预览：本页将改动 3 项", out.getvalue())

    def test_run_passes_dry_and_ref_flags_to_the_phone(self):
        clock = fixtures.Clock()
        install_clock(fast, clock)
        p = fake_phone(clock=clock)
        with redirect_stdout(io.StringIO()):
            with self.assertRaises(FileNotFoundError):     # 脚本不存在，但传参已发生
                fast.execute(p, "run", ["没有这份脚本.py", "--dry", "--ref"])
        self.assertTrue(p.dry)
        self.assertTrue(p.safe_mode)

    def test_run_without_flags_keeps_preview_off(self):
        clock = fixtures.Clock()
        install_clock(fast, clock)
        p = fake_phone(clock=clock)
        with redirect_stdout(io.StringIO()):
            with self.assertRaises(FileNotFoundError):
                fast.execute(p, "run", ["没有这份脚本.py"])
        self.assertFalse(p.dry)


if __name__ == "__main__":
    unittest.main()
