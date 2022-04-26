SELECT 	count( * ) FROM	daily_record WHERE	DATE_FORMAT( date, '%Y-%m-%d' ) = '2022-04-22';
DELETE FROM	daily_record WHERE	DATE_FORMAT( date, '%Y-%m-%d' ) = '2022-04-22';