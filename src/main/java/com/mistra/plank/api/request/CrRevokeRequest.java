package com.mistra.plank.api.request;

public class CrRevokeRequest extends RevokeRequest {

    public CrRevokeRequest(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrRevoke.value();
    }

}
