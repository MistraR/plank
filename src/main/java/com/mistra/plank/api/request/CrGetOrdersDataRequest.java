package com.mistra.plank.api.request;

public class CrGetOrdersDataRequest extends GetOrdersDataRequest {

    public CrGetOrdersDataRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrGetOrdersData.value();
    }

}
