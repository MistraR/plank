package com.mistra.plank.strategy;


import com.mistra.plank.model.vo.trade.TradeRuleVo;

public interface StrategyHandler {

    void handle(TradeRuleVo tradeRuleVo);

}
