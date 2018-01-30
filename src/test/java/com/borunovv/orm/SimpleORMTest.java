package com.borunovv.orm;

import com.borunovv.db.DBAccess;
import com.borunovv.db.H2Access;
import com.borunovv.db.Helper;
import com.borunovv.db.MySQLAccess;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @author borunovv
 */
public class SimpleORMTest {

    @Test
    public void testGetEntityList() throws Exception {
        DBAccess dbAccess = new H2Access("jdbc:h2:~/test;MVCC=true", "sa", "");
        //final DBAccess dbAccess = new MySQLAccess("localhost/test", "root", "1", 10);

        Helper.fillDB(dbAccess, 100);

        List<MyModel> list = SimpleORM.findAll(dbAccess, MyModel.class);

        for (MyModel myModel : list) {
            System.out.println(myModel);
        }

        dbAccess.close();
    }
}
