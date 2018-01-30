package com.borunovv.db;

import java.sql.*;

/**
 * Обертка над JDBC-драйвером MySQL.
 * Внимание: после использования надо вызвать close() !
 *
 * @author borunovv
 */
public class MySQLAccess extends DBAccess {

    public MySQLAccess(String dbUrl, String user, String password) throws SQLException, ClassNotFoundException {
        this(dbUrl, user, password, 1);
    }

    public MySQLAccess(String dbUrl, String user, String password, int poolSize) throws SQLException, ClassNotFoundException {
        super("com.mysql.jdbc.Driver", "mysql", dbUrl, user, password, poolSize);
    }
}
