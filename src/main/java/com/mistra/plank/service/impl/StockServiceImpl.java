package com.mistra.plank.service.impl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.common.util.StockConsts;
import com.mistra.plank.dao.StockInfoDao;
import com.mistra.plank.dao.StockLogDao;
import com.mistra.plank.dao.impl.DailyIndexDao;
import com.mistra.plank.model.entity.DailyIndex;
import com.mistra.plank.model.entity.StockInfo;
import com.mistra.plank.model.entity.StockLog;
import com.mistra.plank.model.vo.DailyIndexVo;
import com.mistra.plank.model.vo.PageParam;
import com.mistra.plank.model.vo.PageVo;
import com.mistra.plank.service.DailyIndexParser;
import com.mistra.plank.service.StockCrawlerService;
import com.mistra.plank.service.StockInfoService;

@Service
public class StockServiceImpl implements StockInfoService {

    private final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);

    private static final String LIST_MESSAGE = "'list' must not be null";

    @Autowired
    private StockInfoDao stockInfoDao;

    @Autowired
    private StockLogDao stockLogDao;

    @Autowired
    private DailyIndexDao dailyIndexDao;

    @Autowired
    private StockCrawlerService stockCrawlerService;

    @Autowired
    private DailyIndexParser dailyIndexParser;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public List<StockInfo> getAll() {
        PageParam pageParam = new PageParam();
        pageParam.setStart(0);
        pageParam.setLength(Integer.MAX_VALUE);
        return stockInfoDao.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public List<StockInfo> getAllListed() {
        return getAll().stream().filter(stockInfo ->
                stockInfo.isValid() && stockInfo.isA()
        ).collect(Collectors.toList());
    }

    @Override
    public void addStockLog(List<StockLog> list) {
        Assert.notNull(list, StockServiceImpl.LIST_MESSAGE);
        if (!list.isEmpty()) {
            for (StockLog stockLog : list) {
                stockLogDao.insert(stockLog);
            }
        }
    }

    @CacheEvict(value = StockConsts.CACHE_KEY_DATA_STOCK, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(List<StockInfo> needAddedList, List<StockInfo> needUpdatedList, List<StockLog> stockLogList) {
        if (needAddedList != null) {
            add(needAddedList);
        }
        if (needUpdatedList != null) {
            update(needUpdatedList);
        }
        if (stockLogList != null) {
            addStockLog(stockLogList);
        }
        if (needAddedList != null && !needAddedList.isEmpty()) {
            List<String> newCodeList = needAddedList.stream().map(StockInfo::getCode)
                    .collect(Collectors.toList());
            stockLogDao.setStockIdByCodeType(newCodeList, StockConsts.StockLogType.New.value());
        }
    }

    private void add(List<StockInfo> list) {
        Assert.notNull(list, StockServiceImpl.LIST_MESSAGE);
        if (!list.isEmpty()) {
            stockInfoDao.add(list);
        }
    }

    private void update(List<StockInfo> list) {
        Assert.notNull(list, StockServiceImpl.LIST_MESSAGE);
        if (!list.isEmpty()) {
            stockInfoDao.update(list);
        }
    }

    @Override
    public void saveDailyIndexToFile(String rootPath) {
        List<StockInfo> list = getAll().stream().filter(StockInfo::isA).collect(Collectors.toList());

        File root = new File(rootPath);

        list.forEach(stockInfo -> {
            logger.info("start save {}: {}", stockInfo.getName(), stockInfo.getCode());
            try {
                File file = new File(root, stockInfo.getExchange() + "/" + stockInfo.getCode() + ".txt");
                if (file.length() < 5 * 1024) {
                    String content = stockCrawlerService.getHistoryDailyIndexsString(stockInfo.getCode());
                    try (FileWriter out = new FileWriter(file)) {
                        FileCopyUtils.copy(content, out);
                    }
                }
            } catch (Exception e) {
                logger.error("get daily index error {} {}", stockInfo.getName(), stockInfo.getCode(), e);
            }
        });
    }

    @Override
    public void saveDailyIndexFromFile(String rootPath) {
        List<StockInfo> list = getAll().stream().filter(StockInfo::isA).collect(Collectors.toList());

        File root = new File(rootPath);

        CountDownLatch countDownLatch = new CountDownLatch(list.size());

        list.forEach(stockInfo -> threadPoolTaskExecutor.execute(() -> {
            try {
                handleStockDaily(root, stockInfo);
            } finally {
                countDownLatch.countDown();
            }
        }));

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("countDownLatch await interrupt", e);
        }
        logger.info("sub task is not completed");
    }

    private void handleStockDaily(File root, StockInfo stockInfo) {
        logger.info("start save {}: {}", stockInfo.getName(), stockInfo.getCode());
        try {
            File file = new File(root, stockInfo.getExchange() + "/" + stockInfo.getCode() + ".txt");
            try (FileReader in = new FileReader(file)) {
                String content = FileCopyUtils.copyToString(in);
                List<DailyIndex> dailyIndexList = dailyIndexParser.parseHistoryDailyIndexList(content);
                dailyIndexList.forEach(dailyIndex -> dailyIndex.setCode(stockInfo.getFullCode()));
                dailyIndexDao.save(dailyIndexList);
            }
        } catch (Exception e) {
            logger.error("save daily index error {}", stockInfo.getFullCode(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveDailyIndex(List<DailyIndex> list) {
        dailyIndexDao.save(list);
    }

    @Override
    public PageVo<StockInfo> getStockList(PageParam pageParam) {
        Page<StockInfo> page = stockInfoDao.selectPage(new Page<>(pageParam.getStart() + 1, pageParam.getLength()), new LambdaQueryWrapper<>());
        return new PageVo<>(page.getRecords(), (int) page.getTotal());
    }

    @Cacheable(value = StockConsts.CACHE_KEY_DATA_STOCK, key = "#code")
    @Override
    public StockInfo getStockByFullCode(String code) {
        StockInfo stockInfo =
                stockInfoDao.selectOne(new LambdaQueryWrapper<StockInfo>().eq(StockInfo::getCode, code.substring(2, 8)).eq(StockInfo::getExchange,
                        code.substring(0, 2)));
        if (stockInfo == null) {
            stockInfo = stockInfoDao.selectOne(new LambdaQueryWrapper<StockInfo>().eq(StockInfo::getCode, code));
        }
        if (stockInfo == null) {
            stockInfo = new StockInfo();
            stockInfo.setAbbreviation("wlrzq");
            stockInfo.setCode(code);
            stockInfo.setName("未录入证券");
            stockInfo.setExchange(StockConsts.Exchange.SH.getName());
            stockInfo.setState(StockConsts.StockState.Terminated.value());
            stockInfo.setType(StockConsts.StockType.A.value());
        }
        return stockInfo;
    }

    @Override
    public PageVo<DailyIndexVo> getDailyIndexList(PageParam pageParam) {
        pageParam.getCondition().put("date", DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
        return dailyIndexDao.getDailyIndexList(pageParam);
    }

    @Override
    public List<DailyIndex> getDailyIndexListByDate(Date date) {
        return dailyIndexDao.getDailyIndexListByDate(date);
    }

    @Override
    public void fixDailyIndex(int date, List<String> code) {
        List<StockInfo> list = getAll().stream().filter(StockInfo::isA).collect(Collectors.toList());
        if (code != null && !code.isEmpty()) {
            list = getAll().stream().filter(v -> code.contains(v.getCode())).collect(Collectors.toList());
        }
        list.forEach(stockInfo -> {
            logger.info("start fixDailyIndex {}: {}", stockInfo.getName(), stockInfo.getCode());
            try {
                int year = date / 100;
                int season = (date % 100 - 1) / 3 + 1;
                String content = stockCrawlerService.getHistoryDailyIndexsStringFrom163(stockInfo.getCode(), year, season);
                List<DailyIndex> dailyIndexList = dailyIndexParser.parse163HistoryDailyIndexList(content);
                dailyIndexList.forEach(d -> d.setCode(stockInfo.getFullCode()));
                dailyIndexDao.save(dailyIndexList);
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                logger.error("fixDailyIndex error {} {}", stockInfo.getName(), stockInfo.getCode(), e);
            }
        });
    }

}
