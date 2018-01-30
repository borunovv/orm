package com.borunovv.db;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author borunovv
 */
public class MySQLAccessTest extends Assert {

    @Test
    public void testTransactions() throws Exception {
        final DBAccess dbAccess = new MySQLAccess("localhost/test", "root", "1", 10);
        TransactionTester.testTransactions(dbAccess, 100);
    }
}
