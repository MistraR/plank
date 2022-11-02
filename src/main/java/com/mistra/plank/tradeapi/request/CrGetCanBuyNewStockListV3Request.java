package com.mistra.plank.tradeapi.request;

public class CrGetCanBuyNewStockListV3Request extends GetCanBuyNewStockListV3Request {

    public CrGetCanBuyNewStockListV3Request(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrGetCanBuyNewStockListV3.value();
    }

}
