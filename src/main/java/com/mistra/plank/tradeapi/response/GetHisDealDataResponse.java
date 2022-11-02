package com.mistra.plank.tradeapi.response;

public class GetHisDealDataResponse extends GetDealDataResponse {

    /**
     * 成交序号
     */
    private String Cjxh;
    /**
     * 成交日期
     */
    private String Cjrq;

    public String getCjxh() {
        return Cjxh;
    }

    public void setCjxh(String cjxh) {
        Cjxh = cjxh;
    }

    public String getCjrq() {
        return Cjrq;
    }

    public void setCjrq(String cjrq) {
        Cjrq = cjrq;
    }
    
    public String getFormatDealDate() {
        return getFormatDealDate(Cjrq);
    }
    
}
