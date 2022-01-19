package com.mistra.plank.job;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.ClearanceMapper;
import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.DragonListMapper;
import com.mistra.plank.mapper.HoldSharesMapper;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.mapper.TradeRecordMapper;
import com.mistra.plank.pojo.Clearance;
import com.mistra.plank.pojo.DailyRecord;
import com.mistra.plank.pojo.DragonList;
import com.mistra.plank.pojo.HoldShares;
import com.mistra.plank.pojo.Stock;
import com.mistra.plank.pojo.TradeRecord;
import com.mistra.plank.pojo.dto.StockSample;
import com.mistra.plank.pojo.enums.ClearanceReasonEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 巴巴罗萨计划
 *
 * @author mistra@future.com
 * @date 2021/11/19
 */
@Slf4j
@Component
public class Barbarossa implements CommandLineRunner {

    private final StockMapper stockMapper;
    private final DailyRecordMapper dailyRecordMapper;
    private final ClearanceMapper clearanceMapper;
    private final TradeRecordMapper tradeRecordMapper;
    private final HoldSharesMapper holdSharesMapper;
    private final DragonListMapper dragonListMapper;
    private final PlankConfig plankConfig;
    private final DailyRecordProcessor dailyRecordProcessor;
    private final DragonListProcessor dragonListProcessor;
    private final StockProcessor stockProcessor;

    private final String gemPlankStock = "中文在线,首都在线,扬杰科技,华安鑫创,任子行,捷成股份,科恒股份,中粮工科,春晖智控,华中数控,中集车辆,银邦股份,泰林生物,启迪设计,炬华科技,万讯自控,万集科技,川网传媒,康平科技,智莱科技,信息发展,奥联电子,观想科技,丝路视觉,达嘉维康,旗天科技,中泰股份,科拓生物,欣锐科技,德艺文创,山水比德,海泰科,派瑞股份,卡倍亿,华力创通,晶雪节能,宁波方正,聚灿光电,锋尚文化,海联讯,苏奥传感,绿岛风,金银河,易瑞生物,漱玉平民,商络电子,会畅通讯,耐普矿机,国安达,中青宝,雅本化学,冠中生态,东土科技,佐力药业,每日互动,奥雅设计,向日葵,润丰股份,帝尔激光,万里马,国联水产,红日药业,光庭信息,瑞纳智能,美晨生态,飞荣达,多瑞医药,建科院,粤万年青,正海磁材,扬电科技,富春股份,秋田微,中捷精工,爱司凯,通灵股份,博俊科技,方直科技,数字政通,乐心医疗,仟源医药,飞凯材料,中辰股份,艾比森,威唐工业,迦南科技,睿智医药,九强生物,三六五网,透景生命,华立科技,赛摩智能,山河药辅,新诺威,天舟文化,德迈仕,建研设计,安克创新,佳讯飞鸿,太龙股份,经纬辉开,GQY视讯,天泽信息,吉峰科技,海辰药业,安车检测,美瑞新材,仕净科技,华宝股份,致远新能,兰卫医学,裕兴股份,开元教育,雄帝科技,民德电子,天瑞仪器,新文化,国林科技,新开普,星辉娱乐,星徽股份,越博动力,贝斯特,陇神戎发,金运激光,网宿科技,左江科技,新光药业,宣亚国际,初灵信息,百胜智能,中科信息,华录百纳,读客文化,银河磁体,电声股份,洁雅股份,青松股份,通合科技,运达科技,通业科技,思特奇,盛天网络,邵阳液压,美康生物,宏昌科技,严牌股份,德固特,大地海洋,海顺新材,捷安高科,吉药控股,中孚信息,佳创视讯,恒信东方,东方日升,东华测试,康泰医学,中创环保,新国都,神思电子,曼卡龙,大宏立,康芝药业,立昂技术,联合光电,三丰智能,融捷健康,金百泽,鹏辉能源,金城医药,倍杰特,翰宇药业,拓新药业,海特生物,华研精机,广生堂,舒泰神,英可瑞,科蓝软件,大富科技,雅创电子,百川畅银,特发服务,景嘉微";

    private final String gemPlankStockTwice = "中文在线,中粮工科,春晖智控,泰林生物,炬华科技,川网传媒,康平科技,观想科技,丝路视觉,山水比德,海泰科,锋尚文化,海联讯,耐普矿机,雅本化学,奥雅设计,国联水产,红日药业,粤万年青,正海磁材,扬电科技,中捷精工,博俊科技,方直科技,仟源医药,中辰股份,迦南科技,赛摩智能,山河药辅,海辰药业,致远新能,兰卫医学,天瑞仪器,新开普,越博动力,陇神戎发,金运激光,左江科技,宣亚国际,中科信息,恒信东方,神思电子,康芝药业,鹏辉能源,翰宇药业,拓新药业,广生堂,舒泰神,大富科技";

