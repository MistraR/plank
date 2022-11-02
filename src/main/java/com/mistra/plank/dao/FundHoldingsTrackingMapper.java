package com.mistra.plank.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.ForeignFundHoldingsTracking;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/5/7
 */
@Mapper
public interface FundHoldingsTrackingMapper extends BaseMapper<ForeignFundHoldingsTracking> {}
