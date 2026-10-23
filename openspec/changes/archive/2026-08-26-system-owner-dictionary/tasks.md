## 1. Schema and seed

- [x] 1.1 Add `sla_system_owner` CREATE TABLE (unique `(system_level, system_name)`) to MyBatis schema mapper and `schema-sla-review.sql`
- [x] 1.2 Ship `responsible_persons.xlsx` on the classpath if startup cannot read `doc/`; parse columns 子系统/父系统, 系统名称, 责任人, 手机号码
- [x] 1.3 Startup: create table, `INSERT IGNORE` seed rows; do not overwrite existing dictionary rows
- [x] 1.4 Mapper: list-all and/or get-by `(system_level, system_name)`; lookup mobile by `owner_name`

## 2. Resolve from dictionary

- [x] 2.1 Replace `OwnerRuleResolver` yml rules with dictionary lookup on trimmed level + name; callers pass `systemLevel` (notify, weekly collect, review Excel)
- [x] 2.2 Incomplete-review nudge maps `owner_name` → mobile from dictionary, not `owners.rules`
- [x] 2.3 Remove `owners.rules` (and unused owner rule properties) from `application.yml`

## 3. Tests and docs

- [x] 3.1 Replace `OwnerRuleResolverTest` region/type cases with dictionary fixtures (hit, distinct level, same name)
- [x] 3.2 Update nudge / review-sync tests that stubbed `OwnerRuleProperties`
- [x] 3.3 README: dictionary table, Excel seed, `refreshOwner` still only updates existing `owner_name`; drop yml rule docs
