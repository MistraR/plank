package com.mistra.plank.api.request;

public class CrGetHisDealDataRequest extends GetHisDealDataRequest {

    public CrGetHisDealDataRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrGetHisDealData.value();
    }

}
