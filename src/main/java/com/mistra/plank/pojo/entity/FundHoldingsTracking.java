package com.mistra.plank.pojo.entity;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基金季报增减数据追踪 数据来源于东财choice
 *
 * @author mistra@future.com
 * @date 2022/5/7
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "fund_holdings_tracking", autoResultMap = true)
public class FundHoldingsTracking {

    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 持有该股票的基金数
     */
    @ExcelProperty(index = 2)
    @TableField(value = "fund_count")
    private Integer fundCount;

    /**
     * 季度 202201
     */
    @TableField(value = "quarter")
    private Integer quarter;

    /**
     * 持股总量
     */
    @ExcelProperty(index = 3)
    @TableField(value = "shareholding_count")
    private Long shareholdingCount;

    /**
     * 持股变动数量
     */
    @ExcelProperty(index = 4)
    @TableField(value = "shareholding_change_count")
    private Long shareholdingChangeCount;

    /**
     * 持股总市值
     */
    @ExcelProperty(index = 7)
    @TableField(value = "total_market")
    private Long totalMarket;

    /**
     * 季度均价
     */
    @TableField(value = "average_price")
    private BigDecimal averagePrice;

    /**
     * 持股变动金额
     */
    @TableField(value = "shareholding_change_amount")
    private Double shareholdingChangeAmount;

    /**
     * 股票代码
     */
    @TableField(value = "code")
    private String code;

    /**
     * 股票名称
     */
    @ExcelProperty(index = 1)
    @TableField(value = "name")
    private String name;
}
