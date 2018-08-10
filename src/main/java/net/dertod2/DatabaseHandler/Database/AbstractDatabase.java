package net.dertod2.DatabaseHandler.Database;

import java.sql.Connection;
import java.sql.SQLException;
import net.dertod2.DatabaseHandler.Database.Pooler.ConnectionPool;
import net.dertod2.DatabaseHandler.Database.Pooler.PoolSettings;
import net.dertod2.DatabaseHandler.Table.EntryHandler;
import net.dertod2.DatabaseHandler.Table.MySQLEntryHandler;
import net.dertod2.DatabaseHandler.Table.PostGREEntryHandler;
import net.dertod2.DatabaseHandler.Table.SQLiteEntryHandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public abstract class AbstractDatabase {
	protected ConnectionPool connectionPool;
	
	protected String host;
	protected Integer port;
	
	protected String database;
	protected String username;
	protected String password;
	
	protected EntryHandler entryHandler;
	
	public AbstractDatabase(String host, Integer port, String database, String username, String password) throws SQLException {
		this.host = host;
		this.port = port;
		
		this.database = database;
		this.username = username;
		this.password = password;
		
		if (this.getType().isUsingDatabaseDriver()) {
			try {
				Class.forName(this.getType().getDriverPackage());
				Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Database Driver '" + ChatColor.GOLD + this.getType().name() + ChatColor.GREEN + "' successfully loaded...");
			} catch (ClassNotFoundException exc) {
				exc.printStackTrace();
			}

			if (this.getType().isUsingConnectionPool()) {
				PoolSettings poolSettings = new PoolSettings();
				
				poolSettings.setUrl(this.getConnectionString());
				poolSettings.setUsername(this.username);
				poolSettings.setPassword(this.password);
	
				this.connectionPool = new ConnectionPool(poolSettings);
				this.connectionPool.startPool();
			}
			
			Connection connection = this.getConnection();
			if (connection != null) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Successfully etablished test connection to database. :-)");
				try {
					connection.close();
				} catch (SQLException exc) {
					exc.printStackTrace();
				}
			}
		} else {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Database Type '" + ChatColor.GOLD + this.getType().name() + ChatColor.GREEN + "' doesn't support Connections. Skip initializing BoneCP.");
		}
	}
	
	public abstract DatabaseType getType();
	
	/**
	 * Fetches an Connection out of the Connection Pool or fetches the SQLite Connection when used
	 * @return connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		if (this.connectionPool != null) {
			return this.connectionPool.getConnection();
		}
		
		return null;
	}
	
	public ConnectionPool getPool() {
		return this.connectionPool;
	}
	
	/**
	 * Returns the {@link EntryHandler} for working with the database
	 * @return EntryHandler
	 */
	public EntryHandler getHandler() {
		if (this.entryHandler != null) return this.entryHandler;
		
		switch (this.getType()) {
			case MySQL:
				this.entryHandler = new MySQLEntryHandler();
				break;
			case PostGRE:
				this.entryHandler = new PostGREEntryHandler();
				break;
			case SQLite:
				this.entryHandler = new SQLiteEntryHandler((SQLiteDatabase) this);
			default:
				break;
		}
		
		return this.entryHandler;
	}
	
	protected abstract String getConnectionString();
	
	public abstract boolean tableExist(String tableName);
	
	public void shutdown() {
		if (this.connectionPool != null) this.connectionPool.shutdown();
	}
}