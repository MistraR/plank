package com.mistra.plank.tradeapi.request;

public class CrGetOrdersDataRequest extends GetOrdersDataRequest {

    public CrGetOrdersDataRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrGetOrdersData.value();
    }

}
