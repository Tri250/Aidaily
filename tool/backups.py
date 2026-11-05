"""备份目录：命名、扫描、`<型号>-gui` 兜底各只有一处。

root 由调用方给 —— 客户端是 `<数据目录>/backups`，命令行是 `AAC_BACKUPS` 或仓库根的 `backups/`。
这里不读环境变量、不认得客户端，否则客户端进程（没有 `AAC_BACKUPS`）会算到仓库根上去。
"""
import datetime
import os


def root_from_env():
    """客户端把 `AAC_BACKUPS` 传给子进程；源码态直接跑 `tool/run.py` 时没有它。"""
    return os.environ.get("AAC_BACKUPS")


def _restrict(d):
    """备份里有应用清单和通知渠道记录（channels.tsv）：目录收紧到本人可读（M-12）。
    Windows 的 FAT/exFAT 不支持 POSIX 权限，忽略即可。"""
    if os.name == "posix":
        try:
            os.chmod(d, 0o700)
        except OSError:
            pass


class Backups:
    def __init__(self, root):
        self.root = os.fspath(root)

    def latest(self, model):
        """最近一份备份，没有则 None。按名字排序取最后一个，所以 `<型号>-gui` 优先于带时间的目录。"""
        try:
            hits = sorted(d for d in os.listdir(self.root) if d.startswith(model + "-"))
        except FileNotFoundError:
            return None
        return os.path.join(self.root, hits[-1]) if hits else None

    def create(self, model, when=None):
        d = os.path.join(self.root, f"{model}-{(when or datetime.datetime.now()):%Y%m%d-%H%M}")
        os.makedirs(d, exist_ok=True)
        _restrict(d)
        return d

    def gui(self, model):
        """客户端勾选营销通知时用：没跑过备份也要有地方放 channels.tsv。"""
        d = os.path.join(self.root, f"{model}-gui")
        os.makedirs(d, exist_ok=True)
        _restrict(d)
        return d
