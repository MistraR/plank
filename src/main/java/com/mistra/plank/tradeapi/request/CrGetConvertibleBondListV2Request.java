package com.mistra.plank.tradeapi.request;

public class CrGetConvertibleBondListV2Request extends GetConvertibleBondListV2Request {

    public CrGetConvertibleBondListV2Request(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrGetConvertibleBondListV2.value();
    }

}
