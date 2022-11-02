package com.mistra.plank.service.impl;

import com.mistra.plank.tradeapi.TradeResultVo;
import com.mistra.plank.tradeapi.request.*;
import com.mistra.plank.tradeapi.response.*;
import com.mistra.plank.common.config.SpringUtil;
import com.mistra.plank.strategy.StrategyHandler;
import com.mistra.plank.dao.impl.ExecuteInfoDao;
import com.mistra.plank.model.entity.*;
import com.mistra.plank.model.vo.PageParam;
import com.mistra.plank.model.vo.PageVo;
import com.mistra.plank.model.vo.TaskVo;
import com.mistra.plank.model.vo.trade.TradeRuleVo;
import com.mistra.plank.service.*;
import com.mistra.plank.common.util.DecimalUtil;
import com.mistra.plank.common.util.StockConsts;
import com.mistra.plank.common.util.StockUtil;
import com.mistra.plank.common.util.TradeUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final Map<String, BigDecimal> lastPriceMap = new HashMap<>();

    @Value("${ocr.service}")
    private String ocrServiceName;

    @Autowired
    private HolidayCalendarService holidayCalendarService;

    @Autowired
    private ExecuteInfoDao executeInfoDao;

    @Autowired
    private StockCrawlerService stockCrawlerService;

    @Autowired
    private StockInfoService stockService;

    @Autowired
    private MessageService messageServicve;

    @Autowired
    private StockSelectedService stockSelectedService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeApiService tradeApiService;

    @Autowired
    private SystemConfigService systemConfigService;

    private static final boolean CrawIndexFromSina = false;

    @Override
    public List<ExecuteInfo> getTaskListById(int... id) {
        return executeInfoDao.getByTaskIdAndState(id, null);
    }

    @Override
    public List<ExecuteInfo> getPendingTaskListById(int... id) {
        return executeInfoDao.getByTaskIdAndState(id, StockConsts.TaskState.Pending.value());
    }

    @Override
    public void executeTask(ExecuteInfo executeInfo) {
        executeInfo.setStartTime(new Date());
        executeInfo.setMessage("");
        int id = executeInfo.getTaskId();
        Task task = Task.valueOf(id);
        try {
            switch (task) {
                case BeginOfYear:
                    holidayCalendarService.updateCurrentYear();
                    break;
                case BeginOfDay:
                    lastPriceMap.clear();
                    break;
                case UpdateOfStock:
                    runUpdateOfStock();
                    break;
                case UpdateOfDailyIndex:
                    runUpdateOfDailyIndex();
                    break;
                case Ticker:
                    runTicker();
                    break;
                case TradeTicker:
                    runTradeTicker();
                    break;
                case ApplyNewStock:
                    runApplyNewStock();
                    break;
                case AutoLogin:
                    autoLogin();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            executeInfo.setMessage(e.getMessage());
            logger.error("task {} {} error", task.getName(), executeInfo.getId(), e);

            String body = String.format("task: %s, error: %s", task.getName(), e.getMessage());
            messageServicve.send(body);
        }

        executeInfo.setCompleteTime(new Date());
        executeInfoDao.update(executeInfo);
    }

    private void autoLogin() {
        final int userId = 1;
        TradeUser tradeUser = tradeService.getTradeUserById(userId);
        TradeMethod tradeMethod = tradeService.getTradeMethodByName(BaseTradeRequest.TradeRequestMethod.YZM.value());

        String randNum = "0.903" + new Date().getTime();
        String yzmUrl = tradeMethod.getUrl() + randNum;

        OcrService ocrService = SpringUtil.getBean(ocrServiceName, OcrService.class);
        String identifyCode = ocrService.process(yzmUrl);

        AuthenticationRequest request = new AuthenticationRequest(tradeUser.getId().intValue());
        request.setIdentifyCode(identifyCode);
        request.setRandNumber(randNum);
        request.setPassword(tradeUser.getPassword());

        TradeResultVo<AuthenticationResponse> resultVo = tradeApiService.authentication(request);
        if (resultVo.success()) {
            AuthenticationResponse response = resultVo.getData().get(0);
            tradeUser.setCookie(response.getCookie());
            tradeUser.setValidateKey(response.getValidateKey());
            tradeService.updateTradeUser(tradeUser);
        } else {
            logger.error("auto login {} {}", request, resultVo.getMessage());
            throw new RuntimeException("auto login failed");
        }

    }

    private void runUpdateOfStock() {
        List<StockInfo> list = stockService.getAll().stream().filter(v -> !v.isIndex()).collect(Collectors.toList());
        Map<String, List<StockInfo>> dbStockMap = list.stream().collect(Collectors.groupingBy(StockInfo::getCode));

        ArrayList<StockInfo> needAddedList = new ArrayList<>();
        ArrayList<StockInfo> needUpdatedList = new ArrayList<>();
        ArrayList<StockLog> stockLogList = new ArrayList<>();

        final Date date = new Date();

        List<StockInfo> crawlerList = stockCrawlerService.getStockList();
        for (StockInfo stockInfo : crawlerList) {
            StockConsts.StockLogType stocLogType = null;
            List<StockInfo> stockGroupList = dbStockMap.get(stockInfo.getCode());
            String oldValue = null;
            String newValue = null;
            if (stockGroupList == null) {
                stocLogType = StockConsts.StockLogType.New;
                oldValue = "";
                newValue = stockInfo.getName();
            } else {
                StockInfo stockInfoInDb = stockGroupList.get(0);
                if (!stockInfo.getName().equals(stockInfoInDb.getName())
                        && StockUtil.isOriName(stockInfo.getName())) {
                    stocLogType = StockConsts.StockLogType.Rename;
                    oldValue = stockInfoInDb.getName();
                    newValue = stockInfo.getName();
                    stockInfo.setId(stockInfoInDb.getId());
                }
            }

            if (stocLogType != null) {
                StockLog stockLog = new StockLog(stockInfo.getId().intValue(), date, stocLogType.value(), oldValue, newValue);
                if (stocLogType == StockConsts.StockLogType.New) {
                    needAddedList.add(stockInfo);
                } else {
                    needUpdatedList.add(stockInfo);
                }
                stockLogList.add(stockLog);
            }
        }

        stockService.update(needAddedList, needUpdatedList, stockLogList);
    }

    private void runUpdateOfDailyIndex() {
        List<StockInfo> list = stockService.getAll().stream()
                .filter(stockInfo -> (stockInfo.isA() || stockInfo.isIndex()) && stockInfo.isValid())
                .collect(Collectors.toList());

        Date date = new Date();

        List<DailyIndex> dailyIndexList = stockService.getDailyIndexListByDate(date);
        List<String> codeList = dailyIndexList.stream().map(DailyIndex::getCode).collect(Collectors.toList());
        list = list.stream().filter(v -> !codeList.contains(v.getFullCode())).collect(Collectors.toList());

        if (CrawIndexFromSina) {
            crawDailyIndexFromSina(list);
        } else {
            crawDailyIndexFromSina(list.stream().filter(StockInfo::isIndex).collect(Collectors.toList()));
            crawDailyIndexFromEastMoney(list);
        }
    }

    private void crawDailyIndexFromEastMoney(List<StockInfo> list) {
        List<DailyIndex> dailyIndexList = stockCrawlerService.getDailyIndexFromEastMoney();
        dailyIndexList = dailyIndexList.stream().filter(d -> list.stream().anyMatch(s -> d.getCode().equals(s.getFullCode()))).collect(Collectors.toList());
        stockService.saveDailyIndex(filterInvalid(dailyIndexList));
    }

    private void crawDailyIndexFromSina(List<StockInfo> list) {
        final int tCount = 500;
        ArrayList<String> stockCodeList = new ArrayList<>(tCount);
        for (StockInfo stockInfo : list) {
            stockCodeList.add(stockInfo.getFullCode());
            if (stockCodeList.size() == tCount) {
                saveDailyIndex(stockCodeList);
                stockCodeList.clear();
            }
        }

        if (!stockCodeList.isEmpty()) {
            saveDailyIndex(stockCodeList);
        }
    }

    private void saveDailyIndex(ArrayList<String> stockCodeList) {
        List<DailyIndex> dailyIndexList = stockCrawlerService.getDailyIndex(stockCodeList);
        stockService.saveDailyIndex(filterInvalid(dailyIndexList));
    }

    private List<DailyIndex> filterInvalid(List<DailyIndex> dailyIndexList) {
        final String currentDateStr = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        return dailyIndexList.stream().filter(dailyIndex ->
                DecimalUtil.bg(dailyIndex.getOpeningPrice(), BigDecimal.ZERO)
                        && dailyIndex.getTradingVolume() > 0
                        && DecimalUtil.bg(dailyIndex.getTradingValue(), BigDecimal.ZERO)
                        && currentDateStr.equals(DateFormatUtils.format(dailyIndex.getDate(), "yyyy-MM-dd"))
        ).collect(Collectors.toList());
    }

    private void runTicker() {
        List<StockSelected> selectList = stockSelectedService.getList();
        List<String> codeList = selectList.stream().map(v -> StockUtil.getFullCode(v.getCode())).collect(Collectors.toList());
        List<DailyIndex> dailyIndexList = stockCrawlerService.getDailyIndex(codeList);

        StringBuilder sb = new StringBuilder();
        for (StockSelected stockSelected : selectList) {
            String code = stockSelected.getCode();
            DailyIndex dailyIndex = dailyIndexList.stream().filter(d -> d.getCode().contains(stockSelected.getCode())).findAny().orElse(null);
            if (dailyIndex == null) {
                continue;
            }
            if (lastPriceMap.containsKey(code)) {
                BigDecimal lastPrice = lastPriceMap.get(code);
                double rate = Math.abs(StockUtil.calcIncreaseRate(dailyIndex.getClosingPrice(), lastPrice).doubleValue());
                if (Double.compare(rate, stockSelected.getRate().doubleValue()) >= 0) {
                    lastPriceMap.put(code, dailyIndex.getClosingPrice());
                    String name = stockService.getStockByFullCode(StockUtil.getFullCode(code)).getName();
                    String body = String.format("%s:当前价格:%.03f, 涨幅%.02f%%", name,
                            dailyIndex.getClosingPrice().doubleValue(),
                            StockUtil.calcIncreaseRate(dailyIndex.getClosingPrice(),
                                    dailyIndex.getPreClosingPrice()).movePointRight(2).doubleValue());
                    sb.append(body + "\n");
                }
            } else {
                lastPriceMap.put(code, dailyIndex.getPreClosingPrice());
                String name = stockService.getStockByFullCode(StockUtil.getFullCode(code)).getName();
                String body = String.format("%s:当前价格:%.03f", name, dailyIndex.getClosingPrice().doubleValue());
                sb.append(body + "\n");
            }
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
            messageServicve.send(sb.toString());
        }
    }

    private void runTradeTicker() {
        List<TradeUser> userList = tradeService.getTradeUserList();
        for (TradeUser tradeUser : userList) {
            runStrategy(tradeUser.getId());
            runDealNotice(tradeUser.getId());
        }
    }

    private void runApplyNewStock() {
        List<TradeUser> userList = tradeService.getTradeUserList();
        for (TradeUser tradeUser : userList) {
            applyNewStock(tradeUser.getId());
            if (systemConfigService.isCr()) {
                applyNewStockCr(tradeUser.getId().intValue());
            }
        }
    }

    private void runStrategy(Long userId) {
        PageParam pageParam = new PageParam();
        pageParam.setStart(0);
        pageParam.setLength(Integer.MAX_VALUE);
        PageVo<TradeRuleVo> pageVo = tradeService.getTradeRuleList(pageParam);

        pageVo.getData().forEach(v -> {
            if (v.isValid()) {
                String beanName = v.getStrategyBeanName();
                StrategyHandler strategyHandler = SpringUtil.getBean(beanName, StrategyHandler.class);
                try {
                    strategyHandler.handle(v);
                } catch (Exception e) {
                    logger.error("strategyHandler {} {} error", v.getStockCode(), v.getStrategyName(), e);
                }
            }
        });
    }

    private void runDealNotice(Long i) {
        List<TradeUser> userList = tradeService.getTradeUserList();
        StringBuilder sb = new StringBuilder();

        for (TradeUser tradeUser : userList) {
            ArrayList<GetDealDataResponse> list = new ArrayList<>();

            TradeResultVo<GetDealDataResponse> dealData = tradeApiService.getDealData(new GetDealDataRequest(tradeUser.getId().intValue()));
            if (!dealData.success()) {
                logger.error("runDealNotice error {}", dealData.getMessage());
                return;
            }

            TradeResultVo<CrGetDealDataResponse> crDealData = tradeApiService.crGetDealData(new CrGetDealDataRequest(tradeUser.getId().intValue()));
            if (!dealData.success()) {
                logger.error("runDealNotice error {}", dealData.getMessage());
                return;
            }

            list.addAll(TradeUtil.mergeDealList(dealData.getData()));
            list.addAll(TradeUtil.mergeDealList(crDealData.getData()));

            List<TradeDeal> tradeDealList = tradeService.getTradeDealListByDate(new Date());
            List<String> dealCodeList = tradeDealList.stream().map(TradeDeal::getDealCode).collect(Collectors.toList());

            List<TradeDeal> needNotifyList = list.stream().filter(v -> !dealCodeList.contains(v.getCjbh())).map(v -> {
                TradeDeal tradeDeal = new TradeDeal();
                tradeDeal.setDealCode(v.getCjbh());
                tradeDeal.setPrice(new BigDecimal(v.getCjjg()));
                tradeDeal.setStockCode(v.getZqdm());

                Date tradeTime = new Date();
                tradeTime = DateUtils.setHours(tradeTime, Integer.valueOf(v.getCjsj().substring(0, 2)));
                tradeTime = DateUtils.setMinutes(tradeTime, Integer.valueOf(v.getCjsj().substring(2, 4)));
                tradeTime = DateUtils.setSeconds(tradeTime, Integer.valueOf(v.getCjsj().substring(4, 6)));

                tradeDeal.setTradeTime(tradeTime);
                tradeDeal.setTradeType(v.getMmlb());
                tradeDeal.setVolume(Integer.parseInt(v.getCjsl()));

                if (v instanceof CrGetDealDataResponse) {
                    CrGetDealDataResponse deal = (CrGetDealDataResponse) v;
                    tradeDeal.setCrTradeType(deal.getXyjylx());
                } else {
                    tradeDeal.setCrTradeType("");
                }

                sb.append(String.format("%s deal %s %s %s %s %s\n",
                        tradeDeal.getCrTradeType().length() > 0 ? "cr" : "normal", v.getFormatDealTime(), v.getZqmc(), v.getMmlb(), v.getCjjg(), v.getCjsl()));

                return tradeDeal;
            }).collect(Collectors.toList());

            tradeService.saveTradeDealList(needNotifyList);
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
            messageServicve.send(sb.toString());
        }
    }

    private void applyNewStock(Long userId) {
        GetCanBuyNewStockListV3Response getCanBuyResponse = tradeApiService.getCanBuyNewStockListV3(new GetCanBuyNewStockListV3Request(userId.intValue())).getData().get(0);

        List<SubmitBatTradeV2Request.SubmitData> newStockList = getCanBuyResponse.getNewStockList().stream()
                .filter(newStock -> getCanBuyResponse.getNewQuota().stream().anyMatch(v -> v.getMarket().equals(newStock.getMarket())))
                .map(newStock -> {
                    GetCanBuyNewStockListV3Response.NewQuotaInfo newQuotaInfo = getCanBuyResponse.getNewQuota().stream().filter(v -> v.getMarket().equals(newStock.getMarket())).findAny().orElse(null);
                    SubmitBatTradeV2Request.SubmitData submitData = new SubmitBatTradeV2Request.SubmitData();

                    submitData.setAmount(Integer.min(Integer.parseInt(newStock.getKsgsx()), Integer.parseInt(newQuotaInfo.getKsgsz())));
                    submitData.setMarket(newStock.getMarket());
                    submitData.setPrice(newStock.getFxj());
                    submitData.setStockCode(newStock.getSgdm());
                    submitData.setStockName(newStock.getZqmc());
                    submitData.setTradeType(SubmitRequest.B);
                    return submitData;
                }).collect(Collectors.toList());

        if (systemConfigService.isApplyNewConvertibleBond()) {
            TradeResultVo<GetConvertibleBondListV2Response> getConvertibleBondResultVo = tradeApiService.getConvertibleBondListV2(new GetConvertibleBondListV2Request(userId.intValue()));
            if (getConvertibleBondResultVo.success()) {
                List<SubmitBatTradeV2Request.SubmitData> convertibleBondList = getConvertibleBondResultVo.getData().stream().filter(GetConvertibleBondListV2Response::isExIsToday).map(convertibleBond -> {
                    SubmitBatTradeV2Request.SubmitData submitData = new SubmitBatTradeV2Request.SubmitData();
                    submitData.setAmount(Integer.parseInt(convertibleBond.getLIMITBUYVOL()));
                    submitData.setMarket(convertibleBond.getMarket());
                    submitData.setPrice(convertibleBond.getPARVALUE());
                    submitData.setStockCode(convertibleBond.getSUBCODE());
                    submitData.setStockName(convertibleBond.getBONDNAME());
                    submitData.setTradeType(SubmitRequest.B);
                    return submitData;
                }).collect(Collectors.toList());

                newStockList.addAll(convertibleBondList);
            } else {
                logger.warn("get convertible stock: {}", getConvertibleBondResultVo);
                messageServicve.send("get convertible stock: " + getConvertibleBondResultVo.getMessage());
            }
        }

        TradeResultVo<GetOrdersDataResponse> orderReponse = tradeApiService.getOrdersData(new GetOrdersDataRequest(userId.intValue()));
        if (orderReponse.success()) {
            List<GetOrdersDataResponse> orderList = orderReponse.getData().stream().filter(v -> v.getWtzt().equals(GetOrdersDataResponse.YIBAO)).collect(Collectors.toList());
            newStockList = newStockList.stream().filter(v -> orderList.stream().noneMatch(order -> order.getZqdm().equals(v.getStockCode()))).collect(Collectors.toList());
        }

        if (newStockList.isEmpty()) {
            return;
        }

        SubmitBatTradeV2Request request = new SubmitBatTradeV2Request(userId.intValue());
        request.setList(newStockList);

        TradeResultVo<SubmitBatTradeV2Response> tradeResultVo = tradeApiService.submitBatTradeV2(request);
        logger.info("apply new stock: {}", tradeResultVo);
        messageServicve.send("apply new stock: " + tradeResultVo.getMessage());
    }

    private void applyNewStockCr(int userId) {
        CrGetCanBuyNewStockListV3Response getCanBuyResponse = tradeApiService.crGetCanBuyNewStockListV3(new CrGetCanBuyNewStockListV3Request(userId)).getData().get(0);

        List<SubmitBatTradeV2Request.SubmitData> newStockList = getCanBuyResponse.getNewStockList().stream()
                .filter(newStock -> getCanBuyResponse.getNewQuota().stream().anyMatch(v -> v.getMarket().equals(newStock.getMarket())))
                .map(newStock -> {
                    CrGetCanBuyNewStockListV3Response.NewQuotaInfo newQuotaInfo = getCanBuyResponse.getNewQuota().stream().filter(v -> v.getMarket().equals(newStock.getMarket())).findAny().orElse(null);
                    CrSubmitBatTradeV2Request.CrSubmitData submitData = new CrSubmitBatTradeV2Request.CrSubmitData();

                    submitData.setAmount(Integer.min(Integer.parseInt(newStock.getKsgsx()), Integer.parseInt(newQuotaInfo.getCustQuota())));
                    submitData.setMarket(newStock.getMarket());
                    submitData.setPrice(newStock.getFxj());
                    submitData.setStockCode(newStock.getSgdm());
                    submitData.setStockName(newStock.getZqmc());
                    submitData.setTradeType(SubmitRequest.B);
                    return submitData;
                }).collect(Collectors.toList());

        if (systemConfigService.isApplyNewConvertibleBond()) {
            TradeResultVo<CrGetConvertibleBondListV2Response> getConvertibleBondResultVo = tradeApiService.crGetConvertibleBondListV2(new CrGetConvertibleBondListV2Request(userId));
            if (getConvertibleBondResultVo.success()) {
                List<SubmitBatTradeV2Request.SubmitData> convertibleBondList = getConvertibleBondResultVo.getData().stream().filter(GetConvertibleBondListV2Response::isExIsToday).map(convertibleBond -> {
                    CrSubmitBatTradeV2Request.CrSubmitData submitData = new CrSubmitBatTradeV2Request.CrSubmitData();
                    submitData.setAmount(Integer.parseInt(convertibleBond.getLIMITBUYVOL()));
                    submitData.setMarket(convertibleBond.getMarket());
                    submitData.setPrice(convertibleBond.getPARVALUE());
                    submitData.setStockCode(convertibleBond.getSUBCODE());
                    submitData.setStockName(convertibleBond.getBONDNAME());
                    submitData.setTradeType(SubmitRequest.B);
                    return submitData;
                }).collect(Collectors.toList());

                newStockList.addAll(convertibleBondList);
            } else {
                logger.warn("get convertible stock: {}", getConvertibleBondResultVo);
                messageServicve.send("get convertible stock: " + getConvertibleBondResultVo.getMessage());
            }
        }

        TradeResultVo<CrGetOrdersDataResponse> orderReponse = tradeApiService.crGetOrdersData(new CrGetOrdersDataRequest(userId));
        if (orderReponse.success()) {
            List<GetOrdersDataResponse> orderList = orderReponse.getData().stream().filter(v -> v.getWtzt().equals(GetOrdersDataResponse.YIBAO)).collect(Collectors.toList());
            newStockList = newStockList.stream().filter(v -> orderList.stream().noneMatch(order -> order.getZqdm().equals(v.getStockCode()))).collect(Collectors.toList());
        }

        if (newStockList.isEmpty()) {
            return;
        }

        CrSubmitBatTradeV2Request request = new CrSubmitBatTradeV2Request(userId);
        request.setList(newStockList);

        TradeResultVo<CrSubmitBatTradeV2Response> tradeResultVo = tradeApiService.crSubmitBatTradeV2(request);
        logger.info("apply new stock: {}", tradeResultVo);
        messageServicve.send("cr apply new stock: " + tradeResultVo.getMessage());
    }

    @Override
    public PageVo<TaskVo> getAllTask(PageParam pageParam) {
        return executeInfoDao.get(pageParam);
    }

    @Override
    public void changeTaskState(int state, int id) {
        executeInfoDao.updateState(state, id);
    }

}
