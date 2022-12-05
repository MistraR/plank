package com.mistra.plank.tradeapi.request;

public class AuthenticationRequest extends BaseTradeRequest {

    private String password;
    private String randNumber;
    private String identifyCode;
    private String duration = "1800";
    private String authCode;
    private String secInfo;
    private String type = "Z";

    public AuthenticationRequest(int userId) {
        super(userId);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRandNumber() {
        return randNumber;
    }

    public void setRandNumber(String randNumber) {
        this.randNumber = randNumber;
    }

    public String getIdentifyCode() {
        return identifyCode;
    }

    public void setIdentifyCode(String identifyCode) {
        this.identifyCode = identifyCode;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getSecInfo() {
        return secInfo;
    }

    public void setSecInfo(String secInfo) {
        this.secInfo = secInfo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getMethod() {
        return BaseTradeRequest.TradeRequestMethod.Authentication.value();
    }


}
