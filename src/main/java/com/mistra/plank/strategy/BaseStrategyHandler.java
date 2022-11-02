package com.mistra.plank.strategy;


import com.mistra.plank.strategy.model.BaseStrategyInput;
import com.mistra.plank.model.vo.trade.TradeRuleVo;

public abstract class BaseStrategyHandler<I extends BaseStrategyInput, R> implements StrategyHandler {

    @Override
    public void handle(TradeRuleVo tradeRuleVo) {
        I input = queryInput(tradeRuleVo);
        R result = handle(input);
        handleResult(input, result);
    }

    public abstract I queryInput(TradeRuleVo tradeRuleVo);

    public abstract R handle(I input);

    public abstract void handleResult(I input, R result);

}
