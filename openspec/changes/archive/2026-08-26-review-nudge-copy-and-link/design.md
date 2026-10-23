## Context

未复盘催办已在同步成功后连续发 3 次 Markdown，并用 `mentioned_mobile_list` @ 责任人。正文目前写条数和 `count_sla`，没有复盘页链接。`review-sync` 已有 `ReviewSyncProperties`（enabled/cron/zone），适合挂页面地址。

## Goals / Non-Goals

**Goals:**

- 固定催办人话文案，去掉条数与内部字段名。
- 用 `[打开复盘页](url)` 指向可配置地址，默认内网 `http://10.32.201.201:8086/review/index.html`。

**Non-Goals:**

- 不改变触发时机、发送次数、@ 规则、incomplete 判定。
- 不改复盘页路由或静态资源路径。
- 不把责任人姓名写进 Markdown 正文（仍走现有 text @ 消息）。

## Decisions

1. **配置键 `review-sync.page-url`**  
   与同步/催办同一前缀，避免再开一组 properties。Java 字段 `pageUrl`，yml 默认填生产复盘页。

2. **链接行始终拼进 Markdown**  
   使用 `ReviewSyncProperties.getPageUrl()` trim 后拼 `[打开复盘页](<url>)`。默认有值；若运行时被配成空白，则省略链接行（仍发两行固定文案），避免非法 `[]()`。

3. **文案硬编码在 nudge 服务**  
   两行中文是产品固定句，不做成 i18n。只有 URL 进配置。

4. **企微 Markdown 链接**  
   群机器人支持 `[text](url)`；展示「打开复盘页」，href 为配置 URL。

## Risks / Trade-offs

- [默认 URL 写死内网 IP] → 换机改 yml；本地开发可把 `page-url` 改成 `http://localhost:8086/review/`。
- [企微对 markdown 链接支持因客户端而异] → 仍发完整 URL 作为 href；若点不开，至少 href 可复制。
- [条数不再出现在群消息] → 日志仍打 incomplete count，运营可看应用日志。

## Migration Plan

发版后重启即可。无需库变更。回滚：还原 Markdown 拼装与 yml 中的 `page-url` 行。

## Open Questions

无。空白 `page-url` 时省略链接行。
