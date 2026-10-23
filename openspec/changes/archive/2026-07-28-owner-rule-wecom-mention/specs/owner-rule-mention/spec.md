## ADDED Requirements

### Requirement: Resolve owner by region and system type rules

The system SHALL resolve a responsible owner for each low-SLI system using configurable rules derived from the owner roster. Matching MUST prefer region keyword hits, then apply trading vs bid-evaluation type constraints; rules without a type constraint MUST match by region alone. Xiong'an bid-evaluation systems MUST resolve to 刘彩霞; Xiong'an trading systems MUST resolve to 王华聪.

#### Scenario: Region and type both apply

- **WHEN** a system name contains a configured region keyword and indicates trading (交易)
- **THEN** the system MUST match a trading-scoped rule for that region when such a rule exists

#### Scenario: Region-only rule when type absent on rule

- **WHEN** a system name matches a region on a rule that has no type constraint
- **THEN** the system MAY be assigned that owner even if the system name has no trading/bid type token

#### Scenario: Xiong'an bid-evaluation owner

- **WHEN** a low-SLI system name is identified as Xiong'an (雄安) and bid-evaluation/open-bid type (开评标/开标/评标)
- **THEN** the resolved owner MUST be 刘彩霞 with mobile 15221477921

#### Scenario: Xiong'an trading owner

- **WHEN** a low-SLI system name is identified as Xiong'an (雄安) and trading type (交易)
- **THEN** the resolved owner MUST be 王华聪 with mobile 13521207069

#### Scenario: Unmatched system

- **WHEN** no rule matches a system
- **THEN** the system MUST remain in the report with a default owner label and MUST NOT contribute a mobile to the mention list

### Requirement: Mention matched owners on WeCom notify

When notifying via WeCom, the system SHALL send the summary Markdown and then a text message that @-mentions the distinct mobile numbers of owners resolved for systems in that report.

#### Scenario: Deduplicate mobiles

- **WHEN** multiple systems resolve to the same owner mobile
- **THEN** that mobile MUST appear at most once in `mentioned_mobile_list`

#### Scenario: Preview does not mention

- **WHEN** a client calls preview
- **THEN** the system MUST NOT send WeCom messages, and SHOULD still expose resolved owner name/mobile on each item for verification
