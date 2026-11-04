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


if __name__ == "__main__":
    unittest.main()
