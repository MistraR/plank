package com.mistra.plank.tradeapi.request;

public class CrRevokeRequest extends RevokeRequest {

    public CrRevokeRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrRevoke.value();
    }

}
