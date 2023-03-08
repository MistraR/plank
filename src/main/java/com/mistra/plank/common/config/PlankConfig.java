package com.mistra.plank.common.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Mistra @ Version: 1.0
 * @ Time: 2021/11/18 22:09
 * @ Description: 配置文件
 * @ Copyright (c) Mistra,All Rights Reserved
 * @ Github: https://github.com/MistraR
 * @ CSDN: https://blog.csdn.net/axela30w
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
     * 雪球 获取某只股票最近recentDayNumber天的每日涨跌记录url
     */
    private String xueQiuStockDetailUrl;

    /**
     * 雪球 获取某只股票当天实时价格,是否涨停等信息
     */
    private String xueQiuStockLimitUpPriceUrl;

    /**
     * 雪球 某只股票当日资金流入趋势接口 {code}=SZ300750
     */
    private String todayFundTrendUrl;

    /**
     * 东财 抓取外资持仓数据
     * https://data.eastmoney.com/hsgtcg/list.html?dtype=Y
     */
    private String foreignShareholdingUrl;

    /**
     * 东财 抓取主力流入，5、10、20天连续流入数据，当天实时流入数据
     */
    private String mainFundUrl;

    /**
     * 雪球 获取某只股票最近多少天的记录
     */
    private Integer recentDayNumber;

    /**
     * 打印日志时显示股票名称还是code
     */
    private Boolean printName;

    /**
     * 是否开启自动交易
     */
    private Boolean automaticTrading;

    /**
     * 自动交易止损比率
     */
    private Double stopLossRate;

    /**
     * 自动交易止盈比率
     */
    private Double takeProfitRate;

    /**
     * 每日自动交易买入金额上限
     */
    private Double automaticTradingMoneyLimitUp;

    /**
     * 自动打板单笔交易金额上限
     */
    private Integer singleTransactionLimitAmount;

    /**
     * 是否开启持仓监控
     */
    private Boolean enableMonitor;

    /**
     * 是否开启自动打板
     */
    private Boolean automaticPlankTrading;

    /**
     * 自动打板是否只打当日涨幅Top5版块的成分股
     */
    private Boolean automaticPlankTop5Bk;

    /**
     * 自动打板时间限制，一般只打早盘强势快速封板的 比如只打10点前封板的票
     */
    private Integer automaticPlankTradingTimeLimit;

    /**
     * 股票成交额过滤阈值，小成交额的不参与
     */
    private Double stockTurnoverThreshold;

    /**
     * 主力流入阈值
     */
    private Double mainFundThreshold;

    /**
     * 当日行业版块涨幅排行
     * http://quote.eastmoney.com/center/hsbk.html
     */
    private String industryBKUrl;

    /**
     * 当日概念版块涨幅排行
     */
    private String conceptBKUrl;

    /**
     * 更新股票所属版块
     */
    private String updateStockBkUrl;

}
