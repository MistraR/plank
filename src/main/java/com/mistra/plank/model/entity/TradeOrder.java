package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mistra.plank.tradeapi.response.GetOrdersDataResponse;
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
@TableName(value = "trade_order", autoResultMap = true)
public class TradeOrder extends BaseModel {

    private static final long serialVersionUID = 1L;
    @TableField(value = "rule_id")
    private Integer ruleId;
    @TableField(value = "stock_code")
    private String stockCode;
    @TableField(value = "entrust_code")
    private String entrustCode;
    @TableField(value = "deal_code")
    private String dealCode;
    @TableField(value = "related_deal_code")
    private String relatedDealCode;
    @TableField(value = "price")
    private BigDecimal price;
    @TableField(value = "volume")
    private Integer volume;
    @TableField(value = "trade_type")
    private String tradeType;
    @TableField(value = "trade_state")
    private String tradeState;
    @TableField(value = "trade_time")
    private Date tradeTime;
    @TableField(value = "state")
    private Integer state;

    public boolean isDealed() {
        return GetOrdersDataResponse.YICHENG.equals(tradeState);
    }

    public boolean isValid() {
        return isDealed() || GetOrdersDataResponse.WEIBAO.equals(tradeState) || GetOrdersDataResponse.YIBAO.equals(tradeState);
    }

    public boolean isManual() {
        return relatedDealCode.startsWith("m");
    }

}
