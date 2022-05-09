SELECT count(id)
FROM daily_record
WHERE DATE_FORMAT(date, '%Y-%m-%d') = '2022-05-05';
SELECT count(id)
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
重点关注股票
SELECT *
FROM stock
WHERE `name` IN ('恩捷股份', '紫金矿业', '药明康德', '宁德时代', '智飞生物', '爱美客', '中环股份', '紫光国微', '兆易创新', '通威股份', '迈瑞医疗')
ORDER BY code;
#
查看基金对某支股票的 季度增减持仓明细
SELECT `quarter`,
       shareholding_count,
       `name`,
       fund_total_market          AS 基金持仓市值,
       average_price              as 增持均价,
       shareholding_change_amount as 增持金额万,
       shareholding_change_count  as 持股数量变动,
       IF(shareholding_change_count < 10, 0, 1)
FROM foreign_fund_holdings_tracking
WHERE name = '药明康德'
ORDER BY `quarter` ASC;

#
查看重点关注的股票历史汇总增持金额
SELECT name, SUM(shareholding_change_amount)
FROM foreign_fund_holdings_tracking
GROUP BY `name`
ORDER BY SUM(shareholding_change_amount) desc;
SELECT name, SUM(shareholding_change_amount)
FROM foreign_fund_holdings_tracking
WHERE `quarter` > 202103
GROUP BY `name`
ORDER BY SUM(shareholding_change_amount) desc;
SELECT name, SUM(shareholding_change_amount)
FROM foreign_fund_holdings_tracking
WHERE `name` IN ('恩捷股份', '紫金矿业', '药明康德', '宁德时代', '智飞生物', '爱美客', '中环股份', '紫光国微', '兆易创新', '通威股份', '迈瑞医疗')
GROUP BY `name`
ORDER BY SUM(shareholding_change_amount) desc # 加仓季度排序
SELECT a.*
from (SELECT name, SUM(IF(shareholding_change_count < 10, 0, 1)) as `quarterAdd`
      FROM foreign_fund_holdings_tracking
      GROUP BY `name`) as a
WHERE a.`quarterAdd` > 2
ORDER BY a.quarterAdd desc # 查看某一板块某一季度的基金持仓汇总
SELECT s.name                                 证券,
       f.fund_total_market_dynamic         AS 基金持仓市值,
       f.foreign_total_market_dynamic      AS 外资持仓市值,
       f.foreign_fund_total_market_dynamic AS 外资和基金,
       f.shareholding_change_amount        AS 本季度增减市值
FROM stock s
         left join foreign_fund_holdings_tracking f
                   on (s.name COLLATE utf8mb4_general_ci) = (f.name COLLATE utf8mb4_general_ci)
WHERE s.classification = '龙头'
  and f.quarter = 202201
ORDER BY f.foreign_fund_total_market_dynamic DESC # 查看某一季度的基金持仓汇总
SELECT s.name                                 证券,
       f.fund_total_market_dynamic         AS 基金持仓市值,
       f.foreign_total_market_dynamic      AS 外资持仓市值,
       f.foreign_fund_total_market_dynamic AS 外资和基金,
       f.shareholding_change_amount        AS 本季度增减市值
FROM stock s
         left join foreign_fund_holdings_tracking f
                   on (s.name COLLATE utf8mb4_general_ci) = (f.name COLLATE utf8mb4_general_ci)
WHERE f.quarter = 202104
ORDER BY f.shareholding_change_amount DESC


