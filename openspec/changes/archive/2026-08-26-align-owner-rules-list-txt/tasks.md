## 1. Owner YAML

- [x] 1.1 Narrow 王立新 to `北京综合交易` and `北京工程建设`, `types: ["TRADE"]`, priority above the generic Beijing trade rule; remove `北京`, `北京市级`, `亦庄`
- [x] 1.2 Add 刘彩霞 BID regions `密云`, `门头沟`; add 胡一奇 BID region `亦庄`
- [x] 1.3 Add 王华聪 TRADE regions `北京`, `亦庄`, `门头沟`, `密云` (keep existing 雄安工程交易 / 石家庄工程开评标 rules)
- [x] 1.4 Add 张洪涛 rule: region `音视频`, empty types, mobile `18210189117`

## 2. Tests and docs

- [x] 2.1 Extend `OwnerRuleResolverTest` (load real-shaped rules or the new Beijing/audio cases): 密云/门头沟 BID→刘彩霞；亦庄 BID→胡一奇；北京/亦庄 TRADE→王华聪；北京综合交易与北京工程建设交易→王立新；音视频→张洪涛
- [x] 2.2 Adjust README owner examples if they still say all Beijing/Yizhuang non-bid → 王立新
