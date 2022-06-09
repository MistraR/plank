/*
 Navicat Premium Data Transfer

 Source Server         : LOCAL
 Source Server Type    : MySQL
 Source Server Version : 80028
 Source Host           : localhost:3306
 Source Schema         : plank

 Target Server Type    : MySQL
 Target Server Version : 80028
 File Encoding         : 65001

 Date: 09/06/2022 09:13:04
*/

SET NAMES utf8mb4;
SET
FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for clearance
-- ----------------------------
DROP TABLE IF EXISTS `clearance`;
CREATE TABLE `clearance`
(
    `id`                bigint                                                        NOT NULL AUTO_INCREMENT,
    `date`              datetime                                                      NOT NULL COMMENT '日期',
    `name`              char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci     NOT NULL COMMENT '名称',
    `code`              char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci     NOT NULL COMMENT '证券代码',
    `cost_price`        decimal(10, 2)                                                NOT NULL COMMENT '买入成本价',
    `number`            int                                                           NOT NULL COMMENT '数量',
    `price`             decimal(10, 2)                                                NOT NULL COMMENT '清仓价格',
    `rate`              decimal(10, 2)                                                NOT NULL COMMENT '盈亏比率',
    `balance`           decimal(10, 2)                                                NOT NULL COMMENT '账户余额',
    `available_balance` decimal(10, 2)                                                NOT NULL COMMENT '可用余额',
    `reason`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '清仓原因',
    `profit`            decimal(10, 2)                                                NOT NULL COMMENT '盈亏利润',
    `day_number`        int                                                           NOT NULL COMMENT '持股天数',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11122 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for daily_record
-- ----------------------------
DROP TABLE IF EXISTS `daily_record`;
CREATE TABLE `daily_record`
(
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `name`          char(10)       NOT NULL COMMENT '名称',
    `code`          char(16)       NOT NULL COMMENT '证券代码',
    `open_price`    decimal(10, 2) NOT NULL COMMENT '开盘价',
    `close_price`   decimal(10, 2) NOT NULL COMMENT '收盘价',
    `date`          datetime       NOT NULL COMMENT '日期',
    `increase_rate` decimal(10, 2) NOT NULL COMMENT '涨跌比率',
    `highest`       decimal(10, 2) NOT NULL COMMENT '最高价',
    `lowest`        decimal(10, 2) NOT NULL COMMENT '最低价',
    `amount`        bigint         NOT NULL COMMENT '成交额',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique` (`code`,`date`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6882218 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for dragon_list
-- ----------------------------
DROP TABLE IF EXISTS `dragon_list`;
CREATE TABLE `dragon_list`
(
    `id`           bigint                                                    NOT NULL AUTO_INCREMENT,
    `date`         datetime                                                  NOT NULL COMMENT '上榜日期',
    `name`         char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
    `code`         char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证券代码',
    `net_buy`      bigint                                                    NOT NULL COMMENT '净买入(W)',
    `buy`          bigint                                                    NOT NULL COMMENT '买入(W)',
    `sell`         bigint                                                    NOT NULL COMMENT '卖出(W)',
    `price`        decimal(10, 2)                                            NOT NULL COMMENT '收盘价',
    `market_value` bigint                                                    NOT NULL COMMENT '流通市值',
    `accum_amount` bigint                                                    NOT NULL COMMENT '成交额',
    `change_rate`  decimal(10, 2)                                            NOT NULL COMMENT '涨跌幅',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=41247 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for foreign_fund_holdings_tracking
-- ----------------------------
DROP TABLE IF EXISTS `foreign_fund_holdings_tracking`;
CREATE TABLE `foreign_fund_holdings_tracking`
(
    `id`                                bigint                                                       NOT NULL AUTO_INCREMENT,
    `code`                              varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '股票代码',
    `name`                              varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '股票名称',
    `fund_count`                        int                                                          NOT NULL COMMENT '持有该股票的基金数',
    `quarter`                           int                                                          NOT NULL COMMENT '季度 202201',
    `shareholding_count`                bigint                                                       NOT NULL COMMENT '持股总量/万股',
    `shareholding_change_count`         bigint                                                       NOT NULL COMMENT '持股变动数量/万股',
    `fund_total_market`                 bigint                                                       NOT NULL COMMENT '报告期末基金持股总市值/万',
    `fund_total_market_dynamic`         bigint                                                       NOT NULL COMMENT '基金持股总市值/万 动态',
    `foreign_total_market_dynamic`      bigint                                                       NOT NULL COMMENT '外资持股总市值/万 动态',
    `foreign_fund_total_market_dynamic` bigint                                                       NOT NULL COMMENT '外资+基金持股总市值/万 动态',
    `average_price`                     decimal(7, 2)                                                NOT NULL COMMENT '季度均价',
    `shareholding_change_amount`        bigint                                                       NOT NULL COMMENT '持股变动金额/万',
    `modify_time`                       datetime                                                     NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新日期',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uidx` (`quarter`,`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=945 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='基金持仓追踪';

-- ----------------------------
-- Table structure for hold_shares
-- ----------------------------
DROP TABLE IF EXISTS `hold_shares`;
CREATE TABLE `hold_shares`
(
    `id`             bigint                                                    NOT NULL AUTO_INCREMENT,
    `name`           char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
    `code`           char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证券代码',
    `number`         int                                                       NOT NULL COMMENT '持股数量',
    `cost`           decimal(10, 2)                                            NOT NULL COMMENT '当前成本价',
    `current_price`  decimal(10, 2)                                            NOT NULL COMMENT '当前价',
    `rate`           decimal(10, 2)                                            NOT NULL COMMENT '盈亏比率',
    `buy_price`      decimal(10, 2)                                            NOT NULL COMMENT '建仓价',
    `buy_time`       datetime                                                  NOT NULL COMMENT '建仓日期',
    `fifteen_profit` tinyint                                                   NOT NULL COMMENT '收益是否到过15%',
    `buy_number`     int                                                       NOT NULL COMMENT '建仓数量',
    `profit`         decimal(10, 2)                                            NOT NULL COMMENT '利润',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11185 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for stock
-- ----------------------------
DROP TABLE IF EXISTS `stock`;
CREATE TABLE `stock`
(
    `code`               char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci    NOT NULL COMMENT '证券代码',
    `name`               varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证券名称',
    `market_value`       bigint                                                       NOT NULL COMMENT '市值',
    `transaction_amount` decimal(14, 2)                                               NOT NULL COMMENT '当日成交额',
    `current_price`      decimal(14, 2)                                               NOT NULL COMMENT '当前价格',
    `purchase_price`     decimal(10, 2)                                               NOT NULL DEFAULT '0.00' COMMENT '预计建仓价格',
    `volume`             bigint                                                       NOT NULL COMMENT '当日成交量',
    `modify_time`        datetime                                                     NOT NULL COMMENT '最近更新日期',
    `track`              tinyint(1) unsigned zerofill NOT NULL COMMENT '是否开启建仓点监控',
    `focus`              tinyint(1) unsigned zerofill NOT NULL COMMENT '重点关注',
    `shareholding`       tinyint(1) unsigned zerofill NOT NULL COMMENT '是否持仓',
    `classification`     varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '所属板块',
    `ma5`                decimal(10, 2)                                                        DEFAULT NULL COMMENT '5日均线',
    `ma10`               decimal(10, 2)                                                        DEFAULT NULL COMMENT '10日均线',
    `ma20`               decimal(10, 2)                                                        DEFAULT NULL COMMENT '20日均线',
    PRIMARY KEY (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for trade_record
-- ----------------------------
DROP TABLE IF EXISTS `trade_record`;
CREATE TABLE `trade_record`
(
    `id`                bigint                                                        NOT NULL AUTO_INCREMENT,
    `date`              datetime                                                      NOT NULL COMMENT '日期',
    `type`              tinyint                                                       NOT NULL COMMENT '交易类型0-买入1-卖出',
    `name`              char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci     NOT NULL COMMENT '名称',
    `code`              char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci     NOT NULL COMMENT '证券代码',
    `money`             int                                                           NOT NULL COMMENT '金额',
    `number`            int                                                           NOT NULL COMMENT '数量',
    `price`             decimal(10, 2)                                                NOT NULL COMMENT '价格',
    `balance`           decimal(10, 2)                                                NOT NULL COMMENT '账户余额',
    `reason`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易原因',
    `available_balance` decimal(10, 2)                                                NOT NULL COMMENT '可用余额',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=23566 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for trade_record_real
-- ----------------------------
DROP TABLE IF EXISTS `trade_record_real`;
CREATE TABLE `trade_record_real`
(
    `id`          bigint                                                    NOT NULL AUTO_INCREMENT COMMENT '清仓原因',
    `buy_date`    datetime                                                  NOT NULL COMMENT '建仓日期',
    `sell_date`   datetime       DEFAULT NULL COMMENT '清仓日期',
    `day`         int                                                       NOT NULL COMMENT '持股天数',
    `name`        char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
    `number`      int                                                       NOT NULL COMMENT '买入数量',
    `buy_price`   decimal(10, 2)                                            NOT NULL COMMENT '买入价格',
    `sell_price`  decimal(10, 2) DEFAULT NULL COMMENT '卖出价格',
    `balance`     decimal(10, 2) DEFAULT NULL COMMENT '盈亏金额',
    `buy_reason`  tinyint                                                   NOT NULL COMMENT '买入原因 1:爆量突破之后缩量回踩 2:旗型整理，没有缩量 3:游资蛰伏，回踩拉升 4:龙头股首阴5日线低吸 5:热门票均线回踩第二波 6:打板 7:超跌低吸 8:上升趋势买入',
    `sell_reason` tinyint        DEFAULT NULL COMMENT '清仓原因 1:跌破均线清仓 2:止盈清仓 3:利润回撤止盈',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13566 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET
FOREIGN_KEY_CHECKS = 1;
