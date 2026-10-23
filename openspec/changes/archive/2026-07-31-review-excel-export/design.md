## Context

复盘需要标准 Excel，而现网只有企微 markdown / preview JSON。已具备：双 SLI 源、子/父宕机看板、`OwnerRuleResolver`。缺口：全量年度清单查询、宕机「排除状态」、Excel 下载 API。业务已确认：页签名中的「月份」仅命名；导出全部「未排除」/`-` 宕机行；责任人写现有规则下的具体姓名。

## Goals / Non-Goals

**Goals:**

- `GET`（或等价）API 下载双页签 xlsx。
- 页签1：年度 SLI &lt; 99.9% × 未排除/`-` 宕机。
- 页签2：年度清单全量 × 未排除/`-` 宕机。
- 字段固定六列；中断原因为空；类别=SLI 源层级；责任人=现有解析器姓名。

**Non-Goals:**

- 不按宕机开始时间做自然月过滤。
- 不改企微推送/定时。
- 不预填 Grafana「探测失败具体原因」到中断原因。
- 不做上传回写中断原因。

## Decisions

### 1. API

```
GET /api/sla/review-excel?month=2026-07
```

- `month` 可选，格式 `yyyy-MM`，缺省当前东八区年月；只用于页签名与下载文件名。
- 响应：`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` + `Content-Disposition: attachment`.

### 2. 数据流

```
login Grafana
  → collectAnnualInventory (全量，带 SLI + sourceLevel)
  → loadLeaf/Parent downtime (含排除状态)
  → sheet1 = inventory.filter(sli<99.9) ▹ expandUnexcludedDowntime
  → sheet2 = inventory ▹ expandUnexcludedDowntime
  → resolve owner per system name
  → write xlsx
```

- `AnnualSliService` 增加「全量」查询（或 `queryAllRows` + 本地不过滤 / 包装 SQL 去掉 threshold）；低 SLI 页签再 filter。
- 宕机：扩展 `DowntimeEvent.exclusionStatus`；配置 `exclusion-column: 排除状态`；允许值 `未排除`、`-`（trim 后相等）。
- 匹配：按 `sourceLevel` 查对应索引（与方案 B 一致）；无宕机则该系统不出现行。

### 3. Excel 库

- 引入 **Apache POI**（与 Spring Boot 2.7 / Java 8 常见组合）或 EasyExcel；推荐 POI `poi-ooxml` 精简写入即可。

### 4. 列与页签

| 列 | 内容 |
|----|------|
| 系统名称 | 清单名 |
| 系统类别 | `父系统` / `子系统` |
| 中断原因 | `""` |
| 宕机开始时间 | 看板开始展示值 |
| 宕机结束时间 | 看板结束/恢复展示值（含「未恢复」原文若存在） |
| 责任人 | `ResolvedOwner.getName()` |

页签名：`{yyyy-MM}-SLA小于99.9%待复盘`、`{yyyy-MM}-所有待复盘`。

### 5. 父看板排除列

- apply 时确认父系统宕机 SQL 是否含「排除状态」；若列名不同则配置化 `exclusion-column`。

## Risks / Trade-offs

- **[Risk] 全量+全历史未排除行导致文件很大 / 查询慢** → Mitigation：沿用现有 dashboard SQL 范围；必要时后续加可选时间窗参数（本变更不做）。
- **[Risk] 父看板无排除状态列** → Mitigation：spike；缺列则该源无法过滤或视为不导出。
- **[Trade-off] 页签含「月份」但不按月过滤** → 已按业务确认；文档/README 写清以免误解。

## Migration Plan

1. 加依赖与配置列。  
2. 实现 inventory 全量 + exclusion + Excel API。  
3. 用浏览器/curl 下载抽查两页签。  
4. 回滚：移除 API 与依赖即可。

## Open Questions

- （无）月份命名不滤数据、责任人写姓名已确认。
