package net.dertod2.DatabaseHandler.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.dertod2.DatabaseHandler.Binary.DatabaseHandler;
import net.dertod2.DatabaseHandler.Database.SQLiteDatabase;
import net.dertod2.DatabaseHandler.Exceptions.EmptyFilterException;
import net.dertod2.DatabaseHandler.Exceptions.NoTableColumnException;
import net.dertod2.DatabaseHandler.Table.Column.ColumnType;
import net.dertod2.DatabaseHandler.Table.Column.DataType;
import net.dertod2.DatabaseHandler.Table.Column.EntryType;

public class SQLiteEntryHandler extends EntryHandler {
	private SQLiteDatabase sqLiteDatabase;
	
	public SQLiteEntryHandler(SQLiteDatabase sqLiteDatabase) {
		this.sqLiteDatabase = sqLiteDatabase;
	}
	
	public void insert(List<TableEntry> entryList) throws IllegalArgumentException, IllegalAccessException, SQLException {
		Connection connection = sqLiteDatabase.getConnection();
		for (TableEntry tableEntry : entryList) this.insert(tableEntry, connection);
		this.closeConnection(connection, null, null);
	}
	
	public void insert(TableEntry tableEntry) throws IllegalArgumentException, IllegalAccessException, SQLException {
		Connection connection = sqLiteDatabase.getConnection();
		this.insert(tableEntry, connection);
		this.closeConnection(connection, null, null);
	}
	
	private void insert(TableEntry tableEntry, Connection connection) throws IllegalArgumentException, IllegalAccessException, SQLException {
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		
		Map<Column, Object> dataList = tableEntry.getEntryColumns();
		Iterator<Column> iterator = dataList.keySet().iterator();
		
		StringBuilder columnList = new StringBuilder();
		StringBuilder valueList = new StringBuilder();
		
		while (iterator.hasNext()) {
			Column column = iterator.next();
			if (column.autoIncrement() || column.columnType() == ColumnType.Primary) continue; // Skip auto Increment - the database sets the value itself
			
			columnList.append("`" + column.columnName() + "`, ");
			valueList.append("?, ");
		}
		
		// Remove the leading letter
		if(columnList.length() > 0) columnList.delete(columnList.length() - 2, columnList.length());
		if(valueList.length() > 0) valueList.delete(valueList.length() - 2, valueList.length());
		
		preparedStatement = connection.prepareStatement("INSERT INTO `" + tableEntry.getTableName() + "` (" + columnList.toString() + ") VALUES (" + valueList.toString() + ");", PreparedStatement.RETURN_GENERATED_KEYS); 
		
		iterator = dataList.keySet().iterator(); // Need again... cause iterator cant start over again
		
		int index = 1;
		while (iterator.hasNext()) {
			Column column = iterator.next();
			if (column.autoIncrement() || column.columnType() == ColumnType.Primary) continue; // Skip auto Increment - the database sets the value itself
			Object columnValue = dataList.get(column);
			
			this.setStatement(index++, preparedStatement, columnValue, column);
		}
		
		preparedStatement.executeUpdate();
		
		boolean hasPrimaryKey = tableEntry.hasPrimaryKey();	
		Column primaryKey = tableEntry.getPrimaryKey();
		
		if (hasPrimaryKey) { // TODO - handle this when having multible auto increments
			resultSet = preparedStatement.getGeneratedKeys();
			if (resultSet.next()) tableEntry.setColumn(primaryKey, resultSet.getInt(1));
		}
		
		tableEntry.isLoadedEntry = true;
		this.closeConnection(null, preparedStatement, resultSet);
	}

