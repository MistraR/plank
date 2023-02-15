package com.mistra.plank.job;

import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.dao.HoldSharesMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.service.TradeApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2023/2/15 13:17
 * @ Description: 自动打板交易，开启的话会监控成交额大于3亿的全市场股票，发现上板则会自动下单排队
 */
@Slf4j
@Component
public class AutomaticPlankTrading implements CommandLineRunner {

    private final StockMapper stockMapper;
    private final TradeApiService tradeApiService;

    private final PlankConfig plankConfig;
    private final StockProcessor stockProcessor;
    private final HoldSharesMapper holdSharesMapper;

    public AutomaticPlankTrading(StockMapper stockMapper, TradeApiService tradeApiService,
                                 PlankConfig plankConfig, StockProcessor stockProcessor,
                                 HoldSharesMapper holdSharesMapper) {
        this.stockMapper = stockMapper;
        this.tradeApiService = tradeApiService;
        this.plankConfig = plankConfig;
        this.stockProcessor = stockProcessor;
        this.holdSharesMapper = holdSharesMapper;
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
