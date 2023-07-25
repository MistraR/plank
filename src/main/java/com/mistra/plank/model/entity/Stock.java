package com.mistra.plank.model.entity;

import java.math.BigDecimal;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "stock", autoResultMap = true)
public class Stock implements Comparable<Stock> {

    @TableId(value = "code")
    private String code;

    @TableField
    private String name;

    /**
     * 市值 流通值
     */
    @TableField(value = "market_value")
    private Long marketValue;

    /**
     * 当前价格
     */
    @TableField(value = "current_price")
    private BigDecimal currentPrice;

    /**
     * 预计建仓价格
     */
    @TableField(value = "purchase_price")
    private BigDecimal purchasePrice;

    /**
     * 当日成交额
     */
    @TableField(value = "transaction_amount")
    private BigDecimal transactionAmount;

    /**
     * 买点类型 5-MA5 10-MA10 20-MA20
     */
    @TableField(value = "purchase_type")
    private Integer purchaseType;

    /**
     * 5日均线
     */
    @TableField(value = "ma5")
    private BigDecimal ma5;

    /**
     * 10日均线
     */
    @TableField(value = "ma10")
    private BigDecimal ma10;

    /**
     * 20日均线
     */
    @TableField(value = "ma20")
    private BigDecimal ma20;

    /**
     * 名称缩写
     */
    @TableField(value = "abbreviation")
    private String abbreviation;

    /**
     * 是否关注
     */
    @TableField(value = "track")
    private Boolean track;

    /**
     * 是否持仓
     */
    @TableField(value = "shareholding")
    private Boolean shareholding;

    /**
     * 所属板块
     */
    @TableField(value = "classification")
    private String classification;

    /**
     * 自动交易类型
     */
    @TableField(value = "automatic_trading_type")
    private String automaticTradingType;

    /**
     * 买入数量
     */
    @TableField(value = "buy_amount")
    private Integer buyAmount;

    /**
     * 触发自动低吸买入的价格
     */
    @TableField(value = "suck_trigger_price")
    private BigDecimal suckTriggerPrice;

    /**
     * 最近一次自动下单买入时间
     */
    @TableField(value = "buy_time")
    private Date buyTime;

    /**
     * 当前连板数
     */
    @TableField(value = "plank_num")
    private Integer plankNumber;

    @Override
    public int compareTo(Stock o) {
        return (int) (this.transactionAmount.doubleValue()) - (int) (o.transactionAmount.doubleValue());
    }
}
