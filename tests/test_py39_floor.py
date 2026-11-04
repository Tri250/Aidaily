"""Python 3.9 地板版本守护（兼容性自检，2026-11-04）。

docs/01-准备工作.md 承诺 Python 3.9 以上可跑，CI 只在 3.12+ 跑：PEP 604 注解
（`X | None`）在 3.9 是导入时就炸的 TypeError，测试环境发现不了。
这两个模块的注解在导入时求值（fast.py 的构造签名、chan_fast.py 的 dataclass
字段），必须挂 `from __future__ import annotations`。以后往这两个模块或别的
模块加 PEP 604 注解时，要么同样挂 future，要么改用 Optional。
"""
import unittest
from pathlib import Path

TOOL = Path(__file__).resolve().parents[1] / "tool"


class Py39FloorTests(unittest.TestCase):
    def test_modules_with_pep604_annotations_carry_the_future_import(self):
        for name in ("fast.py", "chan_fast.py"):
            with self.subTest(module=name):
                self.assertIn("from __future__ import annotations",
                              (TOOL / name).read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
