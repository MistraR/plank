package com.mistra.plank.service.impl;

import com.mistra.plank.dao.RobotDao;
import com.mistra.plank.model.entity.Robot;
import com.mistra.plank.service.RobotService;
import com.mistra.plank.common.util.StockConsts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
@Service
public class RobotServiceImpl implements RobotService {

    private static final String ID_SYSTEM = "1";

    @Autowired
    private RobotDao robotDao;

    @Cacheable(value = StockConsts.CACHE_KEY_CONFIG_ROBOT, key = "'" + RobotServiceImpl.ID_SYSTEM + "'")
    @Override
    public Robot getSystem() {
        return getById(Integer.parseInt(RobotServiceImpl.ID_SYSTEM));
    }

    @Cacheable(value = StockConsts.CACHE_KEY_CONFIG_ROBOT, key = "#id.toString()")
    @Override
    public Robot getById(int id) {
        return robotDao.selectById(id);
    }

}
