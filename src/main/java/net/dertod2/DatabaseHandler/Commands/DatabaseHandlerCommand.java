package net.dertod2.DatabaseHandler.Commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.dertod2.DatabaseHandler.Binary.DatabaseHandler;
import net.dertod2.DatabaseHandler.Database.Pooler.ConnectionPool;
import net.dertod2.DatabaseHandler.Database.Pooler.PoolSettings;
import net.dertod2.DatabaseHandler.Database.Pooler.PoolStatistics;
import net.dertod2.DatabaseHandler.Database.Pooler.PooledConnection;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class DatabaseHandlerCommand implements TabExecutor {
	private static long second = 1000, minute = 60000, hour = 3600000, day = 86400000, month = NumberUtils.toLong("2592000000"), year = NumberUtils.toLong("31104000000");	
	
	private final Map<String, String> tabCategoryMap = ImmutableMap.<String, String>builder()
			.put("reload", "databasehandler.commands.databasehandler.reload")
			.put("restart", "databasehandler.commands.databasehandler.restart")
			.put("update", "databasehandler.commands.databasehandler.update")
			.put("stats", "databasehandler.commands.databasehandler.stats")
			.put("list", "databasehandler.commands.databasehandler.list")
			.build();
	private Map<String, BaseComponent[]> helpMap;
	
	public DatabaseHandlerCommand() {
		this.helpMap = new HashMap<String, BaseComponent[]>();
		
		this.helpMap.put("databasehandler.commands.databasehandler.reload", 
				new ComponentBuilder("/dh ").color(ChatColor.DARK_GRAY)
				.append("reload").color(ChatColor.GRAY)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Reloads the plugin and settings")))
				.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/databasehandler:dh reload")).create());
		this.helpMap.put("databasehandler.commands.databasehandler.restart", 
				new ComponentBuilder("/dh ").color(ChatColor.DARK_GRAY)
				.append("restart").color(ChatColor.GRAY)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Restarts the database pool")))
				.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/databasehandler:dh restart")).create());	
		this.helpMap.put("databasehandler.commands.databasehandler.update", 
				new ComponentBuilder("/dh ").color(ChatColor.DARK_GRAY)
				.append("update").color(ChatColor.GRAY)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Searches for new versions of this plugin")))
				.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/databasehandler:dh update")).create());	
		this.helpMap.put("databasehandler.commands.databasehandler.stats", 
				new ComponentBuilder("/dh ").color(ChatColor.DARK_GRAY)
				.append("stats").color(ChatColor.GRAY)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Shows different statistics over the connection pool")))
				.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/databasehandler:dh stats")).create());	
		this.helpMap.put("databasehandler.commands.databasehandler.list", 
				new ComponentBuilder("/dh ").color(ChatColor.DARK_GRAY)
				.append("list").color(ChatColor.GRAY)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Shows informations over loaned connections")))
				.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/databasehandler:dh list")).create());
	}
	
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1 && args[0].equalsIgnoreCase("reload") && sender.hasPermission(this.tabCategoryMap.get("reload"))) {
			DatabaseHandler.getInstance().reloadConfig();
			Bukkit.getPluginManager().disablePlugin(DatabaseHandler.getInstance());
			Bukkit.getPluginManager().enablePlugin(DatabaseHandler.getInstance());		
			sender.sendMessage(ChatColor.GREEN + "Plugin and configuration reloaded!");
		} else if (args.length == 1 && args[0].equalsIgnoreCase("restart") && sender.hasPermission(this.tabCategoryMap.get("restart"))) {
			DatabaseHandler.get().getPool().restart();
			sender.sendMessage(ChatColor.GREEN + "Restarted database pool.");
		} else if (args.length == 1 && args[0].equalsIgnoreCase("update") && sender.hasPermission(this.tabCategoryMap.get("update"))) {
			sender.sendMessage(ChatColor.GREEN + "Checking for updates... When no further messages appears there are no updates available!");
			DatabaseHandler.updater.check(sender);
		} else if (args.length == 1 && args[0].equalsIgnoreCase("stats") && sender.hasPermission(this.tabCategoryMap.get("stats"))) {
			if (sender instanceof Player) {
				this.stats((Player) sender);
			} else {
				sender.sendMessage(ChatColor.RED + "Currently only available ingame!");
			}
		} else if (args.length == 1 && args[0].equalsIgnoreCase("list") && sender.hasPermission(this.tabCategoryMap.get("list"))) {
			if (sender instanceof Player) {
				this.list((Player) sender);
			} else {
				sender.sendMessage(ChatColor.RED + "Currently only available ingame!");
			}
		} else {
			for (Entry<String, BaseComponent[]> entry : this.helpMap.entrySet()) {
				if (sender.hasPermission(entry.getKey())) {
					if (sender instanceof Player) {
						((Player) sender).spigot().sendMessage(entry.getValue());
					} else {
						sender.sendMessage(TextComponent.toLegacyText(entry.getValue()));
					}
				}
			}
		}
		
		return true;
	}
	
	private void stats(Player sender) {
		sender.sendMessage(ChatColor.DARK_GREEN + "General Statistics of the database pool: ");

		ConnectionPool connectionPool = DatabaseHandler.get().getPool();
		
		PoolStatistics poolStatistics = connectionPool.getStatistics();
		PoolSettings poolSettings = connectionPool.getSettings();
		
		BaseComponent[] general = new ComponentBuilder("Settings: ").color(ChatColor.DARK_GREEN)
		.append(" - Show here <-").color(ChatColor.GOLD)
			.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
					new ComponentBuilder("")
					.append("Smallest Pool Size: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolSettings.getMinimumPoolSize())).color(ChatColor.GOLD)
					.append("\n")
					.append("Biggest Pool Size: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolSettings.getMaximumPoolSize())).color(ChatColor.GOLD)
					.append("\n")
					.append("Minimum Available: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolSettings.getMinimumAvailable())).color(ChatColor.GOLD)
					.append("\n")
					.append("Highest Idle-Time: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolSettings.getIdletime())).color(ChatColor.GOLD)
					.append("\n")
					.append("Maximum Life-Time: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolSettings.getLifetime())).color(ChatColor.GOLD)
					.append("\n")
					.append("Time until Sleep-Mode: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolSettings.getStartSleepMode())).color(ChatColor.GOLD)
					.create()))
			.create();
		sender.spigot().sendMessage(general);

		sender.sendMessage("");
		sender.sendMessage(ChatColor.DARK_GREEN + "Count of watcher loops: " + ChatColor.GOLD + poolStatistics.getWatcherRuns());
		sender.sendMessage(ChatColor.DARK_GREEN + "Watcher loop duration: " + ChatColor.GOLD + poolStatistics.getLastWatcherDuration() + "ms");	
		sender.sendMessage(ChatColor.DARK_GREEN + "Returned to pool: " + ChatColor.GOLD + poolStatistics.getReturnedToPool());
		sender.sendMessage(ChatColor.DARK_GREEN + "Time since last request: " + ChatColor.GOLD + timeToString(connectionPool.getLastFetchTime()));
		sender.sendMessage(ChatColor.DARK_GREEN + "Thread Locks: " + ChatColor.GOLD + poolStatistics.getThreadLock());
		sender.sendMessage(ChatColor.DARK_GREEN + "Sleep-Mode: " + ChatColor.GOLD + (connectionPool.getLastFetchTime() >= poolSettings.getStartSleepMode() ? "Active" : "Inactive"));
		sender.sendMessage("");
		
		BaseComponent[] connections = new ComponentBuilder("Connections: ").color(ChatColor.DARK_GREEN)
		.append("-> Show here <-").color(ChatColor.GOLD)
			.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
					new ComponentBuilder("")
					.append("Created: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolStatistics.getOpenedConnections())).color(ChatColor.GOLD)
					.append("\n")
					.append("----------")
					.append("Active: ").color(ChatColor.DARK_GREEN).append(String.valueOf(connectionPool.getAvailableConnections() + connectionPool.getLoanedConnections())).color(ChatColor.GOLD)
					.append("\n")
					.append("Loaned: ").color(ChatColor.DARK_GREEN).append(String.valueOf(connectionPool.getLoanedConnections())).color(ChatColor.GOLD)
					.append("\n")
					.append("In pool: ").color(ChatColor.DARK_GREEN).append(String.valueOf(connectionPool.getAvailableConnections())).color(ChatColor.GOLD)
					.create()))
		.create();
		sender.spigot().sendMessage(connections);
		
		int closedConnections = poolStatistics.getMaxIdleTimeReached() + poolStatistics.getMaxLifeTimeReached() + poolStatistics.getMaxLoanedTimeReached() + poolStatistics.getInvalidConnection();
		BaseComponent[] closed = new ComponentBuilder("Closed Connections: ").color(ChatColor.DARK_GREEN)
		.append(String.valueOf(closedConnections) + " ").color(ChatColor.GOLD)
		.append("-> Show here <-").color(ChatColor.GOLD)
			.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
					new ComponentBuilder("")
					.append("By reaching idle-time: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolStatistics.getMaxIdleTimeReached())).color(ChatColor.GOLD)
					.append("\n")
					.append("By reaching life-time: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolStatistics.getMaxLifeTimeReached())).color(ChatColor.GOLD)
					.append("\n")
					.append("By reaching loaned-time: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolStatistics.getMaxLoanedTimeReached())).color(ChatColor.GOLD)
					.append("\n")
					.append("By becoming invalid: ").color(ChatColor.DARK_GREEN).append(String.valueOf(poolStatistics.getInvalidConnection())).color(ChatColor.GOLD)
					.create()))
			.create();
		sender.spigot().sendMessage(closed);
	}
	
	private void list(Player sender) {
		List<PooledConnection> connectionList = DatabaseHandler.get().getPool().getActiveConnections();
		if (connectionList.size() > 0) {
			// Verfügbare Verbindung: <Aktive Zeit> {Hover: Idlezeit}
			// Vergebene Verbindung: <Aktive Zeit> {Hover: Idlezeit: Fetcher, Verliehene Zeit}
			sender.sendMessage(ChatColor.DARK_GREEN + "There are " + ChatColor.GOLD + connectionList.size() + ChatColor.DARK_GREEN + " Connections opened: ");
			for (PooledConnection pooledConnection : connectionList) {
				if (pooledConnection.isInPool()) {
					sender.spigot().sendMessage(
							new ComponentBuilder("Available Connection: ").color(ChatColor.DARK_GREEN)
							.append(timeToString(pooledConnection.getLifetime())).color(ChatColor.GOLD)
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Time since connection created").create()))
							.append(" (Show more)")
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Idle: ").color(ChatColor.DARK_GREEN).append(timeToString(pooledConnection.getIdletime())).color(ChatColor.GOLD).create()))
							.create()
							);
				} else {
					sender.spigot().sendMessage(
							new ComponentBuilder("Loaned Connection: ").color(ChatColor.DARK_GREEN)
							.append(timeToString(pooledConnection.getLifetime())).color(ChatColor.GOLD)
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Time since connection created").create()))
							.append(" (Show more)")
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("")
									.append("Idle: ").color(ChatColor.DARK_GREEN).append(timeToString(pooledConnection.getIdletime())).color(ChatColor.GOLD)
									.append("\n")
									.append("Loaned to: ").color(ChatColor.DARK_GREEN).append(pooledConnection.getFetcher()).color(ChatColor.GOLD)
									.append("\n")
									.append("Loaned before: ").color(ChatColor.DARK_GREEN).append(timeToString(pooledConnection.getLoanedtime())).color(ChatColor.GOLD)
									.append("\n")
									.create()
								))
							.create());
				}
			}
		} else {
			sender.sendMessage(ChatColor.RED + "There are no active connections!");
		}
	}
	
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return DatabaseHandlerCommand.tabCompleteList(args[0], this.tabCategoryMap, sender);
		}
		
		return ImmutableList.<String>of();
	}
	
	private static List<String> tabCompleteList(final String token, final Map<String, String> originals, final Permissible permissible) {
		List<String> collection = new ArrayList<String>(originals.size());
		
		for (Entry<String, String> entry : originals.entrySet()) {
			if (entry.getValue().length() == 0 || permissible.hasPermission(entry.getValue())) {
				if (StringUtil.startsWithIgnoreCase(entry.getKey(), token)) {
					collection.add(entry.getKey());
				}
			}
		}
		
		return collection;
	}
	
	private static String timeToString(long time) {
		long timeYears = time > DatabaseHandlerCommand.year ? time / DatabaseHandlerCommand.year : 0;
		time -= timeYears * DatabaseHandlerCommand.year;
		long timeMonths = time > DatabaseHandlerCommand.month ? time / DatabaseHandlerCommand.month : 0;
		time -= timeMonths * DatabaseHandlerCommand.month;
		long timeDays = time > DatabaseHandlerCommand.day ? time / DatabaseHandlerCommand.day : 0;
		time -= timeDays * DatabaseHandlerCommand.day;
		long timeHours = time > DatabaseHandlerCommand.hour ? time / DatabaseHandlerCommand.hour : 0;
		time -= timeHours * DatabaseHandlerCommand.hour;
		long timeMinutes = time > DatabaseHandlerCommand.minute ? time / DatabaseHandlerCommand.minute : 0;
		time -= timeMinutes * DatabaseHandlerCommand.minute;
		long timeSeconds = time > DatabaseHandlerCommand.second ? time / DatabaseHandlerCommand.second : 0;
		time -= timeSeconds * DatabaseHandlerCommand.second;
		
		String returnableString = "";
		
		returnableString += timeYears != 0 ? timeYears + " " + (timeYears > 1 ? "years" : "year") + " " : "";
		returnableString += timeMonths != 0 ? timeMonths + " " + (timeMonths > 1 ? "months" : "month") + " " : "";
		returnableString += timeDays != 0 ? timeDays + " " + (timeDays > 1 ? "days" : "day") + " " : "";
		
		returnableString += timeHours != 0 ? timeHours + " " + (timeHours > 1 ? "hours" : "hour") + " " : "";
		returnableString += timeMinutes != 0 ? timeMinutes + " " + (timeMinutes > 1 ? "minutes" : "minute") + " " : "";
		returnableString += timeSeconds != 0 ? timeSeconds + " " + (timeSeconds > 1 ? "seconds" : "second") + " " : "";
	
		if (returnableString.length() == 0) returnableString = "< 1 " + "second";
		return returnableString.trim();
	}
}