package com.mistra.plank.tradeapi.response;

public class GetOrdersDataResponse extends BaseTradeResponse {

    public static final String WEIBAO = "未报";
    public static final String YIBAO = "已报";
    public static final String YICHENG = "已成";
    public static final String YICHE = "已撤";

    /**
     * 买卖类别-买
     */
    public static final String B = "B";
    /**
     * 买卖类别-卖
     */
    public static final String S = "S";

    /**
     * 证券名称
     */
    private String Zqmc;
    /**
     * 委托编号
     */
    private String Wtbh;
    /**
     * 委托时间
     * HHmmss
     */
    private String Wtsj;
    /**
     * 证券代码
     */
    private String Zqdm;
    /**
     * 委托数量
     */
    private String Wtsl;
    /**
     * 委托价格
     */
    private String Wtjg;
    /**
     * 委托状态
     *
     * @see #YIBAO
     * @see #YICHENG
     * @see #YICHE
     */
    private String Wtzt;
    /**
     * 买卖类别
     *
     * @see #B
     * @see #S
     */
    private String Mmlb;

    private String Market;

    public String getZqmc() {
        return Zqmc;
    }

    public void setZqmc(String zqmc) {
        Zqmc = zqmc;
    }

    public String getWtbh() {
        return Wtbh;
    }

    public void setWtbh(String wtbh) {
        Wtbh = wtbh;
    }

    public String getWtsj() {
        return Wtsj;
    }

    public void setWtsj(String wtsj) {
        Wtsj = wtsj;
    }

    public String getZqdm() {
        return Zqdm;
    }

    public void setZqdm(String zqdm) {
        Zqdm = zqdm;
    }

    public String getWtsl() {
        return Wtsl;
    }

    public void setWtsl(String wtsl) {
        Wtsl = wtsl;
    }

    public String getWtjg() {
        return Wtjg;
    }

    public void setWtjg(String wtjg) {
        Wtjg = wtjg;
    }

    public String getWtzt() {
        return Wtzt;
    }

    public void setWtzt(String wtzt) {
        Wtzt = wtzt;
    }

    public String getMmlb() {
        return Mmlb;
    }

    public void setMmlb(String mmlb) {
        Mmlb = mmlb;
    }

    public String getMarket() {
        return Market;
    }

    public void setMarket(String market) {
        Market = market;
    }

}
