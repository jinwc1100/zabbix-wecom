## Context

`AnnualSliService` 目前只读 `grafana.dashboard-uid`（`afj5c5o5v3rb4f`）上的 panel 4/2，过滤 `年度SLI < 99.9`。`DowntimeMatchService` 用宕机命中结果决定 `父系统/子系统/未知`。业务现要求：

- 子系统 SLA：`afj5c5o5v3rb4f`
- 父系统 SLA：`aftk5ryifcfeof`
- 两路过滤后合计；**方案 B**：层级以 SLI 来源为准
- 企微字段、@、定时、宕机看板 UID 保持不变

## Goals / Non-Goals

**Goals:**

- 可配置多 SLI 源（dashboard + panels + system-level + 名称列候选）。
- 合并低 SLI 名单；每条携带 `sourceLevel`。
- 匹配宕机时按 `sourceLevel` 优先查对应宕机索引；层级不被「无宕机」打成未知。
- 预览/推送仍输出名称、SLI、层级（及明细侧宕机时段）。

**Non-Goals:**

- 不改责任人规则、webhook、文末指引、调度 cron。
- 不做跨源按名称去重。
- 不实现领导全量汇总报表（另案）。
- 不强制一次改自然月近窗（保持现有 recent-window-days，除非另提）。

## Decisions

### 1. 配置结构：`grafana.sli-sources[]`

```yaml
grafana:
  sli-threshold: 99.9
  sli-sources:
    - name: "子系统SLA"
      dashboard-uid: afj5c5o5v3rb4f
      system-level: "子系统"
      name-columns: ["SLA名称", "服务名称", "子系统", "名称"]
      panels:
        - id: 4
          name: "老版监控"
        - id: 2
          name: "新版监控"
    - name: "父系统SLA"
      dashboard-uid: aftk5ryifcfeof
      system-level: "父系统"
      name-columns: ["父系统名称", "SLA名称", "服务名称", "名称"]
      panels:
        - id: 4   # apply 时对照 Grafana 确认
          name: "老版监控"
        - id: 2
          name: "新版监控"
```

- 废弃单一 `dashboard-uid`/`panels` 作为权威来源；若保留仅作兼容回退（可选：无 `sli-sources` 时回退旧字段）。
- **选择理由**：两源 panel/列名可能不同，列表配置最清晰。

### 2. 方案 B 层级策略

```
SlaLowSliItem.sourceLevel = source.system-level
        │
        ▼
DowntimeMatchService
  if sourceLevel == 父系统 → 只查 parentIndex（有则挂 recent/historical）
  if sourceLevel == 子系统 → 只查 leafIndex
  level 输出 = sourceLevel（始终）
  无宕机 → remark-none，level 不变
```

- **不再**用「同时命中父+子宕机」来改层级；因 SLI 源已声明身份，且名称不撞车。
- 若对侧索引偶发同名命中：忽略，只信与 `sourceLevel` 对应的索引。

### 3. 查询复用

- 继续 `GrafanaClient.queryLowSliRows`（``WHERE `年度SLI` < threshold``）。
- 若父看板年度列名不同：扩展 client 或 per-source `sli-column`（默认 `年度SLI`）；apply 前 spike确认。

### 4. 排序与计数

- 合并后仍按年度 SLI 升序。
- `NotifyResult` 父/子计数按最终 `systemLevel`（即 sourceLevel）统计。

## Risks / Trade-offs

- **[Risk] 父看板 panel id / 列名与假设不符 → 0 条** → Mitigation：apply 时先拉 dashboard JSON 确认；日志打印每源条数。
- **[Risk] 父看板 SQL 无 `年度SLI` 列 → 包装查询失败** → Mitigation：spike 后配置 `sli-column` 或改包装 SQL。
- **[Trade-off] 放弃宕机反推层级** → 无宕机时层级仍正确，符合方案 B；与旧「未知」行为不同（有意为之）。

## Migration Plan

1. 确认 `aftk5ryifcfeof` panel id 与列名，写入 `application.yml`。
2. 实现多源收集 + 匹配策略，打包重启。
3. `GET /api/sla/preview`：核对两源均有贡献、层级标签正确、条数≈两源之和。
4. 回滚：临时只配一个 `sli-sources` 即可退回单源。

## Open Questions

- 父看板 panel id 是否仍为 4/2：apply 阶段对照 Grafana 确认后写入配置（不阻塞本设计）。
