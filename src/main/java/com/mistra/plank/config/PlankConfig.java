package com.mistra.plank.config;


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
}
