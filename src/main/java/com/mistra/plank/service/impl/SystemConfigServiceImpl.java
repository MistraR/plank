package com.mistra.plank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mistra.plank.dao.SystemConfigDao;
import com.mistra.plank.model.entity.SystemConfig;
import com.mistra.plank.service.SystemConfigService;
import com.mistra.plank.common.util.StockConsts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigDao systemConfigDao;

    @Override
    public boolean isMock() {
        List<SystemConfig> list = systemConfigDao.selectList(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getName, "trade_mock").eq(SystemConfig::getState, StockConsts.TradeState.Valid.value()));
        return !list.isEmpty() && list.get(0).getValue1().equals("1");
    }

    @Override
    public List<SystemConfig> getAll() {
        return systemConfigDao.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public boolean isCr() {
        List<SystemConfig> list = systemConfigDao.selectList(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getState, StockConsts.TradeState.Valid.value())
                .eq(SystemConfig::getName, "trade_cr"));
        return !list.isEmpty() && list.get(0).getValue1().equals("1");
    }

    @Override
    public boolean isApplyNewConvertibleBond() {
        List<SystemConfig> list = systemConfigDao.selectList(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getState, StockConsts.TradeState.Valid.value())
                .eq(SystemConfig::getName, "apply_new_convertible_bond"));
        return !list.isEmpty() && list.get(0).getValue1().equals("1");
    }


}
