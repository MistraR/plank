package com.mistra.plank.api.request;

public class CrGetDealDataRequest extends GetDealDataRequest {

    public CrGetDealDataRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrGetDealData.value();
    }

}