    private final String fivePlank = "风范股份,龙津药业,京城股份,兰石重装,亚太药业,龙洲股份,富临运业,新筑股份,华塑股份,福然德,渝开发,九安医疗,万里股份,金山股份,冰山冷热,赛隆药业,蓝光发展,长江材料,翠微股份,湖南天雁,富佳股份,跃岭股份,内蒙新华,三羊马,大龙地产,亚世光电,陕西金叶,开开实业,顾地科技,延华智能,迪生力,蓝科高新,顺钠股份,永安期货,长城电工,镇洋发展,中锐股份,汇绿生态,美盛文化,中铝国际,湖北广电,英洛华,梦天家居,精华制药,新华联,西仪股份";

    /**
     * 一月11号上升趋势样本数据，分析11号加入自选到月底31号的涨幅情况
     */
    private final String monthOneSample = "华安鑫创,东华科技,天永智能,中曼石油,小熊电器,中银绒业,锡业股份,新宝股份,云图控股,远兴能源,国邦医药,盛航股份,杭氧股份,世运电路,电魂网络,赢时胜,惠泉啤酒,胜蓝股份,国药一致,桂林旅游,莱宝高科,美银森," +
            "盛讯达,力鼎光电,云内动力,天地在线,三全食品,华中数控,卫光生物,亚星锚链,誉衡药业,中嘉博创,中石科技,振东制药,天创时尚,吴通控股,佳隆股份,瑞普生物,四川美丰,紫天科技,京新药业,兆丰股份,优博讯,安妮股份,金马游乐,东宝生物,和佳医疗," +
            "仲景食品,天龙集团,景峰医药,数码视讯,诺邦股份,万辰生物,乐普医疗,理邦仪器,麦克奥迪,开能健康,新瀚新材,葫芦娃,常山药业,果麦文化,康跃科技,读客文化,济民医疗,迈克生物,阳普医疗";

    public static final HashMap<String, String> STOCK_MAP = new HashMap<>();

    /**
     * 总金额
     */
    public static BigDecimal BALANCE = new BigDecimal(1000000);
    /**
     * 可用金额
     */
    public static BigDecimal BALANCE_AVAILABLE = new BigDecimal(1000000);

