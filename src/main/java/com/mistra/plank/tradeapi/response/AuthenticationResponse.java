package com.mistra.plank.tradeapi.response;

public class AuthenticationResponse extends BaseTradeResponse {

    private String cookie;
    private String validateKey;

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getValidateKey() {
        return validateKey;
    }

    public void setValidateKey(String validateKey) {
        this.validateKey = validateKey;
    }

}
