package com.mistra.plank.dao;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.DailyIndex;

/**
 * @author rui.wang@yamu.com
 * @description:
 * @date 2023/7/22
 */
@Mapper
public interface DailyIndexMapper extends BaseMapper<DailyIndex> {
}
