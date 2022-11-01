package com.mistra.plank.job;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mistra.plank.api.TradeResultVo;
import com.mistra.plank.api.request.SubmitRequest;
import com.mistra.plank.api.response.SubmitResponse;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.pojo.entity.Stock;
import com.mistra.plank.service.TradeApiService;
import com.mistra.plank.util.HttpUtil;
import com.mistra.plank.util.StockUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2022/11/1 14:18
 * @ Description: 查询需要监控的连板票，发现上板则立即挂买单排板
 * 需要自己在数据库 stock表 手动编辑自己想要监控打板的股票，买入数量，价格
 */
@Slf4j
@Component
public class AutomaticTrading implements CommandLineRunner {

    private final StockMapper stockMapper;
    private final TradeApiService tradeApiService;

    private final PlankConfig plankConfig;

    /**
     * 需要监控的股票
     */
    private static final ConcurrentHashMap<String, Stock> map = new ConcurrentHashMap<>();

    /**
     * 监控中的股票
     */
    private static final HashSet<String> runningSet = new HashSet<>();

    /**
     * 已经成功挂单的股票
     */
    private static final HashSet<String> pendingOrderSet = new HashSet<>();

    private final ReentrantLock lock = new ReentrantLock();

    public AutomaticTrading(StockMapper stockMapper, TradeApiService tradeApiService, PlankConfig plankConfig) {
        this.stockMapper = stockMapper;
        this.tradeApiService = tradeApiService;
        this.plankConfig = plankConfig;
    }

    /**
     * 每3分钟更新一次需要打板的股票
     */
    @Scheduled(cron = "0 */3 * * * ?")
    private void updatePlankMap() {
        if (plankConfig.getAutomaticTrading() && isTradeTime()) {
            List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().eq(Stock::getBuyPlank, true));
            lock.lock();
            try {
                map.clear();
                for (Stock stock : stocks) {
                    if (!pendingOrderSet.contains(stock.getCode())) {
                        map.put(stock.getCode(), stock);
                        if (!runningSet.contains(stock.getCode())) {
                            Barbarossa.executorService.submit(new Task(stock));
                            runningSet.add(stock.getCode());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 当前时间是否是交易时间
     * <p>
     * 只判定了时分秒，没有判定非交易日（周末及法定节假日），因为我一般只交易日才会启动项目
     *
     * @return boolean
     */
    private boolean isTradeTime() {
        int hour = DateUtil.hour(new Date(), true);
        return (hour == 9 && DateUtil.minute(new Date()) > 30) || (hour == 11 && DateUtil.minute(new Date()) <= 29)
                || hour == 10 || hour == 13 || hour == 14;
    }

    @Override
    public void run(String... args) throws Exception {
        updatePlankMap();
    }

    class Task implements Runnable {

        private Stock stock;

        private AtomicBoolean buy = new AtomicBoolean(false);

        public Task(Stock stock) {
            this.stock = stock;
        }

        @Override
        public void run() {
            while (!buy.get() && isTradeTime()) {
                try {
                    automaticTrading(stock, buy);
                    Thread.sleep(200);
                    if (!lock.isLocked()) {
                        if (!map.containsKey(stock.getCode())) {
                            runningSet.remove(stock.getCode());
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void automaticTrading(Stock stock, AtomicBoolean buy) {
        String url = plankConfig.getXueQiuStockDetailUrl().replace("{code}", stock.getCode())
                .replace("{time}", String.valueOf(System.currentTimeMillis()))
                .replace("{recentDayNumber}", "1");
        String body = HttpUtil.getHttpGetResponseString(url, plankConfig.getXueQiuCookie());
        JSONObject data = JSON.parseObject(body).getJSONObject("data");
        JSONArray list = data.getJSONArray("item");
        if (CollectionUtils.isNotEmpty(list)) {
            Object o = list.get(0);
            // 实时价格
            double increaseRate = ((JSONArray) o).getDoubleValue(5);
            if (increaseRate >= stock.getBuyPrice().doubleValue()) {
                buy(stock, buy);
            }
        }
    }

    private void buy(Stock stock, AtomicBoolean buy) {
        SubmitRequest request = new SubmitRequest(1);
        request.setAmount(stock.getBuyAmount());
        request.setPrice(stock.getBuyPrice().doubleValue());
        request.setStockCode(stock.getCode().substring(2, 8));
        request.setZqmc(stock.getName());
        request.setTradeType(SubmitRequest.B);
        request.setMarket(StockUtil.getStockMarket(request.getStockCode()));
        TradeResultVo<SubmitResponse> response = tradeApiService.submit(request);
        if (response.success()) {
            runningSet.remove(stock.getCode());
            map.remove(stock.getCode());
            pendingOrderSet.add(stock.getCode());
            buy.set(true);
            stock.setBuyPlank(false);
            stockMapper.updateById(stock);
            log.info("成功下单[{}],数量:{},价格:{},message:{}", stock.getName(), stock.getBuyAmount(), stock.getBuyPrice().doubleValue(),
                    response.getData().get(0).getWtbh());
        } else {
            log.error("下单[{}]失败,message:{}", stock.getName(), response.getMessage());
        }
    }
}