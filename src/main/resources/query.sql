SELECT count(*)
FROM daily_record
WHERE DATE_FORMAT(date, '%Y-%m-%d') = '2022-05-05';
SELECT count(*)
FROM daily_record
WHERE DATE_FORMAT(date, '%Y-%m-%d') = DATE_FORMAT(now(), '%Y-%m-%d');

#
白酒
SELECT *
FROM stock
WHERE classification = '白酒'
ORDER BY RIGHT (code, 6) DESC;
#
光伏
SELECT *
FROM stock
WHERE classification = '光伏'
ORDER BY RIGHT (code, 6) DESC;
#
半导体
SELECT *
FROM stock
WHERE classification = '半导体'
ORDER BY RIGHT (code, 6) DESC;
#
医疗服务
SELECT *
FROM stock
WHERE classification = '医疗服务'
ORDER BY RIGHT (code, 6) DESC;
#
锂电池
SELECT *
FROM stock
WHERE classification = '锂电池'
ORDER BY RIGHT (code, 6) DESC;
#
龙头
SELECT *
FROM stock
WHERE classification = '龙头'
ORDER BY RIGHT (code, 6) DESC;
#
医美
SELECT *
FROM stock
WHERE classification = '医美'
ORDER BY RIGHT (code, 6) DESC;
#
逼近建仓点的股票
SELECT *
FROM stock
WHERE `name` IN ('东方财富', '爱尔眼科', '药明康德', '贝泰妮', '珀莱雅', '爱美客', '泸州老窖', '康龙化成', '伊利股份', '美的集团')
ORDER BY code;





