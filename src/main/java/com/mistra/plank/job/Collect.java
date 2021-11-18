package com.mistra.plank.job;

import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.DragonListMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Slf4j
@Component
public class Collect implements CommandLineRunner {

    private DailyRecordMapper dailyRecordMapper;

    private DragonListMapper dragonListMapper;

    public Collect(DailyRecordMapper dailyRecordMapper) {
        this.dailyRecordMapper = dailyRecordMapper;
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
