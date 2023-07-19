package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2023/2/20 11:34
 * @ Description:
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "bk", autoResultMap = true)
public class Bk implements Comparable<Bk> {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "name")
    private String name;

    /**
     * 版块代码
     */
    @TableField(value = "bk")
    private String bk;

    /**
     * 当日涨幅
     */
    @TableField(value = "increase_rate")
    private BigDecimal increaseRate;

    /**
     * 版块分类 行业版块|概念版块
     */
    @TableField(value = "classification")
    private String classification;

    /**
     * 是否忽略该板块
     */
    @TableField(value = "ignore_update")
    private Boolean ignoreUpdate;

    /**
     * 是否参与自动打板
     */
    @TableField(value = "auto_plank")
    private Boolean autoPlank;

    @Override
    public int compareTo(Bk o) {
        return (int) ((o.increaseRate.doubleValue() * 100) - (this.increaseRate.doubleValue() * 100));
    }
}
