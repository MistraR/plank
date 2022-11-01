package com.mistra.plank.pojo.vo.trade;

import com.mistra.plank.pojo.entity.TradeRule;

public class TradeRuleVo extends TradeRule {

    private static final long serialVersionUID = 1L;

    private String stockName;
    private String strategyName;
    private String strategyBeanName;

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String getStrategyBeanName() {
        return strategyBeanName;
    }

    public void setStrategyBeanName(String strategyBeanName) {
        this.strategyBeanName = strategyBeanName;
    }

}
