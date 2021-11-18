package com.mistra.plank.job;

import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.DragonListMapper;
import com.mistra.plank.mapper.HoldSharesMapper;
import com.mistra.plank.mapper.TradeRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Slf4j
@Component
public class Rocket implements CommandLineRunner {

    @Autowired
    private DailyRecordMapper dailyRecordMapper;

    @Autowired
    private DragonListMapper dragonListMapper;

    @Autowired
    private HoldSharesMapper holdSharesMapper;

    @Autowired
    private TradeRecordMapper tradeRecordMapper;


    @Override
    public void run(String... args) throws Exception {

    }
}
