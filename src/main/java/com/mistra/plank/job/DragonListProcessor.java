package com.mistra.plank.job;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.DragonListMapper;
import com.mistra.plank.pojo.DragonList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

/**
 * 抓取每日龙虎榜数据，只取净买入额前20
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Slf4j
@Component
public class DragonListProcessor {

    private final PlankConfig plankConfig;
    private final DragonListMapper dragonListMapper;

    public DragonListProcessor(PlankConfig plankConfig, DragonListMapper dragonListMapper) {
        this.plankConfig = plankConfig;
        this.dragonListMapper = dragonListMapper;
    }

    public void run() throws IOException {
        Date date = new Date(1637659454000L);
        String strDateFormat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        do {
            this.execute(date, sdf.format(date));
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
    }

    private void execute(Date date, String timeStr) throws IOException {
        try {

            String url = plankConfig.getDragonListUrl().replace("{time}", timeStr);
            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(URI.create(url));
            CloseableHttpResponse response = defaultHttpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String body = "";
            if (entity != null) {
                body = EntityUtils.toString(entity, "UTF-8");
            }
            body = body.substring(body.indexOf("(") + 1, body.indexOf(")"));
            JSONObject data = JSON.parseObject(body).getJSONObject("result");
            if (Objects.isNull(data)) {
                return;
            }
            log.info("抓取{}日龙虎榜数据！", date);
            JSONArray list = data.getJSONArray("data");
            List<DragonList> dragonLists = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(list)) {
                JSONObject stock = new JSONObject();
                for (Object o : list) {
                    try {
                        stock = (JSONObject) o;
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
                        dragonList.setChangeRate(stock.getBigDecimal("CHANGE_RATE").setScale(2, BigDecimal.ROUND_HALF_UP));
                        dragonLists.add(dragonList);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            Map<String, List<DragonList>> collect = dragonLists.stream().collect(Collectors.groupingBy(DragonList::getCode));
            for (Map.Entry<String, List<DragonList>> entry : collect.entrySet()) {
                dragonListMapper.insert(entry.getValue().get(0));
            }
        } catch (Exception e) {
        }
    }
}
