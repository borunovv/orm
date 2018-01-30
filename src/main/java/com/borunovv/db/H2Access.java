package com.borunovv.db;

import java.sql.SQLException;

/**
 * Обертка над JDBC-драйвером H2.
 * Внимание: после использования надо вызвать close() !
 *
 * @author borunovv
 */
public class H2Access extends DBAccess {

    public H2Access(String dbUrl, String user, String password) throws SQLException, ClassNotFoundException {
        this(dbUrl, user, password, 1);
    }

    public H2Access(String dbUrl, String user, String password, int poolSize) throws SQLException, ClassNotFoundException {
        super("org.h2.Driver", "h2", dbUrl, user, password, poolSize);
    }
}

