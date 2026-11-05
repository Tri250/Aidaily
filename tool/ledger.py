"""变更账本（ADR-0014，2.0.0 M-05 的底座）。

一次执行结束后把「改了什么」按条目落盘到本地数据目录，条目自带单条还原所需
的数据；还原时只认条目里写明的方法，白名单之外一律拒绝：

- channel：客户端勾选的那一行（还原 = `chan_fast.py on <单行清单>`）
- package：挂起的广告组件包名（还原 = `run.py unsuspend <包名>`）

账本只存本地、不上传；给前端展示前先经 scrub() 把序列号脱敏。
"""
import copy
import json
import os
import re
import time
import uuid

KIND_CHANNEL = "channel"
KIND_PACKAGE = "package"
UNDO_METHODS = {"chan_on", "unsuspend"}


class LedgerError(Exception):
    """账本条目不合法：拒绝生成还原命令，而不是猜。"""


def _clean(text):
    return re.sub(r"[\t\r\n]+", " ", str(text or "")).strip()


def build(tasks, results, serial, model, version, t0, elapsed):
    """由任务清单和执行结果构建账本。执行结果里每步 rc==0 才认为条目确定生效，
    否则记「未确认」——账本只写实际观察到的结论，不写推断。"""
    by_title = {r.get("title"): r for r in results or []}
    items = []
    for task in tasks:
        r = by_title.get(task.title) or {}
        rc, skipped, stopped = r.get("rc"), r.get("skipped"), r.get("stopped")
        ran = not skipped and not stopped
        if task.channels is not None and ran:
            after = "已关" if rc == 0 else "未确认"
            for c in task.channels:
                row = [c["pkg"], c["id"], _clean(c.get("name")), _clean(c.get("app"))]
                items.append({"kind": KIND_CHANNEL, "pkg": row[0], "id": row[1],
                              "name": row[2], "app": row[3], "before": "开", "after": after,
                              "undo": {"method": "chan_on", "row": row}})
        if task.script == "run.py" and len(task.args) >= 4 and task.args[2] == "suspend" and ran:
            after = "已挂起" if rc == 0 else "未确认"
            for pkg in task.args[3:]:
                items.append({"kind": KIND_PACKAGE, "pkg": pkg, "name": "",
                              "app": "", "before": "运行", "after": after,
                              "undo": {"method": "unsuspend", "pkg": pkg}})
    return {
        "batch": uuid.uuid4().hex[:8],
        "version": version,
        "serial": serial,
        "model": model,
        "t0": time.strftime("%Y%m%d-%H%M%S", time.localtime(t0)),
        "elapsed": round(elapsed),
        "tasks": [{"title": r.get("title", ""), "rc": r.get("rc"), "sec": r.get("sec", 0),
                   "skipped": bool(r.get("skipped")), "stopped": bool(r.get("stopped"))}
                  for r in results or []],
        "items": items,
    }


def undo_plan(items):
    """把要还原的条目变成两类命令的参数。只认白名单方法，防注入：包名 / 渠道列
    原样透传给引擎，引擎侧本就只按白名单处理。"""
    chan_rows, pkgs = [], []
    for it in items or []:
        undo = (it or {}).get("undo") or {}
        method = undo.get("method")
        if method == "chan_on":
            row = [_clean(x) for x in (undo.get("row") or [])]
            if len(row) >= 2 and row[0] and row[1]:
                chan_rows.append(row)
        elif method == "unsuspend":
            pkg = _clean(undo.get("pkg"))
            if pkg and pkg not in pkgs:
                pkgs.append(pkg)
        elif method is None:
            continue                      # 没有还原数据的条目：只能整类还原
        else:
            raise LedgerError(f"不支持的还原方法：{method}")
    return {"chan_rows": chan_rows, "pkgs": pkgs}


def scrub(record):
    """给前端展示用的副本：序列号脱敏，其余原样。"""
    out = copy.deepcopy(record or {})
    if out.get("serial"):
        out["serial"] = "<序列号>"
    return out


class Ledger:
    """账本文件：<数据目录>/ledgers/<时间>-<批次>.json。文件名按时间排序，latest()
    只挑串号精确匹配的那份；型号为空时同样拒绝匹配（与 backups.latest 同一教训）。"""

    def __init__(self, root):
        self.root = os.fspath(root)

    def path(self, record):
        return os.path.join(self.root, f"{record['t0']}-{record['batch']}.json")

    def write(self, record):
        os.makedirs(self.root, exist_ok=True)
        fn = self.path(record)
        with open(fn, "w", encoding="utf-8") as f:
            json.dump(record, f, ensure_ascii=False, indent=1)
        if os.name == "posix":
            os.chmod(fn, 0o600)           # 账本含应用与渠道名，目录收紧到本人可读
        return fn

    def read(self, fn):
        try:
            with open(fn, encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return None                   # 坏文件跳过，不挡后面的还原

    def latest(self, serial="", model=""):
        try:
            names = sorted(os.listdir(self.root), reverse=True)
        except OSError:
            return None
        for name in names:
            if not name.endswith(".json"):
                continue
            rec = self.read(os.path.join(self.root, name))
            if not isinstance(rec, dict):
                continue
            if serial and rec.get("serial") != serial:
                continue
            if not serial and model and rec.get("model") != model:
                continue
            return rec
        return None
