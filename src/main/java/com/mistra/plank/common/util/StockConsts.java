package com.mistra.plank.common.util;

import java.util.NoSuchElementException;

public class StockConsts {

    public static final String KEY_AUTH_USER_ID = "user_id";

    public static final String KEY_AUTH_TOKEN = "auth-token";

    public static final String CACHE_KEY_PREFIX = "stock:";

    private static final String CACHE_KEY_DATA_PREFIX = CACHE_KEY_PREFIX + "data:";
    public static final String CACHE_KEY_DATA_STOCK = StockConsts.CACHE_KEY_DATA_PREFIX + "stock";
    public static final String CACHE_KEY_DATA_BUSINESS_DATE = StockConsts.CACHE_KEY_DATA_PREFIX + "businessDate";

    private static final String CACHE_KEY_CONFIG_PREFIX = CACHE_KEY_PREFIX + "config:";
    public static final String CACHE_KEY_CONFIG_ROBOT = StockConsts.CACHE_KEY_CONFIG_PREFIX + "robot";

    private static final String CACHE_KEY_TRADE_PREFIX = CACHE_KEY_PREFIX + "trade:";
    public static final String CACHE_KEY_TRADE_USER = StockConsts.CACHE_KEY_TRADE_PREFIX + "tradeUser";
    public static final String CACHE_KEY_TRADE_USER_LIST = StockConsts.CACHE_KEY_TRADE_PREFIX + "tradeUserList";
    public static final String CACHE_KEY_TRADE_METHOD = StockConsts.CACHE_KEY_TRADE_PREFIX + "tradeMethod";

    public static final String CACHE_KEY_TOKEN = CACHE_KEY_PREFIX + "auth:token";

    public static final long DURATION_REDIS_DEFAULT = 3600 * 24 * 2;

    public enum Exchange {
        SH("sh", "HA"), SZ("sz", "SA"), BJ("bj", "BA");
        private final String name;
        private final String market;

        Exchange(String name, String market) {
            this.name = name;
            this.market = market;
        }

        public String getName() {
            return name;
        }

        public String getMarket() {
            return market;
        }

        public boolean isSh() {
            return name.equals(Exchange.SH.name);
        }

        public boolean isSz() {
            return name.equals(Exchange.SZ.name);
        }

        public boolean isBj() {
            return name.equals(Exchange.BJ.name);
        }

        public static Exchange valueOfName(String name) {
            for (Exchange exchange : Exchange.values()) {
                if (exchange.name.equals(name)) {
                    return exchange;
                }
            }
            throw new NoSuchElementException("no exchange named " + name);
        }

    }

    public enum StockState {
        /**
         * 上市
         */
        Listed(0),
        /**
         * 停牌
         */
        Suspended(1),
        /**
         * 退市
         */
        Terminated(2);
        private final int value;

        StockState(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public enum StockType {
        // CB 可转债
        A(0), Index(1), ETF(2), B(3), CB(4);
        private final int value;

        StockType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public enum StockLogType {
        New(0), Rename(1), Terminated(2);
        private final int value;

        StockLogType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public enum TaskState {
        Completed(0), InProgress(1), Pending(2);
        private final int value;

        TaskState(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public enum RobotType {
        DingDing(0), WetChat(1);
        private final int value;

        RobotType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public enum TradeState {
        Invalid(0), Valid(1);
        private final int value;

        TradeState(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public enum MessageType {
        Message(0), Email(1);
        private final int value;

        MessageType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

}
