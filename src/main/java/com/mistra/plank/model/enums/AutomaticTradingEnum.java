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
    AUTO_PLANK("自动打板"),
    SUCK("低吸"),
    MANUAL("手动买入"),
    CANCEL("取消自动交易监控"),
    /**
     * 有些票不想打板,比如一字连板起来的,这种一般不参与,就在数据库手动排除掉
     */
    CANCEL_AUTO_PLANK("不参与自动打板");

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
