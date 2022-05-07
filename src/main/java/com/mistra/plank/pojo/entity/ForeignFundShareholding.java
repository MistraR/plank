package com.mistra.plank.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 外资(截止上个交易日)+基金(最新季报)持股市值
 *
 * @author mistra@future.com
 * @date 2022/5/7
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "foreign_fund_shareholding", autoResultMap = true)
public class ForeignFundShareholding {

    @TableId(value = "id")
    private String id;

    /**
     * 股票代码
     */
    @TableField(value = "code")
    private String code;

    /**
     * 股票名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 外资持股数量
     */
    @TableField(value = "foreign_shareholding_count")
    private Long foreignShareholdingCount;

    /**
     * 外资+最新基金季报持股总金额
     */
    @TableField(value = "total")
    private Long total;

    /**
     * 最近更新日期
     */
    @TableField(value = "modify_time")
    private Date modifyTime;
}
