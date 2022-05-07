package com.mistra.plank.job;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.DragonListMapper;
import com.mistra.plank.pojo.entity.DragonList;
import com.mistra.plank.util.HttpUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 抓取每日龙虎榜数据，只取净买入额前20
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Slf4j
@Component
public class DragonListProcessor {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final PlankConfig plankConfig;
    private final DragonListMapper dragonListMapper;

    public DragonListProcessor(PlankConfig plankConfig, DragonListMapper dragonListMapper) {
        this.plankConfig = plankConfig;
        this.dragonListMapper = dragonListMapper;
    }

    public void run() {
        Date date = new Date(plankConfig.getDragonListTime());
        do {
            this.execute(date, sdf.format(date));
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
    }

    private void execute(Date date, String timeStr) {
        try {
            String url = plankConfig.getDragonListUrl().replace("{time}", timeStr);
            String body = HttpUtil.getHttpGetResponseString(url, null);
            body = body.substring(body.indexOf("(") + 1, body.indexOf(")"));
            JSONObject data = JSON.parseObject(body).getJSONObject("result");
            if (Objects.isNull(data)) {
                return;
            }
            log.info("抓取{}日龙虎榜数据！", sdf.format(date));
            JSONArray list = data.getJSONArray("data");
            List<DragonList> dragonLists = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(list)) {
                JSONObject stock;
                for (Object o : list) {
                    try {
                        stock = (JSONObject)o;
                        DragonList dragonList = new DragonList();
                        dragonList.setDate(date);
                        String[] split = stock.getString("SECUCODE").split("\\.");
                        dragonList.setCode(split[1] + split[0]);
                        dragonList.setName(stock.getString("SECURITY_NAME_ABBR"));
                        dragonList.setNetBuy(stock.getBigDecimal("BILLBOARD_NET_AMT").longValue());
                        BigDecimal buyAmt = stock.getBigDecimal("BILLBOARD_BUY_AMT");
                        dragonList.setBuy(Objects.nonNull(buyAmt) ? buyAmt.longValue() : 0);
                        BigDecimal sellAmt = stock.getBigDecimal("BILLBOARD_SELL_AMT");
                        dragonList.setSell(Objects.nonNull(sellAmt) ? sellAmt.longValue() : 0);
                        dragonList.setPrice(stock.getBigDecimal("CLOSE_PRICE"));
                        dragonList.setMarketValue(stock.getBigDecimal("FREE_MARKET_CAP").longValue());
                        dragonList.setAccumAmount(stock.getBigDecimal("ACCUM_AMOUNT").longValue());
                        dragonList.setChangeRate(stock.getBigDecimal("CHANGE_RATE").setScale(2, RoundingMode.HALF_UP));
                        dragonLists.add(dragonList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            Map<String, List<DragonList>> collect =
                dragonLists.stream().collect(Collectors.groupingBy(DragonList::getCode));
            for (Map.Entry<String, List<DragonList>> entry : collect.entrySet()) {
                dragonListMapper.insert(entry.getValue().get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
