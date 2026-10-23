## 1. List API

- [x] 1.1 Add `countSla` to `ReviewEventQuery`, controller `GET /api/sla/review-events`, and `applyCountSla` (omit/blank/`all` unconstrained; `是`/`否` exact; else 400)
- [x] 1.2 Add mapper `AND count_sla = #{countSla}` when set; AND with existing filters including `reviewStatus`

## 2. List pages

- [x] 2.1 On `events.html` add 是否计算SLA 全部/是/否; default and reset 全部; send `countSla` on query; known URL keys include `countSla`
- [x] 2.2 Mirror the same control and local filter on `events-preview.html`

## 3. Tests

- [x] 3.1 Tests: valid `是`/`否`, invalid value 400, AND with `reviewStatus=incomplete` empty, page default/URL hydrate; `mvn test`
