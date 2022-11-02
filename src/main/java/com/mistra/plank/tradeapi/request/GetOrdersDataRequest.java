package com.mistra.plank.tradeapi.request;

public class GetOrdersDataRequest extends BaseQueryRequest {

    public GetOrdersDataRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.GetOrdersData.value();
    }

}
