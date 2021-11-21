package com.mistra.plank.pojo.enums;

/**
 * 减仓，清仓操作原因
 *
 * @author mistra@future.com
 * @date 2021/11/19
 */
public enum ClearanceReasonEnum {

    /**
     * 操作原因
     */
    BREAK_POSITION("跌破均线清仓"),
    BREAK_LOSS_LINE("跌破止损线清仓"),
    TAKE_PROFIT("收益回撤到10个点止盈清仓"),
    PROFIT_UPPER("收益达到止盈条件清仓"),
    TEN_DAY("持股超过8天 清仓"),
    POSITION_HALF("收益15% 减半仓"),
    POSITION_QUARTER("收益20% 减至1/4仓");

    /**
     * 原因
     */
    private final String desc;

    public String getDesc() {
        return this.desc;
    }

    ClearanceReasonEnum(String desc) {
        this.desc = desc;
    }
}
