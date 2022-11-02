package com.mistra.plank.tradeapi.request;

public abstract class BaseQueryRequest extends BaseTradeRequest {

    /**
     * 请求行数
     */
    private String qqhs = "60";
    private String dwc;

    protected BaseQueryRequest(int userId) {
        super(userId);
    }

    public String getQqhs() {
        return qqhs;
    }

    public void setQqhs(String qqhs) {
        this.qqhs = qqhs;
    }

    public String getDwc() {
        return dwc;
    }

    public void setDwc(String dwc) {
        this.dwc = dwc;
    }

}
