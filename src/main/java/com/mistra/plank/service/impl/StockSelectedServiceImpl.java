package com.mistra.plank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mistra.plank.dao.StockSelectedDao;
import com.mistra.plank.model.entity.StockSelected;
import com.mistra.plank.service.StockSelectedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockSelectedServiceImpl implements StockSelectedService {

    @Autowired
    private StockSelectedDao stockSelectedDao;

    @Override
    public List<StockSelected> getList() {
        return stockSelectedDao.selectList(new LambdaQueryWrapper<>());
    }

}