	public void remove(TableEntry tableEntry, Map<String, Object> filterList) throws SQLException {
		Connection connection = sqLiteDatabase.getConnection();
		PreparedStatement preparedStatement = null;
		
		if (filterList != null && filterList.size() > 0) {
			StringBuilder stringBuilder = new StringBuilder();
			
			Iterator<String> iterator = filterList.keySet().iterator();
			while (iterator.hasNext()) {
				String columnName = iterator.next();
				if (filterList.get(columnName) == null) continue; // Can't check NULL variables
				if (tableEntry.getColumn(columnName) == null) throw new NoTableColumnException(columnName, tableEntry.getClass().getName());
				
				if (stringBuilder.length() > 0) {
					stringBuilder.append(" AND `" + columnName + "`");
				} else {
					stringBuilder.append("`" + columnName + "`");
				}
				
				stringBuilder.append(" = ?");
			}
			
			preparedStatement = connection.prepareStatement("DELETE FROM `" + tableEntry.getTableName() + "` WHERE " + stringBuilder.toString() + ";");
			
			int index = 1;
			iterator = filterList.keySet().iterator();
			while (iterator.hasNext()) {
				String columnName = iterator.next();
				
				Object columnValue = filterList.get(columnName);
				if (columnValue == null) continue; // Can't check NULL variables
				
				Column column = tableEntry.getColumn(columnName);
				this.setStatement(index++, preparedStatement, columnValue, column);
			}
			
			preparedStatement.executeUpdate();
		} else {
			preparedStatement = connection.prepareStatement("TRUNCATE TABLE `" + tableEntry.getTableName() + "`;");
			preparedStatement.executeUpdate();
		}
		
		this.closeConnection(connection, preparedStatement, null);
	}

	public boolean update(TableEntry tableEntry, Map<String, Object> filterList, List<String> specificRows) throws SQLException, IllegalArgumentException, IllegalAccessException {
		if (!tableEntry.isLoadedEntry) return false;
		
		Connection connection = sqLiteDatabase.getConnection();
		PreparedStatement preparedStatement = null;
		boolean returnResult;
		
		StringBuilder setBuilder = new StringBuilder();
		Map<Column, Object> columnList = tableEntry.getEntryColumns();
		
		Iterator<Column> setIterator = columnList.keySet().iterator();	
		while (setIterator.hasNext()) {
			Column column = setIterator.next();
			if (column.columnType() == ColumnType.Primary || column.autoIncrement()) continue;
			if (specificRows != null && !specificRows.isEmpty() && !specificRows.contains(column.columnName())) continue;
			
			if (setBuilder.length() > 0) {
				setBuilder.append(",  `" + column.columnName() + "` = ?");
			} else {
				setBuilder.append("`" + column.columnName() + "` = ?");
			}
		}
		
		int index = 1;
		
		if (filterList != null && filterList.size() > 0) {
			StringBuilder whereBuilder = new StringBuilder();
			
			Iterator<String> whereIterator = filterList.keySet().iterator();
			while (whereIterator.hasNext()) {
				String columnName = whereIterator.next();
				
				Column column = tableEntry.getColumn(columnName);
				if (column == null) throw new NoTableColumnException(columnName, tableEntry.getClass().getName());
				
				if (whereBuilder.length() > 0) {
					whereBuilder.append(" AND `" + columnName + "`");
				} else {
					whereBuilder.append("`" + columnName + "`");
				}
				
				whereBuilder.append(" = ?");
			}
			
			preparedStatement = connection.prepareStatement("UPDATE `" + tableEntry.getTableName() + "` SET " + setBuilder.toString() + " WHERE " + whereBuilder.toString() + ";");	
		
			setIterator = columnList.keySet().iterator();	
			while (setIterator.hasNext()) {
				Column column = setIterator.next();
				if (column.columnType() == ColumnType.Primary || column.autoIncrement()) continue;
				Object columnValue = columnList.get(column);
				
				this.setStatement(index++, preparedStatement, columnValue, column);
			}
			
			whereIterator = filterList.keySet().iterator();
			while (whereIterator.hasNext()) {
				String columnName = whereIterator.next();
				
				Column column = tableEntry.getColumn(columnName);
				Object columnValue = tableEntry.getColumn(column);
				
				this.setStatement(index++, preparedStatement, columnValue, column);
			}
		} else {
			preparedStatement = connection.prepareStatement("UPDATE `" + tableEntry.getTableName() + "` SET " + setBuilder.toString() + ";");
		
			setIterator = columnList.keySet().iterator();	
			while (setIterator.hasNext()) {
				Column column = setIterator.next();
				if (column.columnType() == ColumnType.Primary || column.autoIncrement()) continue;
				Object columnValue = columnList.get(column);
				
				this.setStatement(index++, preparedStatement, columnValue, column);
			}
		}
		
		returnResult = preparedStatement.executeUpdate() > 0;
		this.closeConnection(connection, preparedStatement, null);
		
		return returnResult;
	}

