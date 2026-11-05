# 规则包底座：纯数据 RulePack + 字段白名单校验 + SHA-256 清单，脚本仍是唯一被执行的来源

基线 `8ddcd67`（v0.3.2）。

机型规则目前长在 35 个目录的 Python 脚本里（38 个 recipe 文件）。改动规则 =
改代码、发版、用户更新客户端，社区想加一个开关要等维护者。2.0 规划（M-02）
要求把规则变成带签名校验的数据包；本 ADR 定底座，不含行为迁移。

## Decision

**新增 `tool/rulepack.py`（schema / 校验 / 完整性 / 加载）与
`tool/rulepack_migrate.py`（脚本元数据 → 草案）。引擎本版不改用规则包。**

- 包是纯数据：`pack_id / schema_version / brand / models / build_patterns /
  segments / evidence`。字段白名单：未知字段、缺字段、错类型一律
  `RulePackError`——白名单 + 类型收死在标量 / 字符串列表，"夹带代码"没有
  能放进来的字段。
- segment 必须有 `name / kind / safe_mode`；`targets / keep / protected` 是
  字符串列表、允许为空（迁移草案只有步骤名，没有目标数据）。
- `verify_manifest()` 按 `<sha256>  <相对路径>` 清单核对包文件；路径越界
  （绝对路径、`..`）直接拒绝。它防分发链路上的截断与错包，不防持钥者——
  非对称签名等代码签名凭据就绪后接入，接口不变。
- `load()` 任何一步失败都抛错，由调用方回落内建机型脚本。**脚本永远保留，
  规则包是叠加不是替换**（FR-03 的立场；回落路径与 `--ref` 安全模式同构）。
- 迁移工具只迁数据：`MODELS / BUILD / @step` 变成包字段，草案全部标
  `safe_mode=unverified`，evidence 里写明"行为未经真机对拍"。体检真实发现：
  `honor-PNM-AN10` 没记录 BUILD，列为待人工确认——这正是体检该说的话。

## Considered Options

- **本版就让引擎吃规则包**：37 份脚本的行为对拍（FR-03）没有真机阵列，
  仓促切换等于把"数据驱动"变成"第二套真相"。底座先行、对拍在 2.0.0 正式版，
  与规划排期一致。
- **schema 用 JSON Schema 库**：多一个依赖、Windows 打包多一份体积；白名单
  校验器百行内写完且错误文案是给用户看的人话。
- **迁移连 targets 一起抽**：targets 长在各步骤函数体内，静态抽取必然不完整，
  抽出来的清单会以"看起来全"的方式误导对拍。草案宁缺。
- **草案提交进仓库**：它们是体检产出不是适配成果，提交会造成"已迁移"的错觉。
  由 `rulepack_migrate.py --out` 按需产出，测试验证 35 份草案全部过校验。

## Consequences

- `packs/` 分发目录、对拍工具、引擎加载器是后续 ADR；本底座的
  `load() / build_matches() / segments_for()` 为它们预留了稳定入口。
- `BUILD` 前缀比较与 `build_ok()` 语义对齐（`build_patterns` 任一命中 build /
  os 前缀；空列表 = 不限版本，同脚本 `BUILD=""`）。
- 加新字段必须同时改 `PACK_FIELDS / SEGMENT_FIELDS` 与测试——白名单的
  代价，换来"包里不可能藏可执行内容"。
