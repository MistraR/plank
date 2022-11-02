package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Date;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "daily_index", autoResultMap = true)
public class DailyIndex extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "code")
    private String code;
    @TableField(value = "date")
    private Date date;
    @TableField(value = "pre_closing_price")
    private BigDecimal preClosingPrice;
    @TableField(value = "opening_price")
    private BigDecimal openingPrice;
    @TableField(value = "highest_price")
    private BigDecimal highestPrice;
    @TableField(value = "lowest_price")
    private BigDecimal lowestPrice;
    @TableField(value = "closing_price")
    private BigDecimal closingPrice;
    @TableField(value = "trading_volume")
    private Long tradingVolume;
    @TableField(value = "trading_value")
    private BigDecimal tradingValue;
    @TableField(value = "rurnover_rate")
    private BigDecimal rurnoverRate;

}
