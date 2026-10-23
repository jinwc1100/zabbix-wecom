## 1. Rules configuration

- [x] 1.1 Encode `doc/list.txt` into YAML owner rules (regions, types, name, mobile, priority)
- [x] 1.2 Add explicit rules: 雄安+开评标→刘彩霞；雄安+交易→王华聪；keep 石家庄工程开评标 / 其它名单条目
- [x] 1.3 Bind `OwnerRuleProperties` (or extend owners config) and load at startup

## 2. Matching engine

- [x] 2.1 Implement system-name parsers for region hit and type (交易 / 开评标 / NONE)
- [x] 2.2 Implement rule resolver with priority: specific → region+type → region-only; unmatched → default without mobile
- [x] 2.3 Attach `ownerName` / `ownerMobile` onto each notify item during report build

## 3. WeCom mention wiring

- [x] 3.1 Collect distinct owner mobiles from report items in `notifyNow`
- [x] 3.2 After summary markdown, send text with `mentioned_mobile_list` (skip if empty)
- [x] 3.3 Ensure preview returns owner fields but does not call WeCom

## 4. Verification

- [x] 4.1 Preview sample systems (江门/大同交易 vs 开评标/雄安开标 vs 雄安交易) and confirm owners
- [x] 4.2 Notify once and confirm group @ uses expected mobiles
- [x] 4.3 Update README with rule matching and @ behavior