	public boolean update(TableEntry tableEntry, Map<String, Object> filterList, Map<String, Object> specificRows) throws Exception {
		if (specificRows == null || specificRows.isEmpty()) throw new Exception("The specificRows argument can't be null");
		
		Connection connection = sqLiteDatabase.getConnection();
		PreparedStatement preparedStatement = null;
		boolean returnResult;
		
		StringBuilder setBuilder = new StringBuilder();
		
		Iterator<String> setIterator = specificRows.keySet().iterator();	
		while (setIterator.hasNext()) {
			String columnName = setIterator.next();
			Column column = tableEntry.getColumn(columnName);
			if (column == null)throw new NoTableColumnException(columnName, tableEntry.getClass().getName());
			
			if (column.columnType() == ColumnType.Primary || column.autoIncrement()) continue;

			if (setBuilder.length() > 0) {
				setBuilder.append(",  `" + column.columnName() + "` = ?");
			} else {
				setBuilder.append("`" + column.columnName() + "` = ?");
			}
		}
		
		int index = 1;
		
		if (filterList != null && filterList.size() > 0) {
			StringBuilder whereBuilder = new StringBuilder();
			
			Iterator<String> whereIterator = filterList.keySet().iterator();
			while (whereIterator.hasNext()) {
				String columnName = whereIterator.next();
				
				Column column = tableEntry.getColumn(columnName);
				if (column == null) throw new NoTableColumnException(columnName, tableEntry.getClass().getName());
				
				if (whereBuilder.length() > 0) {
					whereBuilder.append(" AND `" + columnName + "`");
				} else {
					whereBuilder.append("`" + columnName + "`");
				}
				
				whereBuilder.append(" = ?");
			}
			
			preparedStatement = connection.prepareStatement("UPDATE `" + tableEntry.getTableName() + "` SET " + setBuilder.toString() + " WHERE " + whereBuilder.toString() + ";");	
		
			setIterator = specificRows.keySet().iterator();	
			while (setIterator.hasNext()) {
				String columnName = setIterator.next();
				Column column = tableEntry.getColumn(columnName);
				
				if (column.columnType() == ColumnType.Primary || column.autoIncrement()) continue;
				this.setStatement(index++, preparedStatement, specificRows.get(columnName), column);
			}
			
			whereIterator = filterList.keySet().iterator();
			while (whereIterator.hasNext()) {
				String columnName = whereIterator.next();
				
				Column column = tableEntry.getColumn(columnName);
				Object columnValue = tableEntry.getColumn(column);
				
				this.setStatement(index++, preparedStatement, columnValue, column);
			}
		} else {
			preparedStatement = connection.prepareStatement("UPDATE `" + tableEntry.getTableName() + "` SET " + setBuilder.toString() + ";");
		
			setIterator = specificRows.keySet().iterator();	
			while (setIterator.hasNext()) {
				String columnName = setIterator.next();
				Column column = tableEntry.getColumn(columnName);
				
				if (column.columnType() == ColumnType.Primary || column.autoIncrement()) continue;
				this.setStatement(index++, preparedStatement, specificRows.get(columnName), column);
			}
		}
		
		returnResult = preparedStatement.executeUpdate() > 0;
		this.closeConnection(connection, preparedStatement, null);
		
		return returnResult;
	}
	
