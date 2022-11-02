package com.mistra.plank.handler.model;


import com.mistra.plank.pojo.vo.trade.TradeRuleVo;

public class BaseStrategyInput {

    private TradeRuleVo tradeRuleVo;

    public BaseStrategyInput(TradeRuleVo tradeRuleVo) {
        this.tradeRuleVo = tradeRuleVo;
    }

    public TradeRuleVo getTradeRuleVo() {
        return tradeRuleVo;
    }

    public int getUserId() {
        return tradeRuleVo.getUserId();
    }

}
