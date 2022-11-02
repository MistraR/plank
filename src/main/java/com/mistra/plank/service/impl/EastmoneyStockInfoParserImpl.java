package com.mistra.plank.service.impl;

import com.alibaba.fastjson.JSON;
import com.mistra.plank.model.entity.DailyIndex;
import com.mistra.plank.model.entity.StockInfo;
import com.mistra.plank.service.StockInfoParser;
import com.mistra.plank.common.util.StockConsts;
import com.mistra.plank.common.util.StockUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component("eastmoneyStockInfoParser")
public class EastmoneyStockInfoParserImpl implements StockInfoParser {

    // http://quote.eastmoney.com/center/gridlist.html#kcb_board

    @Override
    public List<EmStock> parseStockInfoList(String content) {
        char[] chArr = content.toCharArray();
        char[] newCharArr = new char[chArr.length];
        int i = 0;
        for (char ch : chArr) {
            if (ch == ' ') {
                continue;
            }
            if (ch == 'Ａ') {
                ch = 'A';
            } else if (ch == 'Ｂ') {
                ch = 'B';
            }
            newCharArr[i++] = ch;
        }

        StockResultVo stockResultVo = JSON.parseObject(new String(newCharArr, 0, i), StockResultVo.class);

        // {"f12":"000718","f13":0,"f14":"苏宁环球"}
        return stockResultVo.getData().getDiff().stream().map(v -> {
            String code = v.getF12();
            EmStock emStock = new EmStock();

            StockInfo stockInfo = new StockInfo();
            String exchange = v.getF13() == 0 ? StockUtil.getExchange(code) : StockConsts.Exchange.SH.getName();
            int type = StockUtil.getStockType(exchange, code);
            stockInfo.setExchange(exchange);
            stockInfo.setName(v.getF14());
            stockInfo.setCode(code);
            stockInfo.setExchange(exchange);
            stockInfo.setType(type);

            DailyIndex dailyIndex = new DailyIndex();
            dailyIndex.setDate(new Date());
            dailyIndex.setCode(stockInfo.getFullCode());
            dailyIndex.setClosingPrice(BigDecimal.valueOf(v.getF2()).movePointLeft(2));
            dailyIndex.setTradingVolume(v.getF5() * 100);
            dailyIndex.setTradingValue(BigDecimal.valueOf(v.getF6()));
            dailyIndex.setRurnoverRate(BigDecimal.valueOf(v.getF8()).movePointLeft(2));
            dailyIndex.setHighestPrice(BigDecimal.valueOf(v.getF15()).movePointLeft(2));
            dailyIndex.setLowestPrice(BigDecimal.valueOf(v.getF16()).movePointLeft(2));
            dailyIndex.setOpeningPrice(BigDecimal.valueOf(v.getF17()).movePointLeft(2));
            dailyIndex.setPreClosingPrice(BigDecimal.valueOf(v.getF18()).movePointLeft(2));

            emStock.setStockInfo(stockInfo);
            emStock.setDailyIndex(dailyIndex);

            return emStock;
        }).collect(Collectors.toList());
    }

    public static class StockResultVo {

        private StockResultDataVo data;

        public StockResultDataVo getData() {
            return data;
        }

        public void setData(StockResultDataVo data) {
            this.data = data;
        }
    }

    public static class StockResultDataVo {

        private List<StockResultDiffVo> diff;

        public List<StockResultDiffVo> getDiff() {
            return diff;
        }

        public void setDiff(List<StockResultDiffVo> diff) {
            this.diff = diff;
        }

    }

    public static class StockResultDiffVo {

        private int f1;
        private int f2;
        private int f3;
        private int f4;
        private long f5;
        private double f6;
        private int f7;
        private int f8;
        private int f9;
        private int f10;
        private int f11;
        private String f12;
        private int f13;
        private String f14;
        private int f15;
        private int f16;
        private int f17;
        private int f18;
        private int f20;
        private int f21;
        private int f22;
        private int f23;
        private int f24;
        private int f25;
        private double f62;

        public int getF1() {
            return f1;
        }

        public void setF1(int f1) {
            this.f1 = f1;
        }

        public int getF2() {
            return f2;
        }

        public void setF2(int f2) {
            this.f2 = f2;
        }

        public int getF3() {
            return f3;
        }

        public void setF3(int f3) {
            this.f3 = f3;
        }

        public int getF4() {
            return f4;
        }

        public void setF4(int f4) {
            this.f4 = f4;
        }

        public long getF5() {
            return f5;
        }

        public void setF5(long f5) {
            this.f5 = f5;
        }

        public double getF6() {
            return f6;
        }

        public void setF6(double f6) {
            this.f6 = f6;
        }

        public int getF7() {
            return f7;
        }

        public void setF7(int f7) {
            this.f7 = f7;
        }

        public int getF8() {
            return f8;
        }

        public void setF8(int f8) {
            this.f8 = f8;
        }

        public int getF9() {
            return f9;
        }

        public void setF9(int f9) {
            this.f9 = f9;
        }

        public int getF10() {
            return f10;
        }

        public void setF10(int f10) {
            this.f10 = f10;
        }

        public int getF11() {
            return f11;
        }

        public void setF11(int f11) {
            this.f11 = f11;
        }

        public String getF12() {
            return f12;
        }

        public void setF12(String f12) {
            this.f12 = f12;
        }

        public int getF13() {
            return f13;
        }

        public void setF13(int f13) {
            this.f13 = f13;
        }

        public String getF14() {
            return f14;
        }

        public void setF14(String f14) {
            this.f14 = f14;
        }

        public int getF15() {
            return f15;
        }

        public void setF15(int f15) {
            this.f15 = f15;
        }

        public int getF16() {
            return f16;
        }

        public void setF16(int f16) {
            this.f16 = f16;
        }

        public int getF17() {
            return f17;
        }

        public void setF17(int f17) {
            this.f17 = f17;
        }

        public int getF18() {
            return f18;
        }

        public void setF18(int f18) {
            this.f18 = f18;
        }

        public int getF20() {
            return f20;
        }

        public void setF20(int f20) {
            this.f20 = f20;
        }

        public int getF21() {
            return f21;
        }

        public void setF21(int f21) {
            this.f21 = f21;
        }

        public int getF22() {
            return f22;
        }

        public void setF22(int f22) {
            this.f22 = f22;
        }

        public int getF23() {
            return f23;
        }

        public void setF23(int f23) {
            this.f23 = f23;
        }

        public int getF24() {
            return f24;
        }

        public void setF24(int f24) {
            this.f24 = f24;
        }

        public int getF25() {
            return f25;
        }

        public void setF25(int f25) {
            this.f25 = f25;
        }

        public double getF62() {
            return f62;
        }

        public void setF62(double f62) {
            this.f62 = f62;
        }

    }

}
