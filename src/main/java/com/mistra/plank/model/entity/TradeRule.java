package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mistra.plank.common.util.StockConsts;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "trade_rule", autoResultMap = true)
public class TradeRule extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "stock_code")
    private String stockCode;
    @TableField(value = "strategy_id")
    private Integer strategyId;
    @TableField(value = "user_id")
    private Integer userId;
    @TableField(value = "type")
    private Integer type;
    @TableField(value = "value")
    private BigDecimal value;
    @TableField(value = "volume")
    private Integer volume;
    @TableField(value = "open_price")
    private BigDecimal openPrice;
    @TableField(value = "highest_price")
    private BigDecimal highestPrice;
    @TableField(value = "lowest_price")
    private BigDecimal lowestPrice;
    @TableField(value = "highest_volume")
    private BigDecimal highestVolume;
    @TableField(value = "lowest_volume")
    private BigDecimal lowestVolume;
    @TableField(value = "state")
    private Integer state;
    @TableField(value = "description")
    private String description;

    public boolean isValid() {
        return state == StockConsts.TradeState.Valid.value();
    }

    public boolean isProportion() {
        return CalcType.PROPORTION.value == type;
    }

    public enum CalcType {
        PROPORTION(0), DIFFERENCE(1);
        private int value;

        private CalcType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

}
