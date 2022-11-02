package com.mistra.plank.tradeapi.request;

public class SubmitRequest extends BaseTradeRequest {

    /**
     * 买卖类别-买
     */
    public static final String B = "B";
    /**
     * 买卖类别-卖
     */
    public static final String S = "S";

    private String stockCode;
    private double price;
    private int amount;
    private String zqmc = "unknow";
    private String market;

    /**
     * 买卖类别
     *
     * @see #B
     * @see #S
     */
    private String tradeType;

    public SubmitRequest(int userId) {
        super(userId);
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getZqmc() {
        return zqmc;
    }

    public void setZqmc(String zqmc) {
        this.zqmc = zqmc;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.Submit.value();
    }

}
