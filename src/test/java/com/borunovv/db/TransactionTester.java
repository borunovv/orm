package com.borunovv.db;

import com.borunovv.db.DBAccess;
import com.borunovv.db.MySQLAccess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author borunovv
 */
public abstract class TransactionTester {

    public static void testTransactions(final DBAccess dbAccess, final int threadCount) throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        try {
            Helper.fillDB(dbAccess, 0);
            dbAccess.executeUpdate("INSERT INTO accum(id, value) VALUES (1, 0)");

            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threads.length; ++i) {
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        try {
                            // SELECT ... FOR UPDATE - рулит !!!
                            dbAccess.executeInTransaction(new MySQLAccess.IExecuteInTransaction() {
                                public void execute(MySQLAccess.IQueryExecutor queryExecutor) throws SQLException {
                                    // Эмулируем атомарный инкремент поля в таблице в рамках транзакции !
                                    ResultSet resultSet = queryExecutor.executeSelect("SELECT value FROM accum WHERE id=1 FOR UPDATE");
                                    // Читаем..
                                    Long value = resultSet.next() ? Long.parseLong(resultSet.getString(1)) : 0;
                                    // Инкрементим
                                    long newValue = value + 1;
                                    // Эмулируем задержку.
                                    try {
                                        Thread.sleep(System.currentTimeMillis() % 100 + 10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    // Сохраняем.
                                    queryExecutor.executeUpdate("UPDATE accum SET value=" + newValue + " WHERE id=1");
                                }
                            });

                            Long value = dbAccess.executeSelect("SELECT value FROM accum WHERE id=1", new MySQLAccess.IResultSetProcessor<Long>() {
                                public Long process(ResultSet resultSet) throws SQLException {
                                    return resultSet.next() ?
                                            Long.parseLong(resultSet.getString(1)) :
                                            0;
                                }
                            });

                            System.out.println(Thread.currentThread().getName() + ":" + value);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        counter.incrementAndGet();
                    }
                });
                threads[i].start();
            }

            System.out.println("Waiting..");
            while (counter.get() < threadCount) {
                Thread.sleep(100);
            }

            Long value = dbAccess.executeSelect("SELECT value FROM accum WHERE id=1", new MySQLAccess.IResultSetProcessor<Long>() {
                public Long process(ResultSet resultSet) throws SQLException {
                    return resultSet.next() ?
                            Long.parseLong(resultSet.getString(1)) :
                            0;
                }
            });
            // Убедимся, что счетчик в поле БД равен числу потоков
            // (т.е. каждый поток сделал 1 инкремент и не затер данные другого потока == инкремент прошел атомарно).

            if (value != threadCount) {
                System.out.println("==> ERROR!!!");
            } else {
                System.out.println("SUCCESS!");
            }
        } finally {
            dbAccess.close();
        }

        System.out.println("Done.");
    }
}
