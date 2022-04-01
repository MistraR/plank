/*
 Navicat MySQL Data Transfer

 Source Server         : 139.155.4.123
 Source Server Type    : MySQL
 Source Server Version : 80027
 Source Host           : 139.155.4.123:3306
 Source Schema         : plank

 Target Server Type    : MySQL
 Target Server Version : 80027
 File Encoding         : 65001

 Date: 31/03/2022 16:48:49
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for clearance
-- ----------------------------
DROP TABLE IF EXISTS `clearance`;
CREATE TABLE `clearance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL COMMENT '日期',
  `name` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `code` char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证券代码',
  `cost_price` decimal(10,2) NOT NULL COMMENT '买入成本价',
  `number` int NOT NULL COMMENT '数量',
  `price` decimal(10,2) NOT NULL COMMENT '清仓价格',
  `rate` decimal(10,2) NOT NULL COMMENT '盈亏比率',
  `balance` decimal(10,2) NOT NULL COMMENT '账户余额',
  `available_balance` decimal(10,2) NOT NULL COMMENT '可用余额',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '清仓原因',
  `profit` decimal(10,2) NOT NULL COMMENT '盈亏利润',
  `day_number` int NOT NULL COMMENT '持股天数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for daily_record
-- ----------------------------
DROP TABLE IF EXISTS `daily_record`;
CREATE TABLE `daily_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` char(10) NOT NULL COMMENT '名称',
  `code` char(16) NOT NULL COMMENT '证券代码',
  `open_price` decimal(10,2) NOT NULL COMMENT '开盘价',
  `close_price` decimal(10,2) NOT NULL COMMENT '收盘价',
  `date` datetime NOT NULL COMMENT '日期',
  `increase_rate` decimal(10,2) NOT NULL COMMENT '涨跌比率',
  `highest` decimal(10,2) NOT NULL COMMENT '最高价',
  `lowest` decimal(10,2) NOT NULL COMMENT '最低价',
  `amount` bigint NOT NULL COMMENT '成交额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique` (`code`,`date`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3834877 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for dragon_list
-- ----------------------------
DROP TABLE IF EXISTS `dragon_list`;
CREATE TABLE `dragon_list` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL COMMENT '上榜日期',
  `name` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `code` char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证券代码',
  `net_buy` bigint NOT NULL COMMENT '净买入(W)',
  `buy` bigint NOT NULL COMMENT '买入(W)',
  `sell` bigint NOT NULL COMMENT '卖出(W)',
  `price` decimal(10,2) NOT NULL COMMENT '收盘价',
  `market_value` bigint NOT NULL COMMENT '流通市值',
  `accum_amount` bigint NOT NULL COMMENT '成交额',
  `change_rate` decimal(10,2) NOT NULL COMMENT '涨跌幅',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=20089 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for hold_shares
-- ----------------------------
DROP TABLE IF EXISTS `hold_shares`;
CREATE TABLE `hold_shares` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `code` char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证券代码',
  `number` int NOT NULL COMMENT '持股数量',
  `cost` decimal(10,2) NOT NULL COMMENT '当前成本价',
  `current_price` decimal(10,2) NOT NULL COMMENT '当前价',
  `rate` decimal(10,2) NOT NULL COMMENT '盈亏比率',
  `buy_price` decimal(10,2) NOT NULL COMMENT '建仓价',
  `buy_time` datetime NOT NULL COMMENT '建仓日期',
  `fifteen_profit` tinyint NOT NULL COMMENT '收益是否到过15%',
  `buy_number` int NOT NULL COMMENT '建仓数量',
  `profit` decimal(10,2) NOT NULL COMMENT '利润',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6261 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for stock
-- ----------------------------
DROP TABLE IF EXISTS `stock`;
CREATE TABLE `stock` (
  `code` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证券代码',
  `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证券名称',
  `market_value` bigint NOT NULL COMMENT '市值',
  `transaction_amount` decimal(14,2) NOT NULL COMMENT '当日成交额',
  `current_price` decimal(14,2) NOT NULL COMMENT '当前价格',
  `volume` bigint NOT NULL COMMENT '当日成交量',
  `modify_time` datetime NOT NULL COMMENT '最近更新日期',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for trade_record
-- ----------------------------
DROP TABLE IF EXISTS `trade_record`;
CREATE TABLE `trade_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL COMMENT '日期',
  `type` tinyint NOT NULL COMMENT '交易类型0-买入1-卖出',
  `name` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `code` char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证券代码',
  `money` int NOT NULL COMMENT '金额',
  `number` int NOT NULL COMMENT '数量',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `balance` decimal(10,2) NOT NULL COMMENT '账户余额',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易原因',
  `available_balance` decimal(10,2) NOT NULL COMMENT '可用余额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13564 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for trade_record_real
-- ----------------------------
DROP TABLE IF EXISTS `trade_record_real`;
CREATE TABLE `trade_record_real` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '清仓原因',
  `buy_date` datetime NOT NULL COMMENT '建仓日期',
  `sell_date` datetime DEFAULT NULL COMMENT '清仓日期',
  `day` int NOT NULL COMMENT '持股天数',
  `name` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `number` int NOT NULL COMMENT '买入数量',
  `buy_price` decimal(10,2) NOT NULL COMMENT '买入价格',
  `sell_price` decimal(10,2) DEFAULT NULL COMMENT '卖出价格',
  `balance` decimal(10,2) DEFAULT NULL COMMENT '盈亏金额',
  `buy_reason` tinyint NOT NULL COMMENT '买入原因 1:爆量突破之后缩量回踩 2:旗型整理，没有缩量 3:游资蛰伏，回踩拉升 4:龙头股首阴5日线低吸 5:热门票均线回踩第二波 6:打板 7:超跌低吸 8:上升趋势买入',
  `sell_reason` tinyint DEFAULT NULL COMMENT '清仓原因 1:跌破均线清仓 2:止盈清仓 3:利润回撤止盈',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13566 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
