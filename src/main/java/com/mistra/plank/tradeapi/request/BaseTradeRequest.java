package com.mistra.plank.tradeapi.request;

public abstract class BaseTradeRequest {

    // 返回格式类型
    // { Data: [] }
    public static final int VERSION_DATA_LIST = 0;
    // { Data: {} }
    public static final int VERSION_DATA_OBJ = 1;
    // msg...
    public static final int VERSION_MSG = 2;
    // { }
    public static final int VERSION_OBJ = 3;

    private int userId;

    protected BaseTradeRequest(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public abstract String getMethod();

    public enum TradeRequestMethod {
        GetAsserts("get_asserts"), Submit("submit"), Revoke("revoke"), GetStockList("get_stock_list"),
        GetOrdersData("get_orders_data"), GetDealData("get_deal_data"), Authentication("authentication"),
        AuthenticationCheck("authentication_check"), GetHisDealData("get_his_deal_data"),
        GetHisOrdersData("get_his_orders_data"),
        GetCanBuyNewStockListV3("get_can_buy_new_stock_list_v3"),
        GetConvertibleBondListV2("get_convertible_bond_list_v2"),
        SubmitBatTradeV2("submit_bat_trade_v2"),
        YZM("yzm"),
        CrGetRzrqAsserts("cr_get_rzrq_asserts"),
        CrQueryCollateral("cr_query_collateral"),
        CrSubmit("cr_submit"),
        CrGetOrdersData("cr_get_orders_data"),
        CrGetDealData("cr_get_deal_data"),
        CrGetHisDealData("cr_get_his_deal_data"),
        CrGetHisOrdersData("cr_get_his_orders_data"),
        CrRevoke("cr_revoke"),
        CrGetCanBuyNewStockListV3("cr_get_can_buy_new_stock_list_v3"),
        CrGetConvertibleBondListV2("cr_get_convertible_bond_list_v2"),
        CrSubmitBatTradeV2("cr_submit_bat_trade_v2"),
        ;
        private String value;

        TradeRequestMethod(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

    }

    public int responseVersion() {
        return VERSION_DATA_LIST;
    }

}