	public boolean load(TableEntry tableEntry, Map<String, Object> filterList, LoadHelper loadHelper) throws SQLException, IllegalArgumentException, IllegalAccessException {
		if (tableEntry.isLoadedEntry) return false;
		if (filterList == null) throw new NullPointerException("The FilterList is NULL");
		if (filterList.size() <= 0) throw new EmptyFilterException("filterList");
		
		// Alter LoadHelper
		if (loadHelper == null) loadHelper = new LoadHelper();
		loadHelper.limit(1).offset(0);
		
		Connection connection = sqLiteDatabase.getConnection();
		PreparedStatement preparedStatement = this.prepareSelect(connection, tableEntry, filterList, loadHelper);
		ResultSet resultSet = preparedStatement.executeQuery();
		
		if (resultSet == null || !resultSet.next()) return false;
		
		Map<String, Column> tableLayout = tableEntry.getTableLayout();
		Iterator<Column> columnIterator = tableLayout.values().iterator();
		
		while (columnIterator.hasNext()) {
			Column column = columnIterator.next();
			tableEntry.setColumn(column, this.getResult(resultSet, column));
		}
		
		tableEntry.isLoadedEntry = true;
		tableEntry.inform();
		
		this.closeConnection(connection, preparedStatement, resultSet);
		return true;
	}

	public void load(TableEntry tableEntry, List<TableEntry> resultList, Map<String, Object> filterList, LoadHelper loadHelper) throws IllegalArgumentException, IllegalAccessException, SQLException {
		if (resultList == null) throw new NullPointerException("The resultList is NULL");
		resultList.clear(); // Clear the List - when not empty... not my problem
		
		Connection connection = sqLiteDatabase.getConnection();
		PreparedStatement preparedStatement = this.prepareSelect(connection, tableEntry, filterList, loadHelper);
		ResultSet resultSet = preparedStatement.executeQuery();
		
		if (resultSet == null) return;
		
		List<Column> tableLayout = new ArrayList<Column>(tableEntry.getTableLayout().values());
		
		while (resultSet.next()) {
			TableEntry newInstance = tableEntry.getInstance();
			
			for (Column column : tableLayout) {
				newInstance.setColumn(column, this.getResult(resultSet, column));
			}	
			
			newInstance.isLoadedEntry = true;
			newInstance.inform();
			
			resultList.add(newInstance);
		}
		
		this.closeConnection(connection, preparedStatement, resultSet);
	}
	
	protected PreparedStatement prepareSelect(Connection connection, TableEntry tableEntry, Map<String, Object> filterList, LoadHelper loadHelper) throws SQLException {
		StringBuilder getBuilder = new StringBuilder();
		StringBuilder whereBuilder = new StringBuilder();
		
		Map<String, Column> tableLayout = tableEntry.getTableLayout();
		
		Iterator<String> layoutIterator = tableLayout.keySet().iterator();	
		while (layoutIterator.hasNext()) {			
			if (getBuilder.length() > 0) {
				getBuilder.append(", `" + layoutIterator.next() + "`");
			} else {
				getBuilder.append("`" + layoutIterator.next() + "`");
			}
		}

		if (filterList != null && filterList.size() > 0) {
			Iterator<String> filterIterator = filterList.keySet().iterator();
			while (filterIterator.hasNext()) {
				if (whereBuilder.length() > 0) {
					whereBuilder.append(" AND `" + filterIterator.next() + "`");
				} else {
					whereBuilder.append("`" + filterIterator.next() + "`");
				}
				
				whereBuilder.append(" = ?");
			}
		}
		
		String helper = "";
		if (loadHelper != null) {
			if (loadHelper.columnSorter.size() > 0) {
				helper = " ORDER BY ";
				
				StringBuilder stringBuilder = new StringBuilder();
				for (String field : loadHelper.columnSorter.keySet()) {
					stringBuilder.append(field + " " + loadHelper.columnSorter.get(field) + ", ");
				} 
				
				stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
				
				helper = helper + stringBuilder.toString();
			}

			if (loadHelper.limit > 0) helper = helper + " LIMIT " + loadHelper.limit;
			if (loadHelper.offset > 0) helper = helper + " OFFSET " + loadHelper.offset;
		}
		
		PreparedStatement preparedStatement = connection.prepareStatement("SELECT " + getBuilder.toString() + " FROM `" + tableEntry.getTableName() + "`" + (whereBuilder.length() > 0 ? " WHERE " + whereBuilder.toString() : "") + helper + ";");
		
		if (filterList != null && filterList.size() > 0) {
			int index = 1;
			
			Iterator<String> filterIterator = filterList.keySet().iterator();
			while (filterIterator.hasNext()) {
				String columnName = filterIterator.next();
				
				Column column = tableEntry.getColumn(columnName);
				if (column == null) throw new NoTableColumnException(columnName, tableEntry.getClass().getName());
				Object columnValue = filterList.get(columnName);
				
				this.setStatement(index++, preparedStatement, columnValue, column);
			}
		}
		
		// Execute in the correct method not here
		return preparedStatement;
	}

