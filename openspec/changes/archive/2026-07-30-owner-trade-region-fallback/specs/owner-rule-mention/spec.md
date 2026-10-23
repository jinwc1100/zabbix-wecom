## ADDED Requirements

### Requirement: Trade-owner region fallback for non-bid or unmatched systems

After applying the primary region and type rules, the system SHALL apply a trade-owner region fallback when either (a) the system is not identified as bid-evaluation type (开评标/开标/评标/编标), or (b) primary matching failed. The fallback MUST select the highest-priority rule that hits a region keyword in the system name and is eligible as a trade/non-bid fallback candidate (rules constrained to TRADE, or rules with empty types that represent regional non-bid owners). The fallback MUST match by region keyword alone and MUST NOT require the system name to contain the token `交易`. Successfully matched bid-evaluation systems MUST NOT have their owner replaced by this fallback.

#### Scenario: Xiong'an portal falls back to Wang Huacong

- **WHEN** the system name is `雄安门户网站` (or otherwise contains 雄安 without bid-evaluation tokens)
- **THEN** the resolved owner MUST be 王华聪 with mobile 13521207069

#### Scenario: Xiong'an supervision platform falls back to Wang Huacong

- **WHEN** the system name is `雄安监督平台`
- **THEN** the resolved owner MUST be 王华聪 with mobile 13521207069

#### Scenario: Bid-evaluation Xiong'an still uses Liu Caixia

- **WHEN** a system name contains 雄安 and bid-evaluation tokens (开评标/开标/评标)
- **THEN** the resolved owner MUST remain 刘彩霞 and MUST NOT be replaced by the trade region fallback

#### Scenario: Fallback still fails

- **WHEN** primary matching fails and no trade/non-bid fallback rule hits any region in the system name
- **THEN** the system MUST remain unmatched (default owner label, no mobile for @)

## MODIFIED Requirements

### Requirement: Resolve owner by region and system type rules

The system SHALL resolve a responsible owner for each low-SLI system using configurable rules derived from the owner roster. Matching MUST prefer region keyword hits, then apply trading vs bid-evaluation type constraints; rules without a type constraint MUST match by region alone. Xiong'an bid-evaluation systems MUST resolve to 刘彩霞; Xiong'an trading systems MUST resolve to 王华聪. When primary matching does not yield an owner for a non-bid system (or matching fails), the trade-owner region fallback requirement MUST apply before treating the system as unmatched.

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

#### Scenario: Unmatched system after fallback

- **WHEN** no primary rule and no trade region fallback rule matches a system
- **THEN** the system MUST remain in the report with a default owner label and MUST NOT contribute a mobile to the mention list
