package com.borunovv.db;

import org.junit.Assert;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author borunovv
 */
public class H2AccessTest extends Assert {

    public static final String H2_DB_URL = "jdbc:h2:~/test;MVCC=true";
    public static final String H2_DB_USER = "sa";
    public static final String H2_DB_PASSWORD = "";

    @Test
    public void testAccess() throws Exception {
        final DBAccess dbAccess = createDBAccess(1);
        try {
            Helper.fillDB(dbAccess, 10);

            String value = dbAccess.executeSelect("SELECT value FROM accum WHERE id=1;",
                    new DBAccess.IResultSetProcessor<String>() {
                        public String process(ResultSet resultSet) throws SQLException {
                            resultSet.next();
                            return resultSet.getString("value");
                        }
                    });

            assertEquals("0", value);
        } finally {
            dbAccess.close();
        }
    }

    private DBAccess createDBAccess(int connectionPoolCapacity) throws SQLException, ClassNotFoundException {
        return new H2Access(H2_DB_URL, H2_DB_USER, H2_DB_PASSWORD, connectionPoolCapacity);
    }

    @Test
    public void testTransactions() throws Exception {
        final DBAccess dbAccess = createDBAccess(2);
        dbAccess.executeUpdate("SET LOCK_TIMEOUT 30000");

        TransactionTester.testTransactions(dbAccess, 10);
    }
}