	public boolean exist(TableEntry tableEntry) throws SQLException, IllegalArgumentException, IllegalAccessException {
		Connection connection = sqLiteDatabase.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		boolean returnResult;
		
		if (tableEntry.hasPrimaryKey()) { // Simple and faster :)
			Column column = tableEntry.getPrimaryKey();
			Object primaryKey = tableEntry.getColumn(column);
			
			preparedStatement = connection.prepareStatement("SELECT * FROM `" + tableEntry.getTableName() + "` WHERE " + column.columnName() + " = ?;");
			this.setStatement(1, preparedStatement, primaryKey, column);
			
			resultSet = preparedStatement.executeQuery();
			returnResult = resultSet.next();
		} else { // Needs to check more - so a little bit slower
			Map<Column, Object> columnList = tableEntry.getEntryColumns();
			Iterator<Column> iterator = columnList.keySet().iterator();
			
			StringBuilder stringBuilder = new StringBuilder();
			
			while (iterator.hasNext()) {
				Column column = iterator.next();
				if (columnList.get(column) == null) continue; // Can't check NULL variables
				
				if (stringBuilder.length() > 0) {
					stringBuilder.append(" AND `" + column.columnName() + "`");
				} else {
					stringBuilder.append("`" + column.columnName() + "`");
				}
				
				stringBuilder.append(" = ?");
			}
			
			preparedStatement = connection.prepareStatement("SELECT * FROM `" + tableEntry.getTableName() + "` WHERE " + stringBuilder.toString() + ";");
			iterator = columnList.keySet().iterator(); // New iterator cause we cant start over again
			
			int index = 1;
			while (iterator.hasNext()) {
				Column column = iterator.next();
				Object value = columnList.get(column);
				if (value == null) continue;
				
				this.setStatement(index++, preparedStatement, value, column);
			}
			
			resultSet = preparedStatement.executeQuery();
			returnResult = resultSet.next();
		}
		
		this.closeConnection(connection, preparedStatement, resultSet);
		return returnResult;
	}
	
	public void updateLayout(TableEntry tableEntry) throws SQLException {
		if (!DatabaseHandler.get().tableExist(tableEntry.getTableName())) {
			this.addTable(tableEntry);
			return;
		}
		
		String tableName = tableEntry.getTableName();
		
		List<String> removeList = new ArrayList<String>();
		Map<String, Column> addList = tableEntry.getTableLayout();		
		List<String> existingList = this.getTableColumns(tableEntry.getTableName());
		
		for (String columnName : existingList) {
			if (!addList.containsKey(columnName)) {
				removeList.add(columnName);
			} else {
				addList.remove(columnName);
			}
		}
		
		List<Column> correctAddList = new ArrayList<Column>(addList.values());
		correctAddList.sort(new TableEntry.ColumnClassSorter());
		
		// Add and remove the columns
		for (String columnName : removeList) this.delColumn(tableName, columnName);
		for (Column addColumn : correctAddList) this.addColumn(tableEntry, addColumn);
	}
	
