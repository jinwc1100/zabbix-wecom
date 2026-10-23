## Why

月末定时/手工企微复盘推送会反复提醒年度 SLI &lt; 99.9% 的系统；其中部分已在 Grafana 宕机明细里做了排除（人工复盘），继续推送造成噪音。需要用看板「排除状态 / 排除原因」识别本月已全复盘的系统，推送时跳过，下月自然重新评估。

## What Changes

- 宕机查询补充加载「排除原因」（「排除状态」已有）；配置化 `reason-column`（默认 `排除原因`）。
- 按东八区**自然月**筛选当月宕机，判定单条是否「未复盘」：`排除状态 = 未排除` **或** `排除原因 = -`（满足其一即未复盘）。
- **企微推送**（定时 + 手工 `POST /notify`）：当月存在宕机且**全部已复盘** → 不推该系统；**无当月宕机**但 SLI &lt; 99.9% → **仍推**；当月有任一条未复盘 → 仍推。
- **`GET /preview` 不过滤**：低 SLI 系统仍全部返回；宜标注本月已复盘/不推送状态便于核对。
- 不改复盘 Excel 导出规则；不引入独立静默名单。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `sla-downtime-notify`: 增加基于当月排除字段的已复盘判定；notify/定时过滤已全复盘系统；preview 保留全量并可暴露复盘状态。

## Impact

- 代码：`DowntimeEvent` / `DowntimeQueryService` / `GrafanaProperties`、`DowntimeMatchService` 或 notify 组装层、`SlaReviewNotifyService`、`SlaMarkdownFormatter`（preview 标注可选）、`DowntimeNotifyItem`。
- 配置：`grafana.downtime-*.reason-column`。
- API：`preview` 响应字段可能增加；`notify` 实际推送集合变小。
- 无新依赖。
