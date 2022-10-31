package com.mistra.plank.api.response;

public class CrGetHisDealDataResponse extends CrGetDealDataResponse {

    /**
     * 成交日期
     */
    private String Cjrq;

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
