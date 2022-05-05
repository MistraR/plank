SELECT count(*)
FROM daily_record
WHERE DATE_FORMAT(date, '%Y-%m-%d') = '2022-04-22';
SELECT count(*)
FROM daily_record
WHERE DATE_FORMAT(date, '%Y-%m-%d') = DATE_FORMAT(now(), '%Y-%m-%d');
DELETE
FROM daily_record
WHERE DATE_FORMAT(date, '%Y-%m-%d') = '2022-04-22';

#
白酒
SELECT *
FROM stock
WHERE `name` IN ('泸州老窖', '五粮液', '舍得酒业', '洋河股份', '山西汾酒', '酒鬼酒')
ORDER BY code;
#
光伏
SELECT *
FROM stock
WHERE `name` IN ('隆基股份', '中环股份', '晶澳科技', '通威股份', '阳光电源', '合盛硅业')
ORDER BY code;
#
半导体
SELECT *
FROM stock
WHERE `name` IN ('卓胜微', '韦尔股份', '兆易创新', '圣邦股份', '紫光国微')
ORDER BY code;
#
医疗服务
SELECT *
FROM stock
WHERE `name` IN ('药明康德', '泰格医药', '迈瑞医疗', '凯莱英', '智飞生物', '康龙化成', '博腾股份')
ORDER BY code;
#
锂电池新能源
SELECT *
FROM stock
WHERE `name` IN ('宁德时代', '天齐锂业', '赣锋锂业', '亿纬锂能', '盐湖股份')
ORDER BY code;
#
龙头
SELECT *
FROM stock
WHERE `name` IN ('比亚迪', '立讯精密', '歌尔股份', '万华化学', '美的集团', '汇川技术', '海康威视', '伊利股份', '东方财富', '爱尔眼科')
ORDER BY code;
#
医美
SELECT *
FROM stock
WHERE `name` IN ('爱美客', '贝泰妮', '珀莱雅')
ORDER BY code;

#
逼近建仓点的股票
SELECT *
FROM stock
WHERE `name` IN ('东方财富', '爱尔眼科', '药明康德', '贝泰妮', '珀莱雅', '爱美客', '泸州老窖', '康龙化成', '伊利股份', '美的集团')
ORDER BY code;

UPDATE stock
SET purchase_price=40
WHERE `name` = '宁德时代';

