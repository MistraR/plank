package com.mistra.plank.common.util;

import org.springframework.util.StringUtils;

import java.util.*;

public class SqlCondition {

    private StringBuilder sqlString;
    private Map<String, Object> params;
    private List<Object> objList = new ArrayList<>();

    public SqlCondition(String sqlString, Map<String, Object> params) {
        this.sqlString = new StringBuilder(sqlString);
        this.params = params;
    }

    public SqlCondition(String sqlString, Map<String, Object> params, List<Object> objList) {
        this.sqlString = new StringBuilder(sqlString);
        this.params = params;
        this.objList = objList;
    }

    /**
     * 新增字符串查询条件(等价于 == )
     */
    public SqlCondition addString(String key, String column) {
        String value = Objects.toString(params.get(key), null);
        if (StringUtils.hasLength(value)) {
            sqlString.append(String.format(" and %s = ? ", column));
            objList.add(value);
        }
        return this;
    }

    /**
     * 新增字符串查询条件(等价于 != )
     */
    public SqlCondition addStringNotEquals(String key, String column) {
        String value = Objects.toString(params.get(key), null);
        if (StringUtils.hasLength(value)) {
            sqlString.append(String.format(" and %s <> ? ", column));
            objList.add(value);
        }
        return this;
    }

    /**
     * 新增SQL
     */
    public SqlCondition addSql(String sql, Object... args) {
        if (StringUtils.hasLength(sql)) {
            sqlString.append(sql);
            objList.addAll(Arrays.asList(args));
        }
        return this;
    }

    /**
     * 新增字符串查询条件(等价于 >= )
     */
    public SqlCondition addStringGE(String key, String column) {
        String value = Objects.toString(params.get(key), null);
        if (StringUtils.hasLength(value)) {
            sqlString.append(String.format(" and %s >= ? ", column));
            objList.add(value);
        }
        return this;
    }

    /**
     * 新增字符串查询条件(等价于 <= )
     */
    public SqlCondition addStringLE(String key, String column) {
        String value = Objects.toString(params.get(key), null);
        if (StringUtils.hasLength(value)) {
            sqlString.append(String.format(" and %s <= ? ", column));
            objList.add(value);
        }
        return this;
    }

    /**
     * 新增字符串查询条件(等价于 < )
     */
    public SqlCondition addStringLT(String key, String column) {
        String value = Objects.toString(params.get(key), null);
        if (StringUtils.hasLength(value)) {
            sqlString.append(String.format(" and %s < ? ", column));
            objList.add(value);
        }
        return this;
    }

    /**
     * 新增字符串查询条件(等价于 > )
     */
    public SqlCondition addStringGT(String key, String column) {
        String value = Objects.toString(params.get(key), null);
        if (StringUtils.hasLength(value)) {
            sqlString.append(String.format(" and %s > ? ", column));
            objList.add(value);
        }
        return this;
    }

    /**
     * 模糊查询
     */
    public SqlCondition addStringLike(String key, String column) {
        String value = Objects.toString(params.get(key), null);
        if (StringUtils.hasLength(value)) {
            sqlString.append(String.format(" and %s like ?", column));
            objList.add("%" + value + "%");
        }
        return this;
    }

    /**
     * 添加排序
     */
    public void addSort(String column, boolean isAsc, boolean first) {
       sqlString.append(String.format(first ? " order by %s %s " : ", %s %s ", column, isAsc ? "asc" : "desc"));
    }

    /**
     * 添加分页参数
     *
     * @param start
     * @param length
     */
    public void addPage(int start, int length) {
        objList.add(start);
        objList.add(length);
    }

    public Object[] toArgs() {
        return objList.toArray();
    }

    public String toSql() {
        return sqlString.toString();
    }

    public String getCountSql() {
        return String.format("select count(1) from (%s) _row", sqlString.toString());
    }

    public List<Object> getObjectList() {
        return objList;
    }

}
