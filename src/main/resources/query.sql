SELECT count( * ) FROM	daily_record WHERE	DATE_FORMAT( date, '%Y-%m-%d' ) = '2022-04-22';
SELECT count( * ) FROM	daily_record WHERE	DATE_FORMAT( date, '%Y-%m-%d' ) = DATE_FORMAT( now( ), '%Y-%m-%d' );
DELETE FROM	daily_record WHERE	DATE_FORMAT( date, '%Y-%m-%d' ) = '2022-04-22';


# 重点跟踪股票
SELECT * FROM stock WHERE `name` IN ('卓胜微','韦尔股份','兆易创新','圣邦股份','隆基股份','中环股份','晶澳科技','通威股份','合盛硅业','智飞生物',
                                     '药明康德','泰格医药','迈瑞医疗','爱美客','贝泰妮','珀莱雅','泸州老窖','五粮液','山西汾酒','洋河股份','立讯精密',
                                     '歌尔股份','万华化学','美的集团','汇川技术','宁德时代','盐湖股份','天齐锂业','亿纬锂能','赣锋锂业') ORDER BY code;

# 逼近建仓点的股票
SELECT * FROM stock WHERE `name` IN ('通威股份','智飞生物','泰格医药','贝泰妮','珀莱雅','爱美客','泸州老窖','五粮液') ORDER BY code;

UPDATE stock SET purchase_price=40 WHERE `name`='通威股份';

