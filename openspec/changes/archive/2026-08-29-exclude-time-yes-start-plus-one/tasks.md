## 1. Derivation

- [x] 1.1 Change `ExcludeTimes.derive` so `是` returns `down_start.plusSeconds(1)` without clamping to `down_end` (keep `否` / blank / null behavior)
- [x] 1.2 Update `updateReviewFields` and `refreshExcludeTime` SQL: `是` uses `DATE_ADD(down_start, INTERVAL 1 SECOND)` only (remove `LEAST` vs `down_end`)

## 2. Tests

- [x] 2.1 Replace `ExcludeTimesTest` clamp cases: `是` with duration ≤ 1s MUST be start+1s
- [x] 2.2 Update mapper tests that assert `LEAST` / clamp wording for `refreshExcludeTime` and `updateReviewFields`

## 3. Verify

- [x] 3.1 Run the notifier unit tests covering ExcludeTimes, mapper XML, and review-event sync flags
