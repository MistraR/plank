package com.mistra.plank.model.enums;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2022/11/14 14:06
 * @ Description:
 */
public enum AutomaticTradingEnum {
    /**
     * 自动交易策略
     */
    PLANK("打板"), AUTO_PLANK("自动打板"), SUCK("低吸"),
    BOX_BOTTOM("箱体底部买入"), BREAKTHROUGH("突破买入"),
    MANUAL("手动买入"), CANCEL("取消自动交易监控");

    /**
     * 描述
     */
    private final String desc;

    public String getDesc() {
        return this.desc;
    }

    AutomaticTradingEnum(String desc) {
        this.desc = desc;
    }
}
