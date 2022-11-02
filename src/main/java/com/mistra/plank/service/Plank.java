package com.mistra.plank.service;

import java.util.Date;
import java.util.List;

import com.mistra.plank.model.entity.Stock;

/**
 * 定义自己的选股，买入，卖出策略
 *
 * @author mistra@future.com
 * @date 2022/6/14
 */
public interface Plank {

    /**
     * 检查可以买的票
     * 
     * @param date Date
     * @return List<Stock>
     */
    List<Stock> checkStock(Date date);

    /**
     * 买入股票
     * 
     * @param stocks stocks
     * @param date 买入日期
     * @param fundsPart 资金分层数
     */
    void buyStock(List<Stock> stocks, Date date, Integer fundsPart);

    /**
     * 卖出
     * 
     * @param date 开盘日期
     */
    void sellStock(Date date);

}
