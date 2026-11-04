"""客户端设备列表解析与 dev-off 结果契约（四段主流程自检，2026-11-04）。

adb 本体是外部边界：这里用假输出顶替，被测的是 `devices()` 的解析与
`dev_off()` 的返回结构。daemon 启动行假设备：adb server 没起时 `adb devices -l`
的输出前面带两行 `* daemon …`，旧解析只跳过第一行，把 `*` 和 `List` 当成
两台设备显示给用户（首次下载 adb 后第一次连接必现）。
"""
import tempfile
import unittest

from gui import app


DAEMON_HEAD = ("* daemon not running; starting now at tcp:5037\n"
               "* daemon started successfully\n")
HEADER = "List of devices attached\n"
PROPS = "OPPO\nPEXM00\nFind X8\n\n"          # getprop 的四行输出：品牌、型号、上市名、市场名


def make_api(root, devices_out):
    api = app.Api(root, adb="/fake/adb")

    def _adb(*args, timeout=20):
        return devices_out if args[0] == "devices" else PROPS

    api._adb = _adb
    return api


class DevicesParsingTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = self._tmp.name
        self.addCleanup(self._tmp.cleanup)

    def test_daemon_startup_lines_are_not_devices(self):
        out = DAEMON_HEAD + HEADER + "ABC123\tdevice usb:1-1 product:r0\n"
        devices = make_api(self.root, out).devices()
        self.assertEqual([d["serial"] for d in devices["list"]], ["ABC123"])

    def test_header_line_is_not_a_device(self):
        out = HEADER + "ABC123\tdevice\nDEF456\tunauthorized\n"
        devices = make_api(self.root, out).devices()
        self.assertEqual([d["serial"] for d in devices["list"]], ["ABC123", "DEF456"])

    def test_device_props_are_read_only_for_connected_devices(self):
        out = HEADER + "ABC123\tdevice\nDEF456\tunauthorized\n"
        devices = make_api(self.root, out).devices()
        by_serial = {d["serial"]: d for d in devices["list"]}
        self.assertEqual(by_serial["ABC123"]["brand"], "OPPO")
        self.assertEqual(by_serial["ABC123"]["model"], "PEXM00")
        self.assertEqual(by_serial["ABC123"]["name"], "Find X8")
        self.assertEqual(by_serial["DEF456"]["brand"], "")

    def test_emulator_entries_are_skipped(self):
        out = HEADER + "emulator-5554\tdevice\nABC123\tdevice\n"
        devices = make_api(self.root, out).devices()
        self.assertEqual([d["serial"] for d in devices["list"]], ["ABC123"])

    def test_device_lines_helper_skips_noise(self):
        out = DAEMON_HEAD + HEADER + "\nS\toffline\n"
        self.assertEqual(list(app.device_lines(out)), [("S", "offline")])


class DevOffResultTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = self._tmp.name
        self.addCleanup(self._tmp.cleanup)

    def api(self, rc, out="", err=""):
        api = app.Api(self.root, adb="/fake/adb")
        api._run = lambda *a, **k: (out, err, rc)
        return api

    def test_success_reports_ok_with_the_engine_text(self):
        result = self.api(0, out="已发出关闭指令。请在手机上确认总开关是关着的。\n").dev_off("S")
        self.assertTrue(result["ok"])
        self.assertIn("已发出关闭指令", result["text"])

    def test_failure_carries_nonzero_rc_and_stderr(self):
        result = self.api(1, err="✗ adb 连接断开\n").dev_off("S")
        self.assertFalse(result["ok"])
        self.assertIn("连接断开", result["text"])


if __name__ == "__main__":
    unittest.main()
