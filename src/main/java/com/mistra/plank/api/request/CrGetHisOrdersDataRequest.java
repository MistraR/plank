package com.mistra.plank.api.request;

public class CrGetHisOrdersDataRequest extends GetHisOrdersDataRequest {

    public CrGetHisOrdersDataRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrGetHisOrdersData.value();
    }

}
