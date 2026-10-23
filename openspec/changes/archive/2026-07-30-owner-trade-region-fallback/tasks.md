## 1. Resolver fallback

- [x] 1.1 Update `OwnerRuleResolver` to keep primary BID matches; on non-bid or unmatched, fall back to TRADE / empty-types rules by region (ignore system type token for fallback)
- [x] 1.2 Ensure fallback does not require `交易` in the system name
- [x] 1.3 Add unit tests: `雄安门户网站` / `雄安监督平台` → 王华聪; 雄安开评标 → 刘彩霞; no region → unmatched

## 2. Docs

- [x] 2.1 Document trade region fallback in README (and briefly in `doc/list.txt` if needed)

## 3. Verify

- [x] 3.1 Rebuild; preview (or unit-only) confirm the two Xiong'an systems resolve to 王华聪 mobile for @
- [x] 3.2 Confirm an existing BID Xiong'an-style name still resolves to 刘彩霞
