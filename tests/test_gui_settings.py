"""设置页署名与版本号一致性（发布自检，2026-11-04）。

设置弹窗要有开发者署名；官网下载页的资源基址必须跟着 gui/app.py 的 VERSION 走，
升版本时漏改 index.html 会让下载按钮指向不存在的 Release。
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class SettingsCreditTests(unittest.TestCase):
    def test_settings_modal_carries_the_developer_credit(self):
        html = (ROOT / "gui" / "web" / "index.html").read_text(encoding="utf-8")
        self.assertIn("开发者：某厂大牛马【带娃的小陈工】", html)


class VersionConsistencyTests(unittest.TestCase):
    def test_promo_page_download_base_matches_app_version(self):
        from gui import app
        html = (ROOT / "index.html").read_text(encoding="utf-8")
        self.assertIn(f"releases/download/v{app.VERSION}/", html)


class ReleasePageConsistencyTests(unittest.TestCase):
    """v0.3.2 的一致性缺陷（FR-27）：Release 没有的产物，官网不能给下载按钮。

    守卫三条：每个产物条目必须显式声明 published；published 条目的文件名
    版本号要和 REL.version 一致；渲染入口必须过滤 published: false。
    """
    def setUp(self):
        import re
        self.html = (ROOT / "index.html").read_text(encoding="utf-8")
        rel = re.search(r"const REL = \{(.*?)\n\};", self.html, re.S)
        self.rel = rel.group(1)
        self.version = re.search(r'version: "([^"]+)"', self.rel).group(1)

    def entries(self):
        """按条目起点（换行 + 缩进 + `{ id:`）切块，published 缺了就是 None。"""
        import re
        out = []
        for block in re.split(r"\n    \{ id: ", self.rel)[1:]:
            eid = re.match(r'"([^"]+)"', block).group(1)
            pub = re.search(r"published: (true|false)", block)
            f = re.search(r'file: "([^"]+)"', block)
            out.append((eid, pub.group(1) if pub else None, f.group(1) if f else None))
        return out

    def test_every_entry_declares_published(self):
        entries = self.entries()
        self.assertEqual(len(entries), 4, "四个产物条目都要有 published 标记")
        missing = [i for i, pub, _ in entries if pub is None]
        self.assertEqual(missing, [], f"这些条目缺 published 标记：{missing}")
        self.assertIn("filter((f) => f.published !== false)", self.html,
                      "渲染入口必须过滤未发布的产物")

    def test_published_entries_carry_the_current_version(self):
        bad = [(i, f) for i, pub, f in self.entries() if pub == "true" and f"-{self.version}-" not in f]
        self.assertEqual(bad, [], f"published 条目文件名版本不对：{bad}")

    def test_current_release_publishes_windows_only(self):
        """现状固化：v0.3.2 只发了 Windows。翻转 Mac 的 published 前必须真的有产物。"""
        flags = {i: pub for i, pub, _ in self.entries()}
        self.assertEqual(flags, {"mac-arm64": "false", "mac-intel": "false",
                                 "win-setup": "true", "win-zip": "true"})

    def test_stale_mac_names_are_not_silently_rebranded(self):
        """未发布条目保留旧产物名（0.3.1 占位），而不是改成新版本号装作已发。"""
        files = {i: f for i, _, f in self.entries()}
        self.assertIn("-0.3.1-", files["mac-arm64"])
        self.assertIn("-0.3.1-", files["mac-intel"])


if __name__ == "__main__":
    unittest.main()
