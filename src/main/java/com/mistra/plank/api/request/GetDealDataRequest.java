package com.mistra.plank.api.request;

public class GetDealDataRequest extends BaseQueryRequest {

    public GetDealDataRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.GetDealData.value();
    }

}
