package com.mistra.plank.tradeapi.request;

public class CrGetDealDataRequest extends GetDealDataRequest {

    public CrGetDealDataRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrGetDealData.value();
    }

}
