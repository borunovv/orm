package com.borunovv.orm;

import com.borunovv.db.DBAccess;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Минимальная реализация ORM (Object Relational Mapping) - слой,
 * инкапсулирующий работу с БД (через JDBC) на уровне объектов предметной области (Entity).
 *
 * Аналогично Hibernate Template.
 *
 * @author borunovv
 */
public class SimpleORM {

    // Веренет все записи таблицы в виде списка объектов.
    // entityClass - должен быть аннотирован через @DBEntity.
    public static <T> List<T> findAll(DBAccess dbAccess, final Class<T> entityClass) throws SQLException, InterruptedException {
        return dbAccess.executeSelect("SELECT * FROM `" + getTableName(entityClass) + "`",
                new DBAccess.IResultSetProcessor<List<T>>() {

            public List<T> process(ResultSet resultSet) throws SQLException {
                try {
                    return toEntityList(resultSet, entityClass);
                } catch (Exception e) {
                    throw new RuntimeException("Error ORM mapping", e);
                }
            }
        });
    }

    private static <T> List<T> toEntityList(ResultSet resultSet, Class<T> clazz) throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<T> result = new LinkedList<T>();

        ResultSetMetaData metaData = resultSet.getMetaData();
        String[] columnNames = new String[metaData.getColumnCount()];
        Map<String, String> entry = new HashMap<String, String>();

        for (int i = 0; i < columnNames.length ; ++i) {
            columnNames[i] = metaData.getColumnName(i + 1);
            entry.put(columnNames[i], null);
        }

        while (resultSet.next()) {
            for (String columnName : columnNames) {
                entry.put(columnName, resultSet.getString(columnName));
            }
            result.add(BeanUtils.newInstance(entry, clazz));
        }

        return result;
    }

    private static <T> String getTableName(Class<T> entityClass) {
        DBEntity ann = BeanUtils.getAnnotation(entityClass, DBEntity.class);
        if (ann == null) {
            throw new IllegalArgumentException("Expected annotation 'DBEntry' for calass '" + entityClass.getSimpleName() + "'");
        }
        return ann.table();
    }
}
