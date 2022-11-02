package com.mistra.plank.tradeapi.request;

public class CrSubmitRequest extends SubmitRequest {

    public static final String xyjylx_db_b = "6";
    public static final String xyjylx_rz_b = "a";
    public static final String xyjylx_hq_b = "B";
    public static final String xyjylx_db_s = "7";
    public static final String xyjylx_rq_s = "A";
    public static final String xyjylx_hk_s = "b";

    /**
     * 信用交易类型
     */
    private String xyjylx;

    private String stockName = "unknow";

    public CrSubmitRequest(int userId) {
        super(userId);
    }

    public String getStockName() {
        return stockName;
    }

    public String getXyjylx() {
        return xyjylx;
    }

    public void setXyjylx(String xyjylx) {
        this.xyjylx = xyjylx;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public void setTradeInfo(String jylx) {
        switch (jylx) {
        case xyjylx_db_b:
        case xyjylx_rz_b:
        case xyjylx_hq_b:
            setTradeType(B);
            break;
        case xyjylx_db_s:
        case xyjylx_rq_s:
        case xyjylx_hk_s:
            setTradeType(S);
            break;
        default:
            break;
        }
        setXyjylx(jylx);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrSubmit.value();
    }

}
