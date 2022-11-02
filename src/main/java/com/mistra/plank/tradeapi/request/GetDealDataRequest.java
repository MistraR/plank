package com.mistra.plank.tradeapi.request;

public class GetDealDataRequest extends BaseQueryRequest {

    public GetDealDataRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.GetDealData.value();
    }

}
