package com.mistra.plank.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.DailyRecord;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Mapper
public interface DailyRecordMapper extends BaseMapper<DailyRecord> {}
