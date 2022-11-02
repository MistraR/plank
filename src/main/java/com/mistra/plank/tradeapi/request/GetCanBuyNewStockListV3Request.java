package com.mistra.plank.tradeapi.request;

public class GetCanBuyNewStockListV3Request extends BaseTradeRequest {

    public GetCanBuyNewStockListV3Request(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.GetCanBuyNewStockListV3.value();
    }

    @Override
    public int responseVersion() {
        return VERSION_OBJ;
    }

}
