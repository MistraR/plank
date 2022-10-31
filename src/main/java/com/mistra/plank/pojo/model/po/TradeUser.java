package com.mistra.plank.pojo.model.po;

public class TradeUser extends BaseModel {

    private static final long serialVersionUID = 1L;

    private String accountId;
    private String password;
    private String validateKey;
    private String cookie;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getValidateKey() {
        return validateKey;
    }

    public void setValidateKey(String validateKey) {
        this.validateKey = validateKey;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    @Override
    public String toString() {
        return "TradeUser [accountId=" + accountId + ", password=" + password + ", validateKey="
                + validateKey + ", cookie=" + cookie + ", toString()=" + super.toString() + "]";
    }

}
