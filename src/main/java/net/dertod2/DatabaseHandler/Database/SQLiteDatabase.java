package net.dertod2.DatabaseHandler.Database;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.dertod2.DatabaseHandler.Binary.DatabaseHandler;

public class SQLiteDatabase extends AbstractDatabase {
    private SQLiteConnection connection; // SQLite only supports one connection

    public SQLiteDatabase(String database) throws SQLException {
        super(null, null,
                DatabaseHandler.getInstance().getDataFolder().getAbsolutePath() + File.separatorChar + database, null,
                null);
    }

    public SQLiteDatabase(File database) throws SQLException {
        super(null, null, database.getAbsolutePath(), null, null);
    }

    public DatabaseType getType() {
        return DatabaseType.SQLite;
    }

    public Connection getConnection() {
        try {
            if (this.connection != null && !this.connection.isClosed())
                return this.connection;
            this.connection = new SQLiteConnection(DriverManager.getConnection(this.getConnectionString()));

            return this.connection;
        } catch (SQLException exc) {
            exc.printStackTrace();
            return null;
        }
    }

    protected String getConnectionString() {
        return "jdbc:sqlite:" + this.database + ".sqlite";
    }

    public boolean tableExist(String tableName) {
        Connection connection = this.getConnection();

        try {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getTables(null, null, tableName, null);
            return resultSet.next();
        } catch (Exception exc) {
            exc.printStackTrace();
            return false;
        }
    }
}