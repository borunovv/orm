package com.borunovv.orm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Хелпер.
 * Использует рефлексию для создания и инициализации экземпляра класса.
 * @author borunovv
 */
public class BeanUtils {

    // Создает экземпляр заданного класса (POJO) и инициализирует его свойства (через вызов set-методов).
    public static <T> T newInstance(Map<String, String> params, Class<T> clazz) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Map<Field, String> fieldMapping = getFieldMapping(clazz);
        T obj = clazz.newInstance();
        for (Field field : fieldMapping.keySet()) {
            setFieldValue(clazz, obj, field, findIgnoreCase(params, fieldMapping.get(field)));
        }
        return obj;
    }

    // Поиск по ключу в мапе без учета регистра.
    private static String findIgnoreCase(Map<String, String> params, String key) {
        if (params.containsKey(key)) {
            return params.get(key);
        }
        for (String curKey : params.keySet()) {
            if (curKey.equalsIgnoreCase(key)) {
                return params.get(curKey);
            }
        }
        throw new IllegalArgumentException("Expected value for column '" + key + "'");
    }

    // Ищет и вызывает соответствующий set-метод для заданного поля (field).
    private static <T> void setFieldValue(Class<T> clazz, T obj, Field field, String value) throws InvocationTargetException, IllegalAccessException {
        String setterName = getSetterName(field);
        Method setter = findMethod(clazz, setterName);
        setter.invoke(obj, convertToType(field.getType(), value));
    }

    // Недоделано (для учебных целей пойдет):
    // конвертит из String в заданный тип. Пока только из строки в long/Long/String.
    private static <T> Object convertToType(Class<T> type, String value) {
        if (String.class == type) {
            return value;
        } else if (Long.TYPE == type || Long.class == type) {
            return Long.parseLong(value);
        } else {
            throw new IllegalArgumentException("TODO: not implemented conversion from String to '" + type.getSimpleName() + "'");
        }
    }

    // Веренет set-метод для заданного поля.
    private static String getSetterName(Field field) {
        String fieldName = field.getName();
        return "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    // Ищет медод класса по имени.
    private static <T> Method findMethod(Class<T> clazz, String name) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException("No method '" + name + "' found in class '" + clazz.getSimpleName() + "'");
    }

    // Ищет все поля класса с аннотацией @DBColumn и возвращает мапу [поле,имя столбца в БД]
    private static Map<Field, String> getFieldMapping(Class clazz) {
        Map<Field, String> res = new HashMap<Field, String>();

        for (Field field : clazz.getDeclaredFields()) {
            Annotation column = field.getAnnotation(DBColumn.class);
            if (column != null) {
                String columnName = ((DBColumn) column).value();
                res.put(field, columnName);
            }
        }

        return res;
    }

    // Вернет аннотацию к классу по типу аннотации. Если нету - null.
    public static <T extends Annotation> T getAnnotation(Class<?> entityClass, Class<T> annotationClass) {
        return entityClass.getAnnotation(annotationClass);
    }
}
