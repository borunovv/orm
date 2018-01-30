package com.borunovv.db;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Базовый класс обертки над любым JDBC-драйвером.
 *
 * @author borunovv
 */
public abstract class DBAccess {

    private static final int DEFAULT_CONNECTION_POOL_SIZE = 1;

    // URL к базе данных, например "localhost/test", или полностью: "jdbc:mysql://localhost/test?param=value"
    private String dbUrl;
    private String user;
    private String password;
    private String driverClassName; // Имя класса JDBC-драйвера (для MySQL - 'com.mysql.jdbc.Driver').
    private String protocolScheme;  // Схема протокола (часть урл к БД вида "jdbc:[она тут]//...". Для MySQL - "mysql".
                                    // Необязательно, если в dbUrl задан полный url (типа "jdbc:mysql:...").
    private ConnectionPool pool;

    private final AtomicBoolean classLoaded = new AtomicBoolean(false);

    public DBAccess(String driverClassName, String protocolScheme,
                    String dbUrl, String user, String password) throws SQLException, ClassNotFoundException {
        this(driverClassName, protocolScheme,
                dbUrl, user, password, DEFAULT_CONNECTION_POOL_SIZE);
    }

    public DBAccess(String driverClassName, String protocolScheme,
                    String dbUrl, String user, String password, int poolSize) throws SQLException, ClassNotFoundException {

        this.driverClassName = driverClassName;
        this.protocolScheme = protocolScheme;
        this.dbUrl = getFullUrl(dbUrl);
        this.user = user;
        this.password = password;
        this.pool = new ConnectionPool(poolSize);
        init();
    }

    public void close() throws SQLException, InterruptedException {
        if (pool != null) {
            pool.closeAll();
        }
    }

    public <T> T executeSelect(final String selectQuery, final IResultSetProcessor<T> processor) throws SQLException, InterruptedException {
        ensureInitialized();

        return exec(new IExecuteWithConnection<T>() {
            public T execute(Connection conn) throws SQLException {
                ResultSet resultSet = null;
                Statement statement = null;
                try {
                    // Statements allow to issue SQL queries to the database
                    statement = conn.createStatement();
                    resultSet = statement.executeQuery(selectQuery);
                    return processor.process(resultSet);

                } finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                    if (statement != null) {
                        statement.close();
                    }
                }
            }
        });
    }

    public void executeUpdate(final String updateQuery) throws SQLException, InterruptedException {
        ensureInitialized();

        exec(new IExecuteWithConnection<Integer>() {
            public Integer execute(Connection conn) throws SQLException {
                boolean autoCommitBefore = conn.getAutoCommit();
                Statement statement = null;
                try {
                    conn.setAutoCommit(true);
                    // Statements allow to issue SQL queries to the database
                    statement = conn.createStatement();
                    statement.executeUpdate(updateQuery);
                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                    conn.setAutoCommit(autoCommitBefore);
                }
                return 0;
            }
        });
    }

    public void executeInTransaction(final IExecuteInTransaction executor) throws SQLException, InterruptedException {
        ensureInitialized();

        exec(new IExecuteWithConnection<Integer>() {
            public Integer execute(Connection conn) throws SQLException {
                boolean autoCommitBefore = conn.getAutoCommit();
                int transactionIsolationBefore = conn.getTransactionIsolation();
                final List<ResultSet> resultSets = new LinkedList<ResultSet>();
                Statement statement = null;

                try {
                    conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                    conn.setAutoCommit(false);

                    final Statement localStatement = conn.createStatement();
                    statement = localStatement;

                    IQueryExecutor localExecutor = new IQueryExecutor() {
                        public void executeUpdate(String updateQuery) throws SQLException {
                            localStatement.executeUpdate(updateQuery);
                        }

                        public ResultSet executeSelect(String selectQuery) throws SQLException {
                            ResultSet res = localStatement.executeQuery(selectQuery);
                            resultSets.add(res);
                            return res;
                        }
                    };

                    executor.execute(localExecutor);

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {

                    for (ResultSet resultSet : resultSets) {
                        if (resultSet != null) {
                            resultSet.close();
                        }
                    }
                    if (statement != null) {
                        statement.close();
                    }
                    conn.setAutoCommit(autoCommitBefore);
                    conn.setTransactionIsolation(transactionIsolationBefore);
                }
                return 0;
            }
        });
    }

    public DatabaseMetaData getMetaData() throws SQLException, InterruptedException {
        ensureInitialized();

        return exec(new IExecuteWithConnection<DatabaseMetaData>() {
            public DatabaseMetaData execute(Connection conn) throws SQLException {
                return conn.getMetaData();
            }
        });
    }

    private <T> T exec(IExecuteWithConnection<T> executor) throws SQLException, InterruptedException {
        ensureInitialized();

        Connection conn = null;
        try {
            conn = pool.getConnection();
            return executor.execute(conn);
        } finally {
            pool.putConnectionBack(conn);
        }
    }

    private void ensureInitialized() {
        if (pool == null) {
            throw new IllegalStateException("Connection pool is null.");
        }
    }

    private void init() throws SQLException, ClassNotFoundException {
        // This will load the MySQL driver, each DB has its own driver
        if (classLoaded.compareAndSet(false, true)) {
            Class.forName(driverClassName);
        }
    }

    private String getFullUrl(String url) {
        String fullUrl = url;
        String protocolPrefix = "jdbc:" + protocolScheme + "://";
        if (!url.startsWith("jdbc:")) {
            fullUrl = protocolPrefix + fullUrl;
            if (!fullUrl.contains("?")) {
                fullUrl = fullUrl + "?useUnicode=yes&characterEncoding=UTF-8";
            }
        }
        return fullUrl;
    }

    private Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, user, password);
    }


    public static interface IResultSetProcessor<T> {
        public T process(ResultSet resultSet) throws SQLException;
    }

    public static interface IExecuteInTransaction {
        public void execute(IQueryExecutor queryExecutor) throws SQLException;
    }

    public static interface IQueryExecutor {
        public void executeUpdate(String updateQuery) throws SQLException;

        public ResultSet executeSelect(String selectQuery) throws SQLException;
    }


    private static interface IExecuteWithConnection<T> {
        public T execute(Connection conn) throws SQLException;
    }

    // Простой ("fixed-size") пул соединений с ДБ (фиксированного размера,
    // наполняется по мере необходимости, без таймаутов простоя).
    private class ConnectionPool {
        private BlockingQueue<Connection> freeConnections;
        private volatile int capacity = 0;
        private volatile int count = 0;

        public ConnectionPool(int capacity) {
            this.capacity = capacity;
            freeConnections = new ArrayBlockingQueue<Connection>(capacity);
        }

        public Connection getConnection() throws SQLException, InterruptedException {
            synchronized (this) {
                if (capacity == 0) {
                    throw new IllegalStateException("Pool is closed");
                }
            }

            if (freeConnections.isEmpty()) {
                synchronized (this) {
                    if (count < capacity) {
                        freeConnections.add(createNewConnection());
                        count++;
                    }
                }
            }
            return freeConnections.take();
        }

        public void putConnectionBack(Connection conn) {
            if (conn == null) {
                throw new IllegalArgumentException("Connection is null, url='" + dbUrl + "'");
            }
            freeConnections.add(conn);
        }

        public void closeAll() throws SQLException, InterruptedException {
            synchronized (this) {
                while (count > 0) {
                    Connection conn = freeConnections.take();
                    conn.close();
                    count--;
                }
                capacity = 0;
            }
        }
    }
}
