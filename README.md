# plank 涨停先锋

#### Introduction

- 自动交易：集成了东方财富的自动交易接口，代码搬运来自于：https://github.com/bosspen1/stock
- 抓取数据：A股每支股票每日的成交数据，涨跌幅度，最高最低价等等，每日的龙虎榜数据
- 监控数据：监控自己的持仓，重点关注的股票，主力实时流入
- 选股策略：找出爆量回踩的票，找出最近走上升趋势的股票等
- 晋级胜率：找出最近的连板股梯队，分析出连板股的晋级胜率，为打板提供依据

> 我是赛道股，趋势股，打板都玩，跟随资金，有炒大A的朋友可以加V(GODR3060W)交流.

> 运行之前研究一下配置文件里面的参数，特别是雪球的cookie需要隔断时间换一下

连板晋级率：

![avatar](./src/main/resources/img/1.png)

> 连板越高，晋级概率越大。三板定龙头是有道理的。当然，有可能很多都是一字板，排单都排不进去。

监控数据：

![avatar](./src/main/resources/img/3.png)

基金和外资持仓，季度增减仓数据： sql:src/main/resources/query.text
![avatar](./src/main/resources/img/4.png)