	protected void addTable(TableEntry tableEntry) throws SQLException { 
		Connection connection = sqLiteDatabase.getConnection();
		PreparedStatement preparedStatement = null;
		
		StringBuilder columnBuilder = new StringBuilder();
		List<Column> columnList = tableEntry.getPlainLayout();
		Iterator<Column> iterator = columnList.iterator();
		
		while (iterator.hasNext()) {
			Column column = iterator.next();
			
			columnBuilder.append("`" + column.columnName() + "` ");
			columnBuilder.append(column.autoIncrement() || column.columnType() == ColumnType.Primary ? "INTEGER" : this.toDatabaseType(column.entryType() != EntryType.Normal ? DataType.String : column.dataType()));
			if (column.columnType() != ColumnType.Normal) columnBuilder.append(column.columnType() == ColumnType.Primary ? " PRIMARY KEY AUTOINCREMENT" : " UNIQUE");
			if (iterator.hasNext()) columnBuilder.append(", ");
		}
		
		preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `" + tableEntry.getTableName() + "` (" + columnBuilder.toString() + ");");
		preparedStatement.execute();
		
		this.closeConnection(connection, preparedStatement, null);
	}
	
	protected List<String> getTableColumns(String tableName) throws SQLException {
		Connection connection = sqLiteDatabase.getConnection();
		
		ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName, null);
		List<String> columnList = new ArrayList<String>();
		
		while (resultSet.next()) columnList.add(resultSet.getString("COLUMN_NAME"));
		
		this.closeConnection(connection, null, resultSet);
		return columnList;
	}

	protected void addColumn(TableEntry tableEntry, Column column) throws SQLException {
		Connection connection = DatabaseHandler.getConnection();
		DataType dataType = column.entryType() != EntryType.Normal ? DataType.String : column.dataType();
		
		List<Column> layout = tableEntry.getPlainLayout();
		
		String whereToAdd = column.order() == -1 ? "" : 
			column.order() == 1 && layout.size() > 1 ? " BEFORE `" + tableEntry.getColumn(column.order() + 1).columnName() + "`" : 
				column.order() > 1 && layout.size() >= column.order() ? " AFTER `" + tableEntry.getColumn(column.order() - 1).columnName() + "`" : ""; 

				
		PreparedStatement preparedStatement = connection.prepareStatement("ALTER TABLE `" + tableEntry.getTableName() + "` ADD `" + column.columnName() + "` " + this.toDatabaseType(dataType) + (column.columnType() == ColumnType.Unique ? " UNIQUE" : "") + whereToAdd + ";");
		preparedStatement.execute();
		
		this.closeConnection(connection, preparedStatement, null);
	}

	protected void delColumn(String tableName, String columnName) throws SQLException {
		Connection connection = sqLiteDatabase.getConnection();
		
		PreparedStatement preparedStatement = connection.prepareStatement("ALTER TABLE `" + tableName + "` DROP `" + columnName + "`;");
		preparedStatement.execute();
		
		this.closeConnection(connection, preparedStatement, null);
	}
	
	protected String toDatabaseType(DataType dataType) {
		switch (dataType) {
		case Boolean:
			return "BOOLEAN";
		case Byte:
			return "INT";
		case Double:
			return "DOUBLE";
		case Float:
			return "DOUBLE";
		case Integer:
			return "INT";
		case Long:
			return "BIGINT";
		case Short:
			return "INT";
		case String:
			return "TEXT";
		case Timestamp:
			return "TIMESTAMP";
		case Location:
			return "TEXT";
		case Binary:
			return "BLOB";
		case UniqueId:
			return "TEXT";
		}
		
		return null;
	}
}