    public Barbarossa(StockMapper stockMapper, DailyRecordMapper dailyRecordMapper, ClearanceMapper clearanceMapper,
                      TradeRecordMapper tradeRecordMapper, HoldSharesMapper holdSharesMapper,
                      DragonListMapper dragonListMapper, PlankConfig plankConfig, DailyRecordProcessor dailyRecordProcessor,
                      DragonListProcessor dragonListProcessor, StockProcessor stockProcessor) {
        this.stockMapper = stockMapper;
        this.dailyRecordMapper = dailyRecordMapper;
        this.clearanceMapper = clearanceMapper;
        this.tradeRecordMapper = tradeRecordMapper;
        this.holdSharesMapper = holdSharesMapper;
        this.dragonListMapper = dragonListMapper;
        this.plankConfig = plankConfig;
        this.dailyRecordProcessor = dailyRecordProcessor;
        this.dragonListProcessor = dragonListProcessor;
        this.stockProcessor = stockProcessor;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                .notLike("name", "%ST%")
                .notLike("name", "%st%")
                .notLike("name", "%A%")
                .notLike("name", "%C%")
                .notLike("name", "%N%")
                .notLike("name", "%U%")
                .notLike("name", "%W%")
                .notLike("code", "%BJ%")
                .notLike("code", "%688%")
        );
        stocks.forEach(stock -> STOCK_MAP.put(stock.getCode(), stock.getName()));
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>一共加载[{}]支股票！", stocks.size());
        BALANCE = new BigDecimal(plankConfig.getFunds());
        BALANCE_AVAILABLE = BALANCE;
//        this.collectData();
        this.analyzeSample();
//        this.barbarossa();
    }

    @Scheduled(cron = "0 0 23 * * ? ")
    private void collectData() throws Exception {
//        stockProcessor.run();
//        dragonListProcessor.run();
        dailyRecordProcessor.run();
    }

    private void analyzeSample() throws Exception {
//        analyze();
        analyzeAverageIncrease();
    }

    /**
     * 分析上升趋势样本的平均涨幅
     */
    private void analyzeAverageIncrease() {
        List<String> stockName = Arrays.asList(monthOneSample.split(","));
        Map<String, DailyRecord> join = dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>()
                .eq("date", new Date(plankConfig.getSampleDay())).in("name", stockName))
                .stream().collect(Collectors.toMap(DailyRecord::getName, e -> e));
        increaseDetail(join, stockName, 5, plankConfig.getSampleFiveDay());
        increaseDetail(join, stockName, 10, plankConfig.getSampleTenDay());
        increaseDetail(join, stockName, 15, plankConfig.getSampleFifteenDay());
        increaseDetail(join, stockName, 20, plankConfig.getSampleTwentyDay());
        increaseDetail(join, stockName, 25, plankConfig.getSampleTwentyFiveDay());
        increaseDetail(join, stockName, 30, plankConfig.getSampleThirtyDay());
    }

    private void increaseDetail(Map<String, DailyRecord> join, List<String> stockName, int day, Long time) {
        DecimalFormat df = new DecimalFormat("######0.000");
        Map<String, DailyRecord> sample = dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>()
                .eq("date", new Date(time)).in("name", stockName))
                .stream().collect(Collectors.toMap(DailyRecord::getName, e -> e));
        if (MapUtils.isNotEmpty(sample) && sample.size() > 0) {
            List<StockSample> list = new ArrayList<>();
            for (Map.Entry<String, DailyRecord> entry : sample.entrySet()) {
                if (join.containsKey(entry.getKey())) {
                    BigDecimal closePrice = entry.getValue().getClosePrice();
                    BigDecimal joinClosePrice = join.get(entry.getKey()).getClosePrice();
                    list.add(StockSample.builder().name(entry.getValue().getName())
                            .increase(closePrice.subtract(joinClosePrice).divide(joinClosePrice, 2, BigDecimal.ROUND_HALF_UP).doubleValue()).build());
                }
            }
            log.info("样本加入自选{}个交易日收盘价平均涨幅:{}", day, df.format(list.stream().mapToDouble(StockSample::getIncrease).average().getAsDouble()));
            Collections.sort(list);
            log.info("样本加入自选{}个交易日涨幅明细:{}", day, list.stream().map(stockSample ->
                    stockSample.getName() + stockSample.getIncrease()).collect(Collectors.toList()));
            double count = (double) list.stream().filter(stockSample -> stockSample.getIncrease() > 0).count();
            String winRate = df.format(count / list.size());
            log.info("样本加入自选{}个交易日收盘价胜率:{}", day, winRate);
        }
    }

    /**
     * 分析各连板晋级率
     */
    private void analyze() {
        List<String> gemPlankStockAdded = Arrays.asList(gemPlankStock.split(","));
        List<String> gemPlankStockTwiceAdded = Arrays.asList(gemPlankStockTwice.split(","));
        List<String> fivePlankAdded = Arrays.asList(fivePlank.split(","));
        // 5连板+的股票
        HashSet<String> fivePlankStock = new HashSet<>();
        // 创业板涨停的股票
        HashSet<String> gemPlankStock = new HashSet<>();
        HashMap<String, Integer> gemPlankStockNumber = new HashMap<>();
        String strDateFormat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        Date date = new DateTime(plankConfig.getAnalyzeTime()).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0)
                .withMillisOfSecond(0).toDate();
        //首板一进二胜率
        HashMap<String, BigDecimal> oneToTwo = new HashMap<>(64);
        //二板二进三胜率
        HashMap<String, BigDecimal> twoToThree = new HashMap<>(64);
        //三板三进四胜率
        HashMap<String, BigDecimal> threeToFour = new HashMap<>(64);
        //四板四进五胜率
        HashMap<String, BigDecimal> fourToFive = new HashMap<>(64);
        //五板五进六胜率
        HashMap<String, BigDecimal> fiveToSix = new HashMap<>(64);
        //六板六进七胜率
        HashMap<String, BigDecimal> sixToSeven = new HashMap<>(64);
        List<DailyRecord> dailyRecords = dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>()
                .ge("date", date));
        Map<String, List<DailyRecord>> dateListMap = dailyRecords.stream().collect(Collectors.groupingBy(dailyRecord -> sdf.format(dailyRecord.getDate())));
        //昨日首板
        HashMap<String, Double> yesterdayOne = new HashMap<>(64);
        //昨日二板
        HashMap<String, Double> yesterdayTwo = new HashMap<>(64);
        //昨日三板
        HashMap<String, Double> yesterdayThree = new HashMap<>(64);
        //昨日四板
        HashMap<String, Double> yesterdayFour = new HashMap<>(64);
        //昨日五板
        HashMap<String, Double> yesterdayFive = new HashMap<>(64);
        //昨日六板
        HashMap<String, Double> yesterdaySix = new HashMap<>(64);
        do {
            List<DailyRecord> records = dateListMap.get(sdf.format(date));
            if (CollectionUtils.isNotEmpty(records)) {
                //今日首板
                HashMap<String, Double> todayOne = new HashMap<>(64);
                //今日二板
                HashMap<String, Double> todayTwo = new HashMap<>(32);
                //今日三板
                HashMap<String, Double> todayThree = new HashMap<>(16);
                //今日四板
                HashMap<String, Double> todayFour = new HashMap<>(16);
                //今日五板
                HashMap<String, Double> todayFive = new HashMap<>(16);
                //今日六板
                HashMap<String, Double> todaySix = new HashMap<>(16);
                //今日七板
                HashMap<String, Double> todaySeven = new HashMap<>(16);
                for (DailyRecord dailyRecord : records) {
                    double v = dailyRecord.getIncreaseRate().doubleValue();
                    String name = dailyRecord.getName();
                    String code = dailyRecord.getCode();
                    if (code.contains("SZ30") && v > 19.4 && v < 21) {
                        gemPlankStock.add(name);
                        if (gemPlankStockNumber.containsKey(name)) {
                            gemPlankStockNumber.put(name, gemPlankStockNumber.get(name) + 1);
                        } else {
                            gemPlankStockNumber.put(name, 1);
                        }
                    }
                    if ((!code.contains("SZ30") && v > 9.4 && v < 11) || (code.contains("SZ30") && v > 19.4 && v < 21)) {
                        if (yesterdaySix.containsKey(name)) {
                            // 昨日的六板，今天继续板，进阶7板
                            todaySeven.put(dailyRecord.getName(), v);
                        } else if (yesterdayFive.containsKey(name)) {
                            // 昨日的五板，今天继续板，进阶6板
                            todaySix.put(dailyRecord.getName(), v);
                        } else if (yesterdayFour.containsKey(name)) {
                            // 昨日的四板，今天继续板，进阶5板
                            todayFive.put(dailyRecord.getName(), v);
                        } else if (yesterdayThree.containsKey(name)) {
                            // 昨日的三板，今天继续板，进阶4板
                            todayFour.put(dailyRecord.getName(), v);
                        } else if (yesterdayTwo.containsKey(name)) {
                            // 昨日的二板，今天继续板，进阶3板
                            todayThree.put(dailyRecord.getName(), v);
                        } else if (yesterdayOne.containsKey(name)) {
                            //昨日首板，今天继续板，进阶2板
                            todayTwo.put(dailyRecord.getName(), v);
                        } else if (!yesterdayOne.containsKey(name)) {
                            //昨日没有板，今日首板
                            todayOne.put(dailyRecord.getName(), v);
                        }
                    }
                }
                if (yesterdayOne.size() > 0) {
                    //一进二成功率
                    oneToTwo.put(sdf.format(date), new BigDecimal(todayTwo.size()).divide(new BigDecimal(yesterdayOne.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                if (yesterdayTwo.size() > 0) {
                    //二进三成功率
                    twoToThree.put(sdf.format(date), new BigDecimal(todayThree.size()).divide(new BigDecimal(yesterdayTwo.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                if (yesterdayThree.size() > 0) {
                    //三进四成功率
                    threeToFour.put(sdf.format(date), new BigDecimal(todayFour.size()).divide(new BigDecimal(yesterdayThree.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                if (yesterdayFour.size() > 0) {
                    //四进五成功率
                    fourToFive.put(sdf.format(date), new BigDecimal(todayFive.size()).divide(new BigDecimal(yesterdayFour.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                if (yesterdayFive.size() > 0) {
                    //五进六成功率
                    fiveToSix.put(sdf.format(date), new BigDecimal(todaySix.size()).divide(new BigDecimal(yesterdayFive.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                if (yesterdaySix.size() > 0) {
                    //六进七成功率
                    sixToSeven.put(sdf.format(date), new BigDecimal(todaySeven.size()).divide(new BigDecimal(yesterdaySix.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                log.info("\n-------------------------------------------------------------------------------------------{}日-------------------------------------------------------------------------------------------" +
                                "\n一板{}支:{}\n二板{}支:{}\n三板{}支:{}\n四板{}支:{}\n五板{}支:{}\n六板{}支:{}\n七板{}支:{}" +
                                "\n--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------", sdf.format(date),
                        todayOne.keySet().size(), new ArrayList<>(todayOne.keySet()),
                        todayTwo.keySet().size(), new ArrayList<>(todayTwo.keySet()),
                        todayThree.keySet().size(), new ArrayList<>(todayThree.keySet()),
                        todayFour.keySet().size(), new ArrayList<>(todayFour.keySet()),
                        todayFive.keySet().size(), new ArrayList<>(todayFive.keySet()),
                        todaySix.keySet().size(), new ArrayList<>(todaySix.keySet()),
                        todaySeven.keySet().size(), new ArrayList<>(todaySeven.keySet()));
                fivePlankStock.addAll(todayFive.keySet());
                yesterdayOne.clear();
                yesterdayOne.putAll(todayOne);
                yesterdayTwo.clear();
                yesterdayTwo.putAll(todayTwo);
                yesterdayThree.clear();
                yesterdayThree.putAll(todayThree);
                yesterdayFour.clear();
                yesterdayFour.putAll(todayFour);
                yesterdayFive.clear();
                yesterdayFive.putAll(todayFive);
                yesterdaySix.clear();
                yesterdaySix.putAll(todaySix);
            }
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
        log.info("当前分析时间段5连板+的股票:{}", fivePlankStock);
        log.info("当前分析时间段创业板有过涨停板的股票:{}", gemPlankStock);
        List<String> gemPlankStockTwice = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : gemPlankStockNumber.entrySet()) {
            if (entry.getValue() > 1) {
                gemPlankStockTwice.add(entry.getKey());
            }
        }
        log.info("当前分析时间段创业板涨停2次及以上的股票:{}", gemPlankStockTwice);
        fivePlankStock.removeAll(fivePlankAdded);
        gemPlankStock.removeAll(gemPlankStockAdded);
        gemPlankStockTwice.removeAll(gemPlankStockTwiceAdded);
        log.info("当前分析时间段还未加入自选的5连板+的股票:{}", fivePlankStock);
        log.info("当前分析时间段还未加入自选的创业板有过涨停板的股票:{}", gemPlankStock);
        log.info("当前分析时间段还未加入自选的创业板涨停2次及以上的股票:{}", gemPlankStockTwice);
        double one = 0d;
        for (Map.Entry<String, BigDecimal> entry : oneToTwo.entrySet()) {
            if (entry.getKey().equals(sdf.format(new Date()))) {
                log.info("{}日一进二胜率:{}", entry.getKey(), entry.getValue());
            }
            one += entry.getValue().doubleValue();
        }
        double two = 0d;
        for (Map.Entry<String, BigDecimal> entry : twoToThree.entrySet()) {
            if (entry.getKey().equals(sdf.format(new Date()))) {
                log.info("{}日二进三胜率:{}", entry.getKey(), entry.getValue());
            }
            two += entry.getValue().doubleValue();
        }
        double three = 0d;
        for (Map.Entry<String, BigDecimal> entry : threeToFour.entrySet()) {
            if (entry.getKey().equals(sdf.format(new Date()))) {
                log.info("{}日三进四胜率:{}", entry.getKey(), entry.getValue());
            }
            three += entry.getValue().doubleValue();
        }
        double four = 0d;
        for (Map.Entry<String, BigDecimal> entry : fourToFive.entrySet()) {
            if (entry.getKey().equals(sdf.format(new Date()))) {
                log.info("{}日四进五胜率:{}", entry.getKey(), entry.getValue());
            }
            four += entry.getValue().doubleValue();
        }
        double five = 0d;
        for (Map.Entry<String, BigDecimal> entry : fiveToSix.entrySet()) {
            if (entry.getKey().equals(sdf.format(new Date()))) {
                log.info("{}日五进六胜率:{}", entry.getKey(), entry.getValue());
            }
            five += entry.getValue().doubleValue();
        }
        double six = 0d;
        for (Map.Entry<String, BigDecimal> entry : sixToSeven.entrySet()) {
            if (entry.getKey().equals(sdf.format(new Date()))) {
                log.info("{}日六进七胜率:{}", entry.getKey(), entry.getValue());
            }
            six += entry.getValue().doubleValue();
        }
        if (oneToTwo.size() > 0) {
            log.info("一板>一进二平均胜率：{}", new BigDecimal(one).divide(new BigDecimal(oneToTwo.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
        if (twoToThree.size() > 0) {
            log.info("二板>二进三平均胜率：{}", new BigDecimal(two).divide(new BigDecimal(twoToThree.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
        if (threeToFour.size() > 0) {
            log.info("三板>三进四平均胜率：{}", new BigDecimal(three).divide(new BigDecimal(threeToFour.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
        if (fourToFive.size() > 0) {
            log.info("四板>四进五平均胜率：{}", new BigDecimal(four).divide(new BigDecimal(fourToFive.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
        if (fiveToSix.size() > 0) {
            log.info("五板>五进六平均胜率：{}", new BigDecimal(five).divide(new BigDecimal(fiveToSix.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
        if (sixToSeven.size() > 0) {
            log.info("六板>六进七平均胜率：{}", new BigDecimal(six).divide(new BigDecimal(sixToSeven.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
    }

    /**
     * 巴巴罗萨计划
     */
    private void barbarossa() throws Exception {
        Date date = new Date(plankConfig.getBeginDay());
        do {
            this.barbarossa(date);
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
    }

    private void barbarossa(Date date) {
        int week = DateUtil.dayOfWeek(date);
        if (week < 7 && week > 1) {
            // 工作日
            List<Stock> stocks = this.checkCanBuyStock(date);
            if (CollectionUtils.isNotEmpty(stocks) && BALANCE_AVAILABLE.intValue() > 10000) {
                this.buyStock(stocks, date);
            }
            this.sellStock(date);
        }
    }

    /**
     * 检查可以买的票
     * 首板或者2板  10日涨幅介于10-22%
     * 计算前8天的振幅在15%以内
     *
     * @return List<String>
     */
    private List<Stock> checkCanBuyStock(Date date) {
        List<DragonList> dragonLists = dragonListMapper.selectList(new QueryWrapper<DragonList>()
                .ge("date", date).le("date", date).ge("price", 6).le("price", 100)
                .notLike("name", "%ST%")
                .notLike("name", "%st%")
                .notLike("name", "%A%")
                .notLike("name", "%C%")
                .notLike("name", "%N%")
                .notLike("name", "%U%")
                .notLike("name", "%W%")
                .notLike("code", "%BJ%")
                .notLike("code", "%688%"));
        if (CollectionUtils.isEmpty(dragonLists)) {
            return null;
        }
        List<DailyRecord> dailyRecords = new ArrayList<>();
        for (DragonList dragonList : dragonLists) {
            Page<DailyRecord> page = dailyRecordMapper.selectPage(new Page<>(1, 30), new QueryWrapper<DailyRecord>()
                    .eq("code", dragonList.getCode()).le("date", date).ge("date", DateUtils.addDays(date, -30))
                    .orderByDesc("date"));
            if (page.getRecords().size() > 10) {
                dailyRecords.addAll(page.getRecords().subList(0, 10));
            }
        }
        Map<String, List<DailyRecord>> map = dailyRecords.stream().collect(Collectors.groupingBy(DailyRecord::getCode));
        List<String> stockCode = new ArrayList<>();
        for (Map.Entry<String, List<DailyRecord>> entry : map.entrySet()) {
            // 近8日涨幅
            BigDecimal eightRatio = entry.getValue().get(0).getClosePrice().divide(entry.getValue().get(8).getClosePrice(), 2);
            // 近3日涨幅
            BigDecimal threeRatio = entry.getValue().get(0).getClosePrice().divide(entry.getValue().get(3).getClosePrice(), 2);
            // 前3个交易日大跌的也排除
            if (eightRatio.doubleValue() <= 1.22 && eightRatio.doubleValue() >= 1.1 && threeRatio.doubleValue() < 1.22 &&
                    entry.getValue().get(0).getIncreaseRate().doubleValue() > 0.04 && entry.getValue().get(1).getIncreaseRate().doubleValue() > -0.04 &&
                    entry.getValue().get(2).getIncreaseRate().doubleValue() > -0.04) {
                stockCode.add(entry.getKey());
            }
        }
        dragonLists = dragonLists.stream().filter(dragonList -> stockCode.contains(dragonList.getCode())).collect(Collectors.toList());
        dragonLists = dragonLists.stream().sorted((a, b) -> b.getBuy().compareTo(a.getBuy())).collect(Collectors.toList());
        List<Stock> result = new ArrayList<>();
        for (DragonList dragonList : dragonLists) {
            result.add(stockMapper.selectOne(new QueryWrapper<Stock>().eq("code", dragonList.getCode())));
        }
        return result;
    }

    private void buyStock(List<Stock> stocks, Date date) {
        for (Stock stock : stocks) {
            List<HoldShares> holdShares = holdSharesMapper.selectList(new QueryWrapper<>());
            if (holdShares.size() >= plankConfig.getFundsPart()) {
                log.info("仓位已打满！");
                return;
            }
            Page<DailyRecord> selectPage = dailyRecordMapper.selectPage(new Page<>(1, 5), new QueryWrapper<DailyRecord>()
                    .eq("code", stock.getCode())
                    .ge("date", date)
                    .le("date", DateUtils.addDays(date, 12))
                    .orderByAsc("date"));
            if (selectPage.getRecords().size() < 2) {
                continue;
            }
            DailyRecord dailyRecord = selectPage.getRecords().get(1);
            double openRatio = (selectPage.getRecords().get(1).getOpenPrice().subtract(selectPage.getRecords().get(0).getClosePrice()))
                    .divide(selectPage.getRecords().get(0).getClosePrice(), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
            if (openRatio > -0.02 && openRatio < plankConfig.getBuyPlankRatioLimit().doubleValue() && BALANCE_AVAILABLE.intValue() > 10000) {
                // 低开2个点以下不买
                HoldShares one = holdSharesMapper.selectOne(new QueryWrapper<HoldShares>().eq("code", stock.getCode()));
                if (Objects.isNull(one)) {
                    int money = BALANCE.intValue() / plankConfig.getFundsPart();
                    money = Math.min(money, BALANCE_AVAILABLE.intValue());
                    int number = money / dailyRecord.getOpenPrice().multiply(new BigDecimal(100)).intValue();
                    double cost = number * 100 * dailyRecord.getOpenPrice().doubleValue();
                    BALANCE_AVAILABLE = BALANCE_AVAILABLE.subtract(new BigDecimal(cost));
                    HoldShares holdShare = HoldShares.builder().buyTime(DateUtils.addHours(dailyRecord.getDate(), 9)).code(stock.getCode()).name(stock.getName())
                            .cost(dailyRecord.getOpenPrice()).fifteenProfit(false).number(number * 100)
                            .profit(new BigDecimal(0)).currentPrice(dailyRecord.getOpenPrice()).rate(new BigDecimal(0))
                            .buyPrice(dailyRecord.getOpenPrice()).buyNumber(number * 100).build();
                    holdSharesMapper.insert(holdShare);
                    TradeRecord tradeRecord = new TradeRecord();
                    tradeRecord.setName(holdShare.getName());
                    tradeRecord.setCode(holdShare.getCode());
                    tradeRecord.setDate(DateUtils.addHours(dailyRecord.getDate(), 9));
                    tradeRecord.setMoney((int) (number * 100 * dailyRecord.getOpenPrice().doubleValue()));
                    tradeRecord.setReason("买入" + holdShare.getName() + number * 100 + "股，花费" + cost + "元，当前可用余额" +
                            BALANCE_AVAILABLE.intValue());
                    tradeRecord.setBalance(BALANCE.setScale(2, BigDecimal.ROUND_HALF_UP));
                    tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, BigDecimal.ROUND_HALF_UP));
                    tradeRecord.setPrice(dailyRecord.getOpenPrice());
                    tradeRecord.setNumber(number * 100);
                    tradeRecord.setType(0);
                    tradeRecordMapper.insert(tradeRecord);
                }
            }
        }
    }

    /**
     * 减仓或清仓股票
     *
     * @param date 日期
     */
    private void sellStock(Date date) {
        List<HoldShares> holdShares = holdSharesMapper.selectList(new QueryWrapper<>());
        if (CollectionUtils.isNotEmpty(holdShares)) {
            for (HoldShares holdShare : holdShares) {
                if (!DateUtils.isSameDay(holdShare.getBuyTime(), date) && holdShare.getBuyTime().getTime() < date.getTime()) {
                    Page<DailyRecord> selectPage = dailyRecordMapper.selectPage(new Page<>(1, 20), new QueryWrapper<DailyRecord>()
                            .eq("code", holdShare.getCode())
                            .ge("date", DateUtils.addDays(date, -plankConfig.getDeficitMovingAverage() - 9))
                            .le("date", date)
                            .orderByDesc("date"));
                    List<DailyRecord> dailyRecords = selectPage.getRecords().subList(0, 5);
                    // 今日数据明细
                    DailyRecord todayRecord = dailyRecords.get(0);
                    // 5日均线价格
                    OptionalDouble average = dailyRecords.stream().mapToDouble(dailyRecord -> dailyRecord.getClosePrice().doubleValue()).average();
                    if (average.isPresent() && (todayRecord.getLowest().doubleValue() <= average.getAsDouble())) {
                        // 跌破均线，清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.BREAK_POSITION, date, average.getAsDouble());
                        continue;
                    }

                    // 盘中最低收益率
                    double profitLowRatio = todayRecord.getLowest().subtract(holdShare.getBuyPrice()).divide(holdShare.getBuyPrice(), 2).doubleValue();
                    if (profitLowRatio < plankConfig.getDeficitRatio().doubleValue()) {
                        // 跌破止损线，清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.BREAK_LOSS_LINE, date,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getDeficitRatio().doubleValue()));
                        continue;
                    }

                    if (holdShare.getFifteenProfit() && profitLowRatio <= plankConfig.getProfitClearanceRatio().doubleValue()) {
                        // 收益回撤到10个点止盈清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.TAKE_PROFIT, date, holdShare.getBuyPrice().doubleValue() * 1.1);
                        continue;
                    }

                    // 盘中最高收益率
                    double profitHighRatio = todayRecord.getHighest().subtract(holdShare.getBuyPrice()).divide(holdShare.getBuyPrice(), 2).doubleValue();
                    if (profitHighRatio >= plankConfig.getProfitUpperRatio().doubleValue()) {
                        // 收益25% 清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.PROFIT_UPPER, date,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getProfitUpperRatio().doubleValue()));
                    } else if (profitHighRatio >= plankConfig.getProfitQuarterRatio().doubleValue()) {
                        // 收益20% 减至1/4仓
                        this.reduceStock(holdShare, ClearanceReasonEnum.POSITION_QUARTER, date, todayRecord,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getProfitQuarterRatio().doubleValue()));
                    } else if (profitHighRatio >= plankConfig.getProfitHalfRatio().doubleValue()) {
                        // 收益15% 减半仓
                        this.reduceStock(holdShare, ClearanceReasonEnum.POSITION_HALF, date, todayRecord,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getProfitHalfRatio().doubleValue()));
                    }

                    // 持股超过8天 清仓
                    if (Days.daysBetween(new LocalDate(holdShare.getBuyTime().getTime()), new LocalDate(date.getTime())).getDays() > plankConfig.getClearanceDay()) {
                        this.clearanceStock(holdShare, ClearanceReasonEnum.TEN_DAY, date, todayRecord.getOpenPrice().add(todayRecord.getClosePrice()).doubleValue() / 2);
                    }
                }
            }
        }
    }

    /**
     * 减仓股票
     *
     * @param holdShare           持仓记录
     * @param clearanceReasonEnum 清仓原因
     * @param date                时间
     * @param sellPrice           清仓价格
     */
    private void reduceStock(HoldShares holdShare, ClearanceReasonEnum clearanceReasonEnum, Date date, DailyRecord todayRecord, double sellPrice) {
        if (holdShare.getNumber() <= 0) {
            holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
            return;
        }
        // 卖出数量
        int number = holdShare.getNumber() <= 100 ? 100 : holdShare.getNumber() / 2;
        // 卖出金额
        double money = number * sellPrice;
        // 本次卖出部分盈利金额
        BigDecimal profit = new BigDecimal(number * (sellPrice - holdShare.getBuyPrice().doubleValue()));

        // 可用金额
        BALANCE_AVAILABLE = BALANCE_AVAILABLE.add(new BigDecimal(money));
        TradeRecord tradeRecord = new TradeRecord();
        tradeRecord.setName(holdShare.getName());
        tradeRecord.setCode(holdShare.getCode());
        tradeRecord.setDate(date);
        tradeRecord.setMoney((int) money);
        tradeRecord.setReason("减仓" + holdShare.getName() + number + "股，卖出金额" + (int) money + "元，当前可用余额" + BALANCE_AVAILABLE.intValue() +
                "，减仓原因" + clearanceReasonEnum.getDesc());
        tradeRecord.setBalance(BALANCE.setScale(2, BigDecimal.ROUND_HALF_UP));
        tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, BigDecimal.ROUND_HALF_UP));
        tradeRecord.setPrice(new BigDecimal(sellPrice));
        tradeRecord.setNumber(number);
        tradeRecord.setType(1);
        tradeRecordMapper.insert(tradeRecord);
        if (holdShare.getNumber() - number == 0) {
            holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
            return;
        }
        holdShare.setNumber(holdShare.getNumber() - number);
        holdShare.setCost(holdShare.getBuyPrice().multiply(new BigDecimal(holdShare.getBuyNumber())).subtract(profit).divide(new BigDecimal(number), 2));
        holdShare.setProfit(holdShare.getProfit().add(profit));
        holdShare.setFifteenProfit(true);
        holdShare.setCurrentPrice(todayRecord.getClosePrice());
        holdShare.setRate(todayRecord.getClosePrice().subtract(holdShare.getBuyPrice()).divide(holdShare.getBuyPrice(), 2));
        holdSharesMapper.updateById(holdShare);
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>减仓{},目前盈利{}元!", holdShare.getName(), holdShare.getProfit().add(profit).intValue());
    }

    /**
     * 清仓股票
     *
     * @param holdShare           持仓记录
     * @param clearanceReasonEnum 清仓原因
     * @param date                时间
     * @param sellPrice           清仓价格
     */
    private void clearanceStock(HoldShares holdShare, ClearanceReasonEnum clearanceReasonEnum, Date date, double sellPrice) {
        if (holdShare.getNumber() <= 0) {
            holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
            return;
        }
        // 卖出金额
        double money = holdShare.getNumber() * sellPrice;
        // 本次卖出部分盈利金额
        BigDecimal profit = new BigDecimal(holdShare.getNumber() * (sellPrice - holdShare.getBuyPrice().doubleValue()));
        // 总盈利
        profit = holdShare.getProfit().add(profit);
        // 总资产
        BALANCE = BALANCE.add(profit);
        // 可用金额
        BALANCE_AVAILABLE = BALANCE_AVAILABLE.add(new BigDecimal(money));
        TradeRecord tradeRecord = new TradeRecord();
        tradeRecord.setName(holdShare.getName());
        tradeRecord.setCode(holdShare.getCode());
        tradeRecord.setDate(date);
        tradeRecord.setMoney((int) money);
        tradeRecord.setReason("清仓" + holdShare.getName() + holdShare.getNumber() + "股，卖出金额" + (int) money + "元，当前可用余额" + BALANCE_AVAILABLE.intValue()
                + "，清仓原因" + clearanceReasonEnum.getDesc());
        tradeRecord.setBalance(BALANCE.setScale(2, BigDecimal.ROUND_HALF_UP));
        tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, BigDecimal.ROUND_HALF_UP));
        tradeRecord.setPrice(new BigDecimal(sellPrice));
        tradeRecord.setNumber(holdShare.getNumber());
        tradeRecord.setType(1);
        tradeRecordMapper.insert(tradeRecord);
        String strDateFormat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        Clearance clearance = new Clearance();
        clearance.setCode(holdShare.getCode());
        clearance.setName(holdShare.getName());
        clearance.setCostPrice(holdShare.getBuyPrice());
        clearance.setNumber(holdShare.getBuyNumber());
        clearance.setPrice(new BigDecimal(sellPrice));
        clearance.setRate(profit.divide(new BigDecimal(holdShare.getBuyNumber() * holdShare.getBuyPrice().doubleValue()), 2));
        clearance.setProfit(profit);
        clearance.setReason("清仓" + holdShare.getName() + "总计盈亏" + profit.intValue() + "元，清仓原因:" +
                clearanceReasonEnum.getDesc() + "建仓日期" + sdf.format(holdShare.getBuyTime()));
        clearance.setDate(date);
        clearance.setBalance(BALANCE.setScale(2, BigDecimal.ROUND_HALF_UP));
        clearance.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, BigDecimal.ROUND_HALF_UP));
        clearance.setDayNumber(Days.daysBetween(new LocalDate(holdShare.getBuyTime().getTime()), new LocalDate(date.getTime())).getDays());
        clearanceMapper.insert(clearance);
        holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>清仓{},总共盈利{}元!总资产:[{}]", holdShare.getName(), profit.intValue(), BALANCE.intValue());
    }

}
