## ADDED Requirements

### Requirement: Beijing roster split by bid vs trade vs two municipal names

Configured owner rules derived from `doc/list.txt` MUST assign Beijing systems as follows. Bid-evaluation type (开评标/开标/评标/编标) in 密云 or 门头沟 MUST resolve to 刘彩霞 (`15221477921`). Bid-evaluation type in 亦庄 MUST resolve to 胡一奇 (`15067116115`). Trading type whose name contains `北京综合交易` or `北京工程建设` MUST resolve to 王立新 (`13567174671`). Any other trading type whose name contains `北京`, or contains `亦庄` / `门头沟` / `密云` as district tokens, MUST resolve to 王华聪 (`13521207069`). A generic region keyword `北京` MUST NOT appear on 王立新. A generic `北京` keyword on 王华聪 MUST be TRADE-constrained (not empty types). 王立新's two municipal keywords MUST be TRADE-constrained and MUST have higher priority than 王华聪's `北京` TRADE rule so `北京综合交易平台` is not stolen.

#### Scenario: Miyun bid-evaluation is Liu Caixia

- **WHEN** a system name contains `密云` and bid-evaluation tokens
- **THEN** the resolved owner MUST be 刘彩霞 with mobile 15221477921

#### Scenario: Mentougou bid-evaluation is Liu Caixia

- **WHEN** a system name contains `门头沟` and bid-evaluation tokens
- **THEN** the resolved owner MUST be 刘彩霞 with mobile 15221477921

#### Scenario: Yizhuang bid-evaluation is Hu Yiqi

- **WHEN** a system name contains `亦庄` and bid-evaluation tokens
- **THEN** the resolved owner MUST be 胡一奇 with mobile 15067116115

#### Scenario: Beijing district trading is Wang Huacong

- **WHEN** a system name contains `北京` and trading tokens but does not contain `北京综合交易` or `北京工程建设`
- **THEN** the resolved owner MUST be 王华聪 with mobile 13521207069

#### Scenario: Yizhuang trading is Wang Huacong

- **WHEN** a system name contains `亦庄` and trading tokens and is not bid-evaluation type
- **THEN** the resolved owner MUST be 王华聪 with mobile 13521207069

#### Scenario: Municipal comprehensive trading is Wang Lixin

- **WHEN** a system name contains `北京综合交易`
- **THEN** the resolved owner MUST be 王立新 with mobile 13567174671

#### Scenario: Municipal engineering construction is Wang Lixin

- **WHEN** a system name contains `北京工程建设` and is trading type
- **THEN** the resolved owner MUST be 王立新 with mobile 13567174671

### Requirement: Audio-video systems resolve to Zhang Hongtao

The system SHALL resolve a system whose name contains `音视频` to 张洪涛 with mobile `18210189117`.

#### Scenario: Audio-video name matches Zhang Hongtao

- **WHEN** the system name contains `音视频`
- **THEN** the resolved owner MUST be 张洪涛 with mobile 18210189117
