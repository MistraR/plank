package com.mistra.plank.model.enums;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2022/11/14 14:06
 * @ Description:
 */
public enum ClearanceReasonEnum {
    /**
     * 清仓原因
     */
    STOP_LOSE("触发止损,自动卖出"),
    RATTY_PLANK("炸板,自动卖出"),
    TAKE_PROFIT("触发止盈,自动卖出"),
    UN_PLANK("11点前还未涨停,自动卖出"),
    ROLLER_COASTER("由盈利4%到回撤到触及成本,自动卖出");

    /**
     * 描述
     */
    private final String desc;

    public String getDesc() {
        return this.desc;
    }

    ClearanceReasonEnum(String desc) {
        this.desc = desc;
    }
}
