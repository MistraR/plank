package com.mistra.plank.job;

import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.DragonListMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 抓取龙虎榜数据
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Slf4j
@Component
public class DragonListProcessor {

    private DailyRecordMapper dailyRecordMapper;
    private DragonListMapper dragonListMapper;

    public DragonListProcessor(DailyRecordMapper dailyRecordMapper, DragonListMapper dragonListMapper) {
        this.dailyRecordMapper = dailyRecordMapper;
        this.dragonListMapper = dragonListMapper;
    }

    public void run() {

    }
}
