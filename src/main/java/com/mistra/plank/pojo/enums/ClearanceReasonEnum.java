package com.mistra.plank.pojo.enums;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/19
 */
public enum ClearanceReasonEnum {

    /**
     * 清仓原因
     */
    BREAK_POSITION("跌破均线清仓"),
    BREAK_LOSS_LINE("跌破止损线清仓"),
    TAKE_PROFIT("收益回撤到10个点止盈清仓"),
    PROFIT_HALF("收益15% 减半仓"),
    PROFIT_UPPER("收益到顶清仓");

    private String desc;

    public String getDesc() {
        return this.desc;
    }

    private ClearanceReasonEnum(String desc) {
        this.desc = desc;
    }
}
