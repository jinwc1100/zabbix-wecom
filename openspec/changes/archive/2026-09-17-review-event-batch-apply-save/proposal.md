## Why

复盘列表待填量大时，同类宕机仍须逐条填六项必填再点「保存本行」。责任人需要：先填一条样板，勾选其它行套用同一套分类与长文，再一次保存已填完的勾选行。必填门槛与排除表写入口径保持不变。

## What Changes

- 复盘列表（`events.html` 与对应 preview）增加本页勾选。
- 「套用样板」把样板行的故障类别、子类、描述、解决过程、根本原因、改进措施拷到勾选目标行（样板本身不作为覆盖目标）；`count_sla` 仍由字典随子类带出。套用只改本页内存，不写库。用户点「设为样板」后该行须持续铺满淡蓝底（含该行 select/textarea），不用品牌色左边条；「当前样板」为描边钮，不得与「查询/编辑」实心品牌蓝同色。未保存黄底不得盖过样板淡蓝。未点选时的默认样板不预亮。
- 「保存勾选」对勾选且六项已齐的行逐条走现有单行保存（含 `exclude_time` 与 `sla_excluded_downtime`）。不完整行跳过；部分失败时已成功的行保留，返回失败列表。
- 不降低必填；不在套用时写排除表；不引入 Excel 导入。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `sla-review-event-web`: 列表勾选、套用样板、批量保存勾选已填完行；单行保存规则不变。

## Impact

- 前端：`events.html`、`events-preview.html`。
- 后端：优先复用 `PUT /api/sla/review-events/{id}` 与 `ReviewEventService.update` / `ExcludedDowntimeWriteService.writeAfterSave`；若增加批量入口，仍须逐条调用同一套逻辑。
- 排除库、故障字典、分页 50/100、翻页丢未保存提示：行为不改口径。
