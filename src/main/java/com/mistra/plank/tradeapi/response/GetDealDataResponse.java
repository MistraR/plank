package com.mistra.plank.tradeapi.response;

public class GetDealDataResponse extends BaseTradeResponse {

    /**
     * 买卖类别-买
     */
    public static final String B = "B";
    /**
     * 买卖类别-卖
     */
    public static final String S = "S";

    private String Zqmc;
    /**
     * 委托编号
     */
    private String Wtbh;
    /**
     * 成交编号
     */
    private String Cjbh;
    /**
     * 成交价格
     */
    private String Cjjg;
    /**
     * 成交数量
     */
    private String Cjsl;
    /**
     * 证券代码
     */
    private String Zqdm;
    /**
     * 成交时间 HHmmss
     */
    private String Cjsj;
    /**
     * 委托数量
     */
    private String Wtsl;

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

    public String getCjbh() {
        return Cjbh;
    }

    public void setCjbh(String cjbh) {
        Cjbh = cjbh;
    }

    public String getCjjg() {
        return Cjjg;
    }

    public void setCjjg(String cjjg) {
        Cjjg = cjjg;
    }

    public String getCjsl() {
        return Cjsl;
    }

    public void setCjsl(String cjsl) {
        Cjsl = cjsl;
    }

    public String getCjsj() {
        return Cjsj;
    }

    public void setCjsj(String cjsj) {
        Cjsj = cjsj;
    }

    public String getZqdm() {
        return Zqdm;
    }

    public void setZqdm(String zqdm) {
        Zqdm = zqdm;
    }

    public String getMmlb() {
        return Mmlb;
    }

    public void setMmlb(String mmlb) {
        Mmlb = mmlb;
    }

    public String getWtsl() {
        return Wtsl;
    }

    public void setWtsl(String wtsl) {
        Wtsl = wtsl;
    }

    public String getMarket() {
        return Market;
    }

    public void setMarket(String market) {
        Market = market;
    }

    public String getFormatDealTime() {
        return getFormatDealTime(Cjsj);
    }

    public static String getFormatDealTime(String str) {
        if (str.length() == 6) {
            return new StringBuilder(str).insert(4, ':').insert(2, ':').toString();
        }
        return "00:00:00";
    }

    public static String getFormatDealDate(String str) {
        if (str.length() == 8) {
            return new StringBuilder(str).insert(6, '-').insert(4, '-').toString();
        }
        return "";
    }

}
