package com.mistra.plank.config;


import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Mistra
 * @ Version: 1.0
 * @ Time: 2021/11/18 21:44
 * @ Description:
 * @ Copyright (c) Mistra,All Rights Reserved.
 * @ Github: https://github.com/MistraR
 * @ CSDN: https://blog.csdn.net/axela30w
 */
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
     * 雪球 获取某只股票最近多少天的记录
     */
    private Integer recentDayNumber;

    /**
     * 开始日期
     */
    private Long beginDay;

    /**
     * 起始资金
     */
    private Integer funds;

    /**
     * 资金分层数
     */
    private Integer fundsPart;

    /**
     * 每日仓位层数上限
     */
    private Integer fundsPartLimit;

    /**
     * 止盈清仓比率
     */
    private BigDecimal profitRatio;

    /**
     * 阶段止盈减半仓比率
     */
    private BigDecimal profitHalfRatio;

    /**
     * 止损比率
     */
    private BigDecimal deficitRatio;

    /**
     * 止损均线
     */
    private Integer deficitMovingAverage;

    /**
     * 介入比率下限 2日涨幅
     */
    private BigDecimal joinIncreaseRatioLowerLimit;

    /**
     * 介入比率上限 6日涨幅
     */
    private BigDecimal joinIncreaseRatioUpperLimit;

    /**
     * 可打板涨幅比率
     */
    private BigDecimal buyRatioLimit;

    /**
     * 股价上限
     */
    private Integer stockUpperLimit;

    /**
     * 股价下限
     */
    private Integer stockLowerLimit;

    public String getXueQiuCookie() {
        return xueQiuCookie;
    }

    public void setXueQiuCookie(String xueQiuCookie) {
        this.xueQiuCookie = xueQiuCookie;
    }

    public String getXueQiuAllStockUrl() {
        return xueQiuAllStockUrl;
    }

    public void setXueQiuAllStockUrl(String xueQiuAllStockUrl) {
        this.xueQiuAllStockUrl = xueQiuAllStockUrl;
    }

    public String getXueQiuStockDetailUrl() {
        return xueQiuStockDetailUrl;
    }

    public void setXueQiuStockDetailUrl(String xueQiuStockDetailUrl) {
        this.xueQiuStockDetailUrl = xueQiuStockDetailUrl;
    }

    public Integer getRecentDayNumber() {
        return recentDayNumber;
    }

    public void setRecentDayNumber(Integer recentDayNumber) {
        this.recentDayNumber = recentDayNumber;
    }

    public Integer getFunds() {
        return funds;
    }

    public void setFunds(Integer funds) {
        this.funds = funds;
    }

    public Integer getFundsPart() {
        return fundsPart;
    }

    public void setFundsPart(Integer fundsPart) {
        this.fundsPart = fundsPart;
    }

    public Integer getFundsPartLimit() {
        return fundsPartLimit;
    }

    public void setFundsPartLimit(Integer fundsPartLimit) {
        this.fundsPartLimit = fundsPartLimit;
    }

    public BigDecimal getProfitRatio() {
        return profitRatio;
    }

    public void setProfitRatio(BigDecimal profitRatio) {
        this.profitRatio = profitRatio;
    }

    public BigDecimal getProfitHalfRatio() {
        return profitHalfRatio;
    }

    public void setProfitHalfRatio(BigDecimal profitHalfRatio) {
        this.profitHalfRatio = profitHalfRatio;
    }

    public BigDecimal getDeficitRatio() {
        return deficitRatio;
    }

    public void setDeficitRatio(BigDecimal deficitRatio) {
        this.deficitRatio = deficitRatio;
    }

    public Integer getDeficitMovingAverage() {
        return deficitMovingAverage;
    }

    public void setDeficitMovingAverage(Integer deficitMovingAverage) {
        this.deficitMovingAverage = deficitMovingAverage;
    }

    public BigDecimal getJoinIncreaseRatioLowerLimit() {
        return joinIncreaseRatioLowerLimit;
    }

    public void setJoinIncreaseRatioLowerLimit(BigDecimal joinIncreaseRatioLowerLimit) {
        this.joinIncreaseRatioLowerLimit = joinIncreaseRatioLowerLimit;
    }

    public BigDecimal getJoinIncreaseRatioUpperLimit() {
        return joinIncreaseRatioUpperLimit;
    }

    public void setJoinIncreaseRatioUpperLimit(BigDecimal joinIncreaseRatioUpperLimit) {
        this.joinIncreaseRatioUpperLimit = joinIncreaseRatioUpperLimit;
    }

    public Long getBeginDay() {
        return beginDay;
    }

    public void setBeginDay(Long beginDay) {
        this.beginDay = beginDay;
    }

    public Integer getStockUpperLimit() {
        return stockUpperLimit;
    }

    public void setStockUpperLimit(Integer stockUpperLimit) {
        this.stockUpperLimit = stockUpperLimit;
    }

    public Integer getStockLowerLimit() {
        return stockLowerLimit;
    }

    public void setStockLowerLimit(Integer stockLowerLimit) {
        this.stockLowerLimit = stockLowerLimit;
    }

    public BigDecimal getBuyRatioLimit() {
        return buyRatioLimit;
    }

    public void setBuyRatioLimit(BigDecimal buyRatioLimit) {
        this.buyRatioLimit = buyRatioLimit;
    }
}
