package com.mistra.plank.tradeapi.response;

public class GetAssetsResponse extends BaseTradeResponse {

    /**
     * 总资产
     */
    private String Zzc;
    /**
     * 可用资金
     */
    private String Kyzj;
    /**
     * 可取资金
     */
    private String Kqzj;
    /**
     * 冻结资金
     */
    private String Djzj;

    public String getZzc() {
        return Zzc;
    }

    public void setZzc(String zzc) {
        Zzc = zzc;
    }

    public String getKyzj() {
        return Kyzj;
    }

    public void setKyzj(String kyzj) {
        Kyzj = kyzj;
    }

    public String getKqzj() {
        return Kqzj;
    }

    public void setKqzj(String kqzj) {
        Kqzj = kqzj;
    }

    public String getDjzj() {
        return Djzj;
    }

    public void setDjzj(String djzj) {
        Djzj = djzj;
    }

}
