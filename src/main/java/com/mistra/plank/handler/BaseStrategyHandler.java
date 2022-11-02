package com.mistra.plank.handler;


import com.mistra.plank.handler.model.BaseStrategyInput;
import com.mistra.plank.pojo.vo.trade.TradeRuleVo;

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
