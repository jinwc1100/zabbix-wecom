## 1. Configuration

- [x] 1.1 Update `wecom.webhook-url` in `application.yml` to the new key endpoint

## 2. Markdown hints

- [x] 2.1 Add shared footer in `SlaMarkdownFormatter` for subsystem →「SLA宕机明细」and parent →「SLA父系统宕机明细」
- [x] 2.2 Call the footer from both `formatDetail` and `formatWecomSummary`

## 3. Verification

- [x] 3.1 Rebuild/restart; preview and confirm both `markdown` and `wecomMarkdown` contain the hints
- [x] 3.2 Notify once to the new webhook and confirm delivery
- [x] 3.3 Briefly note the new webhook / hint behavior in README if needed
