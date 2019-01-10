package net.dertod2.DatabaseHandler.Binary;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import net.dertod2.DatabaseHandler.Commands.DatabaseHandlerCommand;
import net.dertod2.DatabaseHandler.Database.AbstractDatabase;
import net.dertod2.DatabaseHandler.Database.DatabaseType;
import net.dertod2.DatabaseHandler.Database.MySQLDatabase;
import net.dertod2.DatabaseHandler.Database.PostGREDatabase;
import net.dertod2.DatabaseHandler.Database.SQLiteDatabase;
import net.dertod2.DatabaseHandler.Table.EntryHandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Differences between Canary DataAccess and DerTod2's DatabaseHandler:<br />
 * + allows saving of Timestamp Objects<br />
 * + doesn't need an primary key (in canary the primary key is hardcoded)<br />
 * + sets the table name over an annotation and not over the constructor<br />
 * + automatic column type detection<br />
 * + when inserting rows with an primary key the key in the entry will be
 * updated automatically<br />
 * + access to private and protected variables, no publics needed<br />
 * + support for postgres and also mysql/sqlite<br />
 * + contains all needed drivers<br />
 * - currently no support for xml<br />
 * 
 * @author DerTod2
 *
 */
public class DatabaseHandler extends JavaPlugin {
    public static boolean debugMode;

    private static DatabaseHandler databaseHandler;
    private static AbstractDatabase abstractDatabase;
    private static EntryHandler entryHandler;

    public static Updater updater;

    public void onEnable() {
        this.saveDefaultConfig(); // Generates default config when not already created

        DatabaseHandler.databaseHandler = this;

        FileConfiguration fileConfiguration = this.getConfig();
        DatabaseHandler.debugMode = fileConfiguration.getBoolean("debug", false);

        DatabaseType databaseType = DatabaseType.byName(fileConfiguration.getString("type"));
        if (databaseType != null) {
            int port = fileConfiguration.getInt("port", -1);

            try {
                switch (databaseType) {
                case MySQL:
                    DatabaseHandler.abstractDatabase = new MySQLDatabase(fileConfiguration.getString("host"),
                            port != -1 ? port : DatabaseType.MySQL.getDriverPort(),
                            fileConfiguration.getString("database"), fileConfiguration.getString("username"),
                            fileConfiguration.getString("password"));
                    break;
                case PostGRE:
                    DatabaseHandler.abstractDatabase = new PostGREDatabase(fileConfiguration.getString("host"),
                            port != -1 ? port : DatabaseType.PostGRE.getDriverPort(),
                            fileConfiguration.getString("database"), fileConfiguration.getString("username"),
                            fileConfiguration.getString("password"));
                    break;
                case SQLite:
                    DatabaseHandler.abstractDatabase = new SQLiteDatabase(fileConfiguration.getString("database"));
                    break;
                }

                if (DatabaseHandler.abstractDatabase != null)
                    DatabaseHandler.entryHandler = DatabaseHandler.abstractDatabase.getHandler();
            } catch (SQLException exc) {
                Bukkit.getConsoleSender()
                        .sendMessage(ChatColor.RED + "Fatal error while setting up the database Connection Pool");
                exc.printStackTrace();
            }
        } else {
            Bukkit.getConsoleSender()
                    .sendMessage(ChatColor.RED + "No database driver set! Please configure the config.yml");
            System.exit(-1);
        }

        getCommand("databasehandler").setExecutor(new DatabaseHandlerCommand());

        DatabaseHandler.updater = new Updater(this.getFile());
        DatabaseHandler.updater.check(Bukkit.getConsoleSender());
    }

    public void onDisable() {
        DatabaseHandler.abstractDatabase.shutdown();
        DatabaseHandler.abstractDatabase = null;
    }

    public static AbstractDatabase get() {
        return DatabaseHandler.abstractDatabase;
    }

    public static Connection getConnection() throws SQLException {
        if (DatabaseHandler.abstractDatabase == null)
            return null;
        return DatabaseHandler.abstractDatabase.getConnection();
    }

    public static DatabaseHandler getInstance() {
        return DatabaseHandler.databaseHandler;
    }

    public static FileConfiguration getConfiguration() {
        return DatabaseHandler.databaseHandler.getConfig();
    }

    /**
     * Creates an extra SQLite Database besides the integrated opened database<br />
     * Only use the entry handler over the internal funtion inside the
     * SQLiteDatabase Object!
     * 
     * @param file
     *            File Object
     * @return SQLiteDatabase Object
     * @throws SQLException
     */
    public static SQLiteDatabase getExtraMySQLDatabase(File file) throws SQLException {
        return new SQLiteDatabase(file);
    }

    /**
     * Returns the {@link EntryHandler} for working with the database
     * 
     * @return EntryHandler
     * @deprecated use the EntryHandler over the
     *             {@link AbstractDatabase#getHandler()} method
     */
    public static EntryHandler getHandler() {
        return DatabaseHandler.entryHandler;
    }
}