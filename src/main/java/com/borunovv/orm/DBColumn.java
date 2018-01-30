package com.borunovv.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Аннотация для привязки полей класса к стобцам в таблице БД.
 * (Аналогично @Column в Hibernate).
 *
 * @author borunovv
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface DBColumn {
    String value() default "";
}
