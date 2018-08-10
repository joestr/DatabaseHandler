package net.dertod2.DatabaseHandler.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Location;

import net.dertod2.DatabaseHandler.Exceptions.ByteArrayUnsupportedException;
import net.dertod2.DatabaseHandler.Table.Column.EntryType;
import net.dertod2.DatabaseHandler.Utils.ConverterUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public abstract class EntryHandler {
    private final String LIST_REGEX = "\u00B6";
    private final String MAP_REGEX = "\n";
    private final String NULL_STRING = "NULL";
	
	/**
	 * Inserts the TableEntry into the database when not already inserted
	 * @param tableEntry
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public abstract void insert(TableEntry tableEntry) throws IllegalArgumentException, IllegalAccessException, SQLException;
	
	public abstract void insert(List<TableEntry> entryList) throws IllegalArgumentException, IllegalAccessException, SQLException;
	
	/**
	 * Removes the entry with the similiar primary key in the database<br />
	 * <b>WARNING</b>: When no primary Key set and not loaded out of database this method will fail
	 * @param tableEntry
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public void remove(TableEntry tableEntry) throws IllegalArgumentException, IllegalAccessException, SQLException {
		if (!tableEntry.isLoadedEntry || !tableEntry.hasPrimaryKey()) return;
		this.remove(tableEntry, ImmutableMap.<String, Object>builder().put(tableEntry.getPrimaryKey().columnName(), tableEntry.getColumn(tableEntry.getPrimaryKey())).build());
	}
	
	/**
	 * Removes all entrys out of the database table which matches the filterList<br />
	 * <b>WARNING</b>:When the filterList is empty or NULL ALL entrys in the table will be removed
	 * @param tableEntry
	 * @param filterList
	 * @throws SQLException 
	 */
	public abstract void remove(TableEntry tableEntry, Map<String, Object> filterList) throws SQLException;
	
	/**
	 * Updates the entry in the table<br />
	 * Works <b>only</b> if an Primary Key is set
	 * @param tableEntry
	 * @return true when an entry was updated or false when not loaded entrySet or no row was updated
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws SQLException 
	 */
	public boolean update(TableEntry tableEntry) throws IllegalArgumentException, IllegalAccessException, SQLException {
		if (!tableEntry.isLoadedEntry || !tableEntry.hasPrimaryKey()) return false;
		return this.update(tableEntry, ImmutableMap.<String, Object>builder().put(tableEntry.getPrimaryKey().columnName(), tableEntry.getColumn(tableEntry.getPrimaryKey())).build());

	}
		
	/**
	 * Updates all entries matches the filterList or all entries when the filterList is empty or NULL
	 * @param tableEntry
	 * @param filterList
	 * @return true when at least one row was updated or false when no row was updated or the tableEntry is not loaded out of the database
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public boolean update(TableEntry tableEntry, Map<String, Object> filterList) throws SQLException, IllegalArgumentException, IllegalAccessException {
		return this.update(tableEntry, filterList, ImmutableList.<String>of());
	}
	
	/**
	 * Updates all entries matches the filterList or all entries when the filterList is empty or NULL<br />
	 * Only changes the rows specified with specificRows and ignores all other rows<br />
	 * Uses the new data out of the spicificRows List
	 * @param tableEntry
	 * @param filterList
	 * @param specificRows
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws SQLException 
	 */
	public abstract boolean update(TableEntry tableEntry, Map<String, Object> filterList, List<String> specificRows) throws SQLException, IllegalArgumentException, IllegalAccessException;
	
	/**
	 * Updates all entries matches the filterList or all entries when the filterList is empty or NULL<br />
	 * Only changes the rows specified with specificRows and ignores all other rows<br />
	 * Uses the new data out of the specificRows Map
	 * @param tableEntry
	 * @param filterList
	 * @param specificRows
	 * @throws Exception 
	 */
	public abstract boolean update(TableEntry tableEntry, Map<String, Object> filterList, Map<String, Object> specificRows) throws Exception;
	
	/**
	 * Loads the result out of the database that matches the filterList element
	 * @param tableEntry
	 * @param filterList
	 * @return
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public boolean load(TableEntry tableEntry, Map<String, Object> filterList) throws SQLException, IllegalArgumentException, IllegalAccessException {
		return this.load(tableEntry, filterList, null);
	}
	
	public abstract boolean load(TableEntry tableEntry, Map<String, Object> filterList, LoadHelper loadHelper) throws SQLException, IllegalArgumentException, IllegalAccessException;
	
	public void load(TableEntry tableEntry, List<TableEntry> resultList) throws IllegalArgumentException, IllegalAccessException, SQLException {
		this.load(tableEntry, resultList, null, new LoadHelper());
	}
	
	public void load(TableEntry tableEntry, List<TableEntry> resultList, LoadHelper loadHelper) throws IllegalArgumentException, IllegalAccessException, SQLException {
		this.load(tableEntry, resultList, null, loadHelper);
	}
	
	/**
	 * Loads all matching entries out of the database
	 * @param tableEntry
	 * @param resultList
	 * @param filterList
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public void load(TableEntry tableEntry, List<TableEntry> resultList, Map<String, Object> filterList) throws IllegalArgumentException, IllegalAccessException, SQLException {
		this.load(tableEntry, resultList, filterList, null);
	}
	
	public abstract void load(TableEntry tableEntry, List<TableEntry> resultList, Map<String, Object> filterList, LoadHelper loadHelper) throws IllegalArgumentException, IllegalAccessException, SQLException;
	
	
	protected abstract PreparedStatement prepareSelect(Connection connection, TableEntry tableEntry, Map<String, Object> filterList, LoadHelper loadHelper) throws SQLException;
	
	/**
	 * Check if this entry exists in the database table
	 * @param tableEntry
	 * @return
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public abstract boolean exist(TableEntry tableEntry) throws SQLException, IllegalArgumentException, IllegalAccessException;
	
	public abstract void updateLayout(TableEntry tableEntry) throws SQLException;
	
	protected abstract void addTable(TableEntry tableEntry) throws SQLException;
	
	protected abstract void addColumn(TableEntry tableEntry, Column column) throws SQLException;
	
	protected abstract void delColumn(String tableName, String columnName) throws SQLException;
	
	protected abstract String toDatabaseType(Column.DataType dataType);
	
	protected abstract List<String> getTableColumns(String tableName) throws SQLException;
	
	protected void setStatement(int index, PreparedStatement preparedStatement, Object value, Column column) throws SQLException {
		switch (column.entryType()) {
			case List:
				preparedStatement.setString(index, setList((List<?>) value));
				
				break;
			case Map:
				preparedStatement.setString(index, setMap((Map<?, ?>) value));
				
				break;
			case Normal:
				switch (column.dataType()) {
					case Boolean:
						preparedStatement.setBoolean(index, (Boolean) value);
						break;
					case Byte:
						preparedStatement.setByte(index, (Byte) value);
						break;
					case Double:
						preparedStatement.setDouble(index, (Double) value);
						break;
					case Float:
						preparedStatement.setFloat(index, (Float) value);
						break;
					case Integer:
						preparedStatement.setInt(index, (Integer) value);
						break;
					case Long:
						preparedStatement.setLong(index, (Long) value);
						break;
					case Short:
						preparedStatement.setShort(index, (Short) value);
						break;
					case String:
						preparedStatement.setString(index, (String) value);
						break;
					case Timestamp:
						preparedStatement.setTimestamp(index, (Timestamp) value);
						break;
					case Location:
						preparedStatement.setString(index, ConverterUtils.toString(((Location) value)));
						break;
					case Binary:
						preparedStatement.setBytes(index, (byte[]) value);
						break;
					case UniqueId:
						preparedStatement.setString(index, ((UUID) value).toString());
						break;
				}
				
			break;
		}
	}
	
	protected Object getResult(ResultSet resultSet, Column column) throws SQLException {
		switch (column.entryType()) {
			case List:
				return this.getList(column.dataType(), resultSet.getString(column.columnName()));
			case Map:
				return this.getMap(column.dataType(), column.entryDataType(), resultSet.getString(column.columnName()));
			case Normal:
				switch (column.dataType()) {
					case Boolean:
						return resultSet.getBoolean(column.columnName());
					case Byte:
						return resultSet.getByte(column.columnName());
					case Double:
						return resultSet.getDouble(column.columnName());
					case Float:
						return resultSet.getFloat(column.columnName());
					case Integer:
						return resultSet.getInt(column.columnName());
					case Long:
						return resultSet.getLong(column.columnName());
					case Short:
						return resultSet.getShort(column.columnName());
					case String:
						return resultSet.getString(column.columnName());
					case Timestamp:
						return resultSet.getTimestamp(column.columnName());
					case Location:
						return ConverterUtils.toLocation(resultSet.getString(column.columnName()));
					case Binary: 
						return resultSet.getBytes(column.columnName());
					case UniqueId:
						return UUID.fromString(resultSet.getString(column.columnName()));
				}
		}
		
		return null;
	}
	
    protected List<?> getList(Column.DataType dataType, String field) {
        List<Object> list = new ArrayList<Object>();
        if (field == null || field.length() <= 0) {
            return list;
        }
        switch (dataType) {
            case Byte:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(Byte.valueOf(s));
                }
                break;
            case Integer:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(Integer.valueOf(s));
                }
                break;
            case Float:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(Float.valueOf(s));
                }
                break;
            case Double:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(Double.valueOf(s));
                }
                break;
            case Long:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(Long.valueOf(s));
                }
                break;
            case Short:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(Short.valueOf(s));
                }
                break;
            case String:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(s);
                }
                break;
            case Boolean:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(Boolean.valueOf(s));
                }
                break;
    		case Timestamp:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(Timestamp.valueOf(s));
                }
                break;  
    		case Location:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(ConverterUtils.toLocation(s));
                }
                break;  
    		case Binary:
                throw new ByteArrayUnsupportedException(EntryType.List);
    		case UniqueId:
                for (String s : field.split(this.LIST_REGEX)) {
                    if (s.equals(NULL_STRING)) {
                        list.add(null);
                        continue;
                    }
                    list.add(UUID.fromString(s));
                }
                break;  
        }
        
        return list;
    }
    
    protected Map<?, ?> getMap(Column.DataType dataType, Column.DataType entryType, String field) {
    	Map<Object, Object> map = new HashMap<Object, Object>();
        if (field == null || field.length() <= 0) {
            return map;
        }
    	
    	for (String s : field.split(this.MAP_REGEX)) {
    		Object finalKey = null;
    		Object finalValue = null;
    		
    		String key = s.split(this.LIST_REGEX)[0];
    		switch (entryType) {
			case Boolean:
				finalKey = Boolean.valueOf(key);
				break;
			case Byte:
				finalKey = Byte.valueOf(key);
				break;
			case Double:
				finalKey = Double.valueOf(key);
				break;
			case Float:
				finalKey = Float.valueOf(key);
				break;
			case Integer:
				finalKey = Integer.valueOf(key);
				break;
			case Long:
				finalKey = Long.valueOf(key);
				break;
			case Short:
				finalKey = Short.valueOf(key);
				break;
			case String:
				finalKey = String.valueOf(key);
				break;
			case Timestamp:
				finalKey = Timestamp.valueOf(key);
				break;
			case Location:
				finalKey = ConverterUtils.toLocation(key);
				break;
			case Binary:
				throw new ByteArrayUnsupportedException(EntryType.Map);
			case UniqueId:
				finalKey = UUID.fromString(key);
    		}
    		
    		String value = s.split(this.LIST_REGEX)[1];
    		switch (dataType) {
			case Boolean:
				finalValue = Boolean.valueOf(value);
				break;
			case Byte:
				finalValue = Byte.valueOf(value);
				break;
			case Double:
				finalValue = Double.valueOf(value);
				break;
			case Float:
				finalValue = Float.valueOf(value);
				break;
			case Integer:
				finalValue = Integer.valueOf(value);
				break;
			case Long:
				finalValue = Long.valueOf(value);
				break;
			case Short:
				finalValue = Short.valueOf(value);
				break;
			case String:
				finalValue = String.valueOf(value);
				break;
			case Timestamp:
				finalValue = Timestamp.valueOf(value);
				break;
			case Location:
				finalValue = ConverterUtils.toLocation(value);
				break;
			case Binary:
				throw new ByteArrayUnsupportedException(EntryType.Map);
			case UniqueId:
				finalValue = UUID.fromString(value);
    		}
    		
    		map.put(finalKey, finalValue);
    	}	
    	
    	return map;
    }
	
    protected String setList(List<?> list) {
        if (list == null) return NULL_STRING;
        
        StringBuilder stringBuilder = new StringBuilder();
        for (Object object : list) {
            if (object == null) {
            	stringBuilder.append(NULL_STRING);
            } else if (object instanceof Location) {
            	stringBuilder.append(ConverterUtils.toString((Location) object));
            } else {
            	stringBuilder.append(String.valueOf(object));
            }
            
            stringBuilder.append(this.LIST_REGEX);
        }
        
        if (stringBuilder.length() > 0) stringBuilder.deleteCharAt(stringBuilder.length() - this.LIST_REGEX.length());
        
        return stringBuilder.toString();
    }
    
    protected String setMap(Map<?, ?> map) {
    	if (map == null) return NULL_STRING;
    	 
    	StringBuilder stringBuilder = new StringBuilder();
    	for (Entry<?, ?> entry : map.entrySet()) {
    		stringBuilder.append(
    				(entry.getKey() instanceof Location ? ConverterUtils.toString((Location) entry.getKey()) : String.valueOf(entry.getKey()))
    						+ this.LIST_REGEX + 
    				(entry.getValue() instanceof Location ? ConverterUtils.toString((Location) entry.getValue()) : String.valueOf(entry.getValue())));
    		
    		stringBuilder.append(this.MAP_REGEX);
    	}
    	
    	if (stringBuilder.length() > 0) stringBuilder.deleteCharAt(stringBuilder.length() - this.MAP_REGEX.length());
    	
    	return stringBuilder.toString();
    }
    
    public void closeConnection(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) {
    	try { if (resultSet != null) resultSet.close(); } catch (SQLException exc) { }
    	try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException exc) { }
    	try { if (connection != null) connection.close(); } catch (SQLException exc) { }
    }
}