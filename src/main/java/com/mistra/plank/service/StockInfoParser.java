package com.mistra.plank.service;

import java.util.List;

import com.mistra.plank.model.entity.DailyIndex;
import com.mistra.plank.model.entity.StockInfo;

public interface StockInfoParser {

    List<EmStock> parseStockInfoList(String content);

    public static class EmStock {

        private StockInfo stockInfo;
        private DailyIndex dailyIndex;

        public StockInfo getStockInfo() {
            return stockInfo;
        }

        public void setStockInfo(StockInfo stockInfo) {
            this.stockInfo = stockInfo;
        }

        public DailyIndex getDailyIndex() {
            return dailyIndex;
        }

        public void setDailyIndex(DailyIndex dailyIndex) {
            this.dailyIndex = dailyIndex;
        }

    }

}
