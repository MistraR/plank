ALTER TABLE `plank`.`stock`
    ADD COLUMN `auto_plank` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否加入自动打板缓存' AFTER `ma20`;

ALTER TABLE `plank`.`trade_record`
DROP COLUMN `balance`,
DROP COLUMN `available_balance`,
MODIFY COLUMN `price` decimal(10, 2) NOT NULL COMMENT '卖出价格' AFTER `number`,
ADD COLUMN `profit` decimal(10, 2) NOT NULL COMMENT '本笔交易利润' AFTER `reason`,
ADD COLUMN `cost_price` decimal(10, 2) NOT NULL COMMENT '成本价' AFTER `profit`;

ALTER TABLE `plank`.`stock`
    ADD COLUMN `cancel_plank` tinyint NOT NULL DEFAULT 0 COMMENT '是否取消打板' AFTER `auto_plank`;