## Why

总览月度低 SLA 卡把 Grafana 列配成不存在的 `月度SLI`，查询 400，排行空白。真实看板与年度同源，月度是当年 `一月`…`十二月` 列，需要按当月展示并允许切换，而不是假列名或每月改 yml。

## What Changes

- 月度采集改为读约定中文月份列（`一月`…`十二月`），不再使用 `sli-column: 月度SLI`；`monthly-sli-sources` 只保留 UID、面板、层级、名称列。
- 打开总览默认当前自然月（`Asia/Shanghai`）；父/子两卡共用一个月份选择；文案带看板年，例如「2026年八月」。
- 未来月在下拉中禁用；低 SLA 表只列入能解析为数字且低于阈值的行；无数字（含 Grafana 的 `-`）不进排行、不当 0。
- 该月有数但全部达标：空态（无低于阈值系统）。该月全员无数据：卡内空态，可用 `--` 表示无月度数据，表体不得出现 SLI% 为 `--` 的排名行。
- 老版/新版面板与年度相同：不按系统名去重，同一系统可出现两行。
- 一次 Grafana 全表，内存按月切片；notify / Excel / `sli-sources` 不变。错误隔离（`monthlyGrafanaError`）保留。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `review-overview-hub`: 月度排行改为当年月份列 + 当月默认 + 可选月份；过滤与空态规则如上；配置不再依赖 `月度SLI` 列名。

## Impact

- `application.yml` `grafana.monthly-sli-sources`（去掉月度 `sli-column`）。
- `AnnualSliService` / `GrafanaClient` 月度采集；`ReviewOverview` JSON（月份元数据与按月列表）。
- `/review/index.html` 月份下拉与空态文案。
- 不改企微推送、Excel、日同步。
