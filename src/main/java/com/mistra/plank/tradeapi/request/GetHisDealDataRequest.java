package com.mistra.plank.tradeapi.request;

public class GetHisDealDataRequest extends BaseTradeRequest {

    private String st;
    private String et;

    public GetHisDealDataRequest(int userId) {
        super(userId);
    }

    public String getSt() {
        return st;
    }

    public void setSt(String st) {
        this.st = st;
    }

    public String getEt() {
        return et;
    }

    public void setEt(String et) {
        this.et = et;
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.GetHisDealData.value();
    }

}
