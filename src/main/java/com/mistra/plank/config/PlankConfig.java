package com.mistra.plank.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.mistra.plank.pojo.enums.BuyStrategyEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Mistra @ Version: 1.0 @ Time: 2021/11/18 21:44 @ Description: 配置文件 @ Copyright (c) Mistra,All Rights
 *         Reserved. @ Github: https://github.com/MistraR @ CSDN: https://blog.csdn.net/axela30w
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix = "plank")
public class PlankConfig {

    /**
     * 雪球 Cookie
     */
    private String xueQiuCookie;

    /**
     * 雪球 获取所有股票信息，每日更新成交量
     */
    private String xueQiuAllStockUrl;

    /**
     * 雪球 获取某只股票最近recentDayNumber天的每日涨跌记录url
     */
    private String xueQiuStockDetailUrl;

    /**
     * 雪球 某只股票当日资金流入趋势接口 {code}=SZ300750
     */
    private String todayFundTrendUrl;

    /**
     * 东财 抓取每日龙虎榜数据，只取净买入额前20
     */
    private String dragonListUrl;

    /**
     * 东财 抓取外资持仓数据
     */
    private String foreignShareholdingUrl;

    /**
     * 东财 抓取主力流入，5、10、20天连续流入数据，当天实时流入数据
     */
    private String mainFundUrl;

    /**
     * 东财 抓取从某天以来的龙虎榜数据
     */
    private Long dragonListTime;

    /**
     * 雪球 获取某只股票最近多少天的记录
     */
    private Integer recentDayNumber;

    /**
     * 起始资金
     */
    private Integer funds;

    /**
     * 选股策略
     */
    private BuyStrategyEnum buyStrategyEnum;

    /**
     * 止盈清仓比率
     */
    private BigDecimal profitUpperRatio;

    /**
     * 阶段止盈减半仓比率
     */
    private BigDecimal profitQuarterRatio;

    /**
     * 阶段止盈减半仓比率
     */
    private BigDecimal profitHalfRatio;

    /**
     * 阶段止盈回车清仓比率
     */
    private BigDecimal profitClearanceRatio;

    /**
     * 止损比率
     */
    private BigDecimal deficitRatio;

    /**
     * 止损均线
     */
    private Integer deficitMovingAverage;

    /**
     * 股价上限
     */
    private Integer stockPriceUpperLimit;

    /**
     * 股价下限
     */
    private Integer stockPriceLowerLimit;

    /**
     * 股价下限
     */
    private Integer clearanceDay;

    /**
     * 可打板涨幅比率
     */
    private BigDecimal buyPlankRatioLimit;

}
