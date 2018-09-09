package net.dertod2.DatabaseHandler.Database.Pooler;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class PooledConnection implements Connection {
    protected ConnectionPool poolReference;

    protected Connection rawConnection;

    protected long created;
    protected long lastActive;
    protected long loaned;

    protected boolean returnToPool = false;
    protected boolean isInPool = true;

    protected boolean autoClose = true;

    protected String currentUser = "None";

    protected PooledConnection(ConnectionPool poolReference, Connection connection) {
        this.poolReference = poolReference;

        this.rawConnection = connection;

        this.created = System.currentTimeMillis();
        this.lastActive = System.currentTimeMillis();
    }

    public Connection getRawConnection() {
        return this.rawConnection;
    }

    public ConnectionPool getPool() {
        return this.poolReference;
    }

    public long getCreated() {
        return this.created;
    }

    public long getLifetime() {
        return System.currentTimeMillis() - this.created;
    }

    public long getLastActive() {
        return this.lastActive;
    }

    public long getIdletime() {
        return System.currentTimeMillis() - this.lastActive;
    }

    public long getLoaned() {
        return this.loaned;
    }

    public long getLoanedtime() {
        return System.currentTimeMillis() - this.loaned;
    }

    public boolean isInPool() {
        return this.isInPool;
    }

    public String getFetcher() {
        return this.currentUser;
    }

    /**
     * Defines if the Pool should kill the connection when the fetcher doesn't gives
     * it back to the pool
     * 
     * @param autoClose
     */
    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }

    /**
     * Defines if the Pool should kill the connection when the fetcher doesn't gives
     * it back to the pool
     * 
     * @return
     */
    public boolean getAutoClose() {
        return this.autoClose;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.rawConnection.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.rawConnection.isWrapperFor(iface);
    }

    public Statement createStatement() throws SQLException {
        return this.rawConnection.createStatement();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.prepareStatement(sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.prepareCall(sql);
    }

    public String nativeSQL(String sql) throws SQLException {
        return this.rawConnection.nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.rawConnection.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return this.rawConnection.getAutoCommit();
    }

    public void commit() throws SQLException {
        this.lastActive = System.currentTimeMillis();
        this.rawConnection.commit();
    }

    public void rollback() throws SQLException {
        this.lastActive = System.currentTimeMillis();
        this.rawConnection.rollback();
    }

    /**
     * Returns this Connection to the Connection Pool instead of closing it.
     */
    public void close() throws SQLException {
        if (this.poolReference == null)
            this.rawConnection.close();

        this.returnToPool = true; // To inform the Pool Watcher
    }

    public boolean isClosed() throws SQLException {
        return this.rawConnection.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return this.rawConnection.getMetaData();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        this.rawConnection.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return this.rawConnection.isReadOnly();
    }

    public void setCatalog(String catalog) throws SQLException {
        this.rawConnection.setCatalog(catalog);
    }

    public String getCatalog() throws SQLException {
        return this.rawConnection.getCatalog();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        this.rawConnection.setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return this.rawConnection.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return this.rawConnection.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        this.rawConnection.clearWarnings();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.createStatement(resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return this.rawConnection.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        this.rawConnection.setTypeMap(map);
    }

    public void setHoldability(int holdability) throws SQLException {
        this.rawConnection.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return this.rawConnection.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.setSavepoint(name);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        this.rawConnection.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        this.rawConnection.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.prepareStatement(sql, columnNames);
    }

    public Clob createClob() throws SQLException {
        return this.rawConnection.createClob();
    }

    public Blob createBlob() throws SQLException {
        return this.rawConnection.createBlob();
    }

    public NClob createNClob() throws SQLException {
        return this.rawConnection.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException {
        return this.rawConnection.createSQLXML();
    }

    public boolean isValid(int timeout) throws SQLException {
        return this.rawConnection.isValid(timeout);
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        this.lastActive = System.currentTimeMillis();
        this.rawConnection.setClientInfo(name, value);
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        this.lastActive = System.currentTimeMillis();
        this.rawConnection.setClientInfo(properties);
    }

    public String getClientInfo(String name) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.getClientInfo(name);
    }

    public Properties getClientInfo() throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.getClientInfo();
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return this.rawConnection.createArrayOf(typeName, elements);
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        this.lastActive = System.currentTimeMillis();
        return this.rawConnection.createStruct(typeName, attributes);
    }

    public void setSchema(String schema) throws SQLException {
        this.rawConnection.setSchema(schema);
    }

    public String getSchema() throws SQLException {
        return this.rawConnection.getSchema();
    }

    public void abort(Executor executor) throws SQLException {
        this.rawConnection.abort(executor);
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        this.rawConnection.setNetworkTimeout(executor, milliseconds);
    }

    public int getNetworkTimeout() throws SQLException {
        return this.rawConnection.getNetworkTimeout();
    }
}