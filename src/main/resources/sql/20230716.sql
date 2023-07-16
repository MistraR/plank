ALTER TABLE `plank`.`stock`
    ADD COLUMN `auto_plank` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否加入自动打板缓存' AFTER `ma20`;