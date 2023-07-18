package com.mistra.plank.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mistra.plank.dao.StockInfoDao;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.dao.StockSelectedDao;
import com.mistra.plank.job.AutomaticPlankTrading;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.entity.StockInfo;
import com.mistra.plank.model.entity.StockSelected;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
import com.mistra.plank.model.param.AutoTradeParam;
import com.mistra.plank.model.param.SelfSelectParam;
import com.mistra.plank.service.StockSelectedService;

@Service
public class StockSelectedServiceImpl implements StockSelectedService {

    @Autowired
    private StockSelectedDao stockSelectedDao;

    @Autowired
    private StockInfoDao stockInfoDao;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AutomaticPlankTrading automaticPlankTrading;

    @Override
    public List<StockSelected> getList() {
        return stockSelectedDao.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public void addSelfSelect(SelfSelectParam selfSelectParam) {
        List<StockInfo> stockInfos = stockInfoDao.selectList(new LambdaQueryWrapper<StockInfo>().in(StockInfo::getName, selfSelectParam.getNames()));
        stockSelectedDao.delete(new LambdaQueryWrapper<>());
        for (StockInfo stockInfo : stockInfos) {
            stockSelectedDao.insert(StockSelected.builder().code(stockInfo.getCode()).rate(new BigDecimal("0.2")).description("").build());
        }
    }

    @Override
    public void tomorrowAutoTradePool(List<AutoTradeParam> autoTradeParams) {
        LambdaUpdateWrapper<Stock> wrapper = new LambdaUpdateWrapper<Stock>()
                .in(Stock::getAutomaticTradingType, AutomaticTradingEnum.PLANK.name(), AutomaticTradingEnum.SUCK.name());
        stockMapper.update(Stock.builder().automaticTradingType(AutomaticTradingEnum.CANCEL.name()).build(), wrapper);
        for (AutoTradeParam autoTradeParam : autoTradeParams) {
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getName, autoTradeParam.getName()));
            if (Objects.nonNull(stock)) {
                stock.setAutomaticTradingType(autoTradeParam.getAutomaticTradingType());
                stock.setBuyAmount(autoTradeParam.getBuyAmount());
                stock.setSuckTriggerPrice(autoTradeParam.getSuckTriggerPrice());
                stockMapper.updateById(stock);
                StockInfo stockInfo = stockInfoDao.selectOne(new LambdaQueryWrapper<StockInfo>().eq(StockInfo::getName, stock.getName()));
                if (Objects.nonNull(stockInfo)) {
                    stockSelectedDao.insert(StockSelected.builder().code(stock.getCode().substring(2, 8)).rate(new BigDecimal("0.2"))
                            .description(autoTradeParam.getAutomaticTradingType()).build());
                }
            }
        }
    }

    @Override
    public void addAutoPlank(SelfSelectParam selfSelectParam) {
        stockMapper.update(Stock.builder().autoPlank(false).build(), new LambdaUpdateWrapper<>());
        List<Stock> stockInfos = stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getName, selfSelectParam.getNames()));
        for (Stock stock : stockInfos) {
            stock.setAutoPlank(true);
            stockMapper.updateById(stock);
        }
        automaticPlankTrading.selectAutoPlankStock();
    }
}
