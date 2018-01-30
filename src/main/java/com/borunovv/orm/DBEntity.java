package com.borunovv.orm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Аннотация для пометки класса как сущности БД (привязка к таблице).
 * (Аналогично @Entity в Hibernate).
 *
 * @author borunovv
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
public @interface DBEntity {
    String table();
}
