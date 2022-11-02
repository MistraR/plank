package com.mistra.plank.tradeapi.request;

import java.util.List;

public abstract class BaseTradeListRequest extends BaseTradeRequest {

    protected BaseTradeListRequest(int userId) {
        super(userId);
    }

    public abstract List<?> getList();

}
