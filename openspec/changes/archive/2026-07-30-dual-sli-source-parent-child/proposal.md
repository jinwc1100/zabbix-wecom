## Why

原先只从一个年度看板拉取低 SLI 系统，无法覆盖「父系统 SLA 明细」与「子系统 SLA」分看板口径。业务需要两路均按年度 SLI &lt; 99.9% 过滤后合计进复盘清单，并以 SLI 来源直接标定父/子层级，避免无宕机命中时大量「未知」。

## What Changes

- 新增父系统 SLA 看板源：`aftk5ryifcfeof`；保留子系统 SLA 看板源：`afj5c5o5v3rb4f`。
- 两源各自过滤 `年度SLI < 99.9`，合并系统名单进入现有推送链路。
- **方案 B**：系统层级优先取自 SLI 来源配置（父系统 / 子系统）；宕机明细仍按层级对应的宕机看板匹配展示时段，不改变企微精简字段形态与 @ 责任人逻辑。
- 配置由单一 `dashboard-uid` + `panels` 改为可配置的多 `sli-sources`。
- 老版/新版面板仍按各源配置扫描；名称不跨源重复，不做去重合并。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `sla-downtime-notify`: 低 SLI 名单改为父+子双看板合计；展示层级以 SLI 来源为准（方案 B），并与对应宕机明细关联。

## Impact

- 配置：`application.yml` → `grafana.sli-sources`（及父看板 panel id / 列名）
- 代码：`GrafanaProperties`、`AnnualSliService`、`SlaLowSliItem`、`DowntimeMatchService`（层级来源策略）
- 运维：改配置后需重启；用 preview 核对两源条数之和与层级标签
- 不变：企微 markdown 三字段、文末指引、责任人 @、定时门控、宕机看板 UID
