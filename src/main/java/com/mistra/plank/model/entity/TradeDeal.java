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
@TableName(value = "trade_deal", autoResultMap = true)
public class TradeDeal extends BaseModel {

    private static final long serialVersionUID = 1L;
    @TableField(value = "stock_code")
    private String stockCode;
    @TableField(value = "deal_code")
    private String dealCode;
    @TableField(value = "price")
    private BigDecimal price;
    @TableField(value = "volume")
    private Integer volume;
    @TableField(value = "trade_type")
    private String tradeType;
    @TableField(value = "cr_trade_type")
    private String crTradeType;
    @TableField(value = "trade_dime")
    private Date tradeTime;
}
