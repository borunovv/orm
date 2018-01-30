package com.borunovv.db;

import java.sql.SQLException;

/**
 * @author borunovv
 */
public class Helper {

    public static void fillDB(DBAccess dbAccess, int count) throws SQLException, InterruptedException {
        String dropTableQuery = "DROP TABLE IF EXISTS accum";
        String createTableQuery = "" +
                "CREATE TABLE accum (\n" +
                "  id bigint(20) NOT NULL AUTO_INCREMENT,\n" +
                "  value bigint(20) NOT NULL DEFAULT '0',\n" +
                "  PRIMARY KEY (id)\n" +
                ")";


        dbAccess.executeUpdate(dropTableQuery);
        dbAccess.executeUpdate(createTableQuery);

        for (int i = 0; i < count; ++i) {
            dbAccess.executeUpdate(String.format("INSERT INTO accum(value) VALUES (%d)", i));
        }
    }
}
