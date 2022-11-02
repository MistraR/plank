package com.mistra.plank.model.enums;

/**
 * 选股策略
 *
 * @author mistra@future.com
 * @date 2021/11/19
 */
public enum BuyStrategyEnum {

    /**
     * 选股策略
     */
    DRAGON_LIST("龙虎榜净买入选股"), RED_THREE_SOLDIERS("红三兵选股"), EXPLOSIVE_VOLUME_BACK("爆量回踩");

    /**
     * 描述
     */
    private final String desc;

    public String getDesc() {
        return this.desc;
    }

    BuyStrategyEnum(String desc) {
        this.desc = desc;
    }
}
