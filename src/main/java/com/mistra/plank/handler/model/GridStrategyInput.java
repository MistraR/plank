package com.mistra.plank.handler.model;

import com.mistra.plank.api.response.GetDealDataResponse;
import com.mistra.plank.pojo.entity.TradeOrder;
import com.mistra.plank.pojo.vo.trade.TradeRuleVo;

import java.util.List;

public class GridStrategyInput extends BaseStrategyInput {

    public GridStrategyInput(TradeRuleVo tradeRuleVo) {
        super(tradeRuleVo);
    }

    private List<GetDealDataResponse> dealDataList;

    private List<TradeOrder> tradeOrderList;

    public List<GetDealDataResponse> getDealDataList() {
        return dealDataList;
    }

    public void setDealDataList(List<GetDealDataResponse> dealDataList) {
        this.dealDataList = dealDataList;
    }

    public List<TradeOrder> getTradeOrderList() {
        return tradeOrderList;
    }

    public void setTradeOrderList(List<TradeOrder> tradeOrderList) {
        this.tradeOrderList = tradeOrderList;
    }

}
