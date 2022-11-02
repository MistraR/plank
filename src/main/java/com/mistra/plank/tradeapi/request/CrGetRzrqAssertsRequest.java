package com.mistra.plank.tradeapi.request;

public class CrGetRzrqAssertsRequest extends BaseTradeRequest {

    private String hblx = "RMB";

    public CrGetRzrqAssertsRequest(int userId) {
        super(userId);
    }

    public String getHblx() {
        return hblx;
    }

    public void setHblx(String hblx) {
        this.hblx = hblx;
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrGetRzrqAsserts.value();
    }

    @Override
    public int responseVersion() {
        return VERSION_DATA_OBJ;
    }

}
