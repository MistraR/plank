package com.mistra.plank.tradeapi.response;

public class CrGetDealDataResponse extends GetDealDataResponse {

    private String Xyjylbbz;
    private String Xyjylx;
    private String Mmsm;
    private String Wtxh;

    public String getXyjylbbz() {
        return Xyjylbbz;
    }

    public void setXyjylbbz(String xyjylbbz) {
        Xyjylbbz = xyjylbbz;
    }

    public String getXyjylx() {
        return Xyjylx;
    }

    public void setXyjylx(String xyjylx) {
        Xyjylx = xyjylx;
    }

    public String getMmsm() {
        return Mmsm;
    }

    public void setMmsm(String mmsm) {
        Mmsm = mmsm;
    }

    public String getWtxh() {
        return Wtxh;
    }

    public void setWtxh(String wtxh) {
        Wtxh = wtxh;
    }

    @Override
    public String getWtbh() {
        return Wtxh;
    }

    @Override
    public String getMmlb() {
        if (Mmsm.contains("买")) {
            return GetDealDataResponse.B;
        }

        if (Mmsm.contains("卖")) {
            return GetDealDataResponse.S;
        }
        if (Mmsm.contains("担保品划入")) {
            return GetDealDataResponse.B;
        }

        if (Mmsm.contains("担保品划出")) {
            return GetDealDataResponse.S;
        }

        return super.getMmlb();
    }

}
