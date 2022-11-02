package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "execute_info", autoResultMap = true)
public class ExecuteInfo extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "task_id")
    private int taskId;
    @TableField(value = "start_time")
    private Date startTime;
    @TableField(value = "complete_time")
    private Date completeTime;
    @TableField(value = "params_str")
    private String paramsStr;
    @TableField(value = "is_manual")
    private boolean isManual;
    @TableField(value = "state")
    private Integer state;
    @TableField(value = "message")
    private String message;
}
