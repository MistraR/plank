package com.mistra.plank.tradeapi.request;

public class GetAssetsRequest extends BaseTradeRequest {

    private String hblx = "RMB";

    public GetAssetsRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.GetAsserts.value();
    }

    public String getHblx() {
        return hblx;
    }

    public void setHblx(String hblx) {
        this.hblx = hblx;
    }

}
