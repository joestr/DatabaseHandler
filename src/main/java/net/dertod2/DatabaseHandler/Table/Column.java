package net.dertod2.DatabaseHandler.Table;

import net.dertod2.DatabaseHandler.Exceptions.UnhandledDataTypeException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Timestamp;
import java.util.UUID;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    public enum DataType {
    	/**
    	 * The primitive <b>boolean</b> or Object {@link Boolean} Class
    	 */
    	Boolean(Boolean.class, boolean.class),
    	/**
    	 * The primitive <b>byte</b> or Object {@link Byte} Class
    	 */
    	Byte(Byte.class, byte.class), 
    	/**
    	 * The primitive <b>short</b> or Object {@link Short} Class
    	 */
    	Short(Short.class, short.class),
    	/**
    	 * The primitive <b>int</b> or Object {@link Integer} Class
    	 */
    	Integer(Integer.class, int.class),
    	/**
    	 * The primitive <b>double</b> or Object {@link Double} Class
    	 */
    	Double(Double.class, double.class),
    	/**
    	 * The primitive <b>float</b> or Object {@link Float} Class
    	 */
    	Float(Float.class, float.class), 
    	/**
    	 * The primitive <b>long</b> or Object {@link Long} Class
    	 */ 	 
    	Long(Long.class, long.class),
    	
    	
    	/**
    	 * The Object {@link Timestamp} Class
    	 */ 
    	Timestamp(Timestamp.class, null),
    	/**
    	 * The Object {@link String} Class
    	 */ 
    	String(String.class, null), 
    	/**
    	 * The Object {@link org.bukkit.Location} Class
    	 */ 
    	Location(org.bukkit.Location.class, null),
    	/**
    	 * The Object {@link UUID} Class
    	 */
    	UniqueId(UUID.class, null),
    	/**
    	 * Binary Data as of java Type <b>byte[]</b>. Saved in database mysql as <b>BLOB</b> and postgres as <b>bytea</b>
    	 */
    	Binary(null, null);

        private Class<?> cls;
        private Class<?> primCls;

        DataType(Class<?> cls, Class<?> primCls) {
            this.cls = cls;
            this.primCls = primCls;
        }

        public boolean isAssignable(Class<?> cls) {
            return this.cls.isAssignableFrom(cls);
        }

        public static DataType fromString(String in) {
            for (DataType dataType : DataType.values()) {
                if (in.equalsIgnoreCase(dataType.name())) {
                    return dataType;
                }
            }
            
            return DataType.String;
        }
        
        public static DataType byClass(Class<?> type) {
            for (DataType dataType : DataType.values()) {
                if (type.isAssignableFrom(dataType.cls) || (dataType.primCls != null && type.isAssignableFrom(dataType.primCls))) {
                    return dataType;
                }
            }
        	
        	throw new UnhandledDataTypeException(type);
        }
        
        public static DataType byInstance(Object object) {
            for (DataType dataType : DataType.values()) {
                if (dataType.cls.isInstance(object)) {
                    return dataType;
                }
            }
        	
            throw new UnhandledDataTypeException(object.getClass());
        }
        

        public Class<?> getTypeClass() {
            return cls;
        }
    }
    
    public enum ColumnType {
    	Normal,
    	Unique,
    	Primary;
    }
    
    public enum EntryType {
    	Normal,
    	List,
    	Map;
    }
	
	/**
	 * The Name of the Column in the database<br />
	 */
	String columnName();
	
	/**
	 * The Type of the column like Integer or Boolean<br />
	 */
	DataType dataType();
	
	/**
	 * Sets this column to an Primary Key or Unique Key< br/>
	 * When left empty this will be an normal column
	 */
	ColumnType columnType() default ColumnType.Normal;
	
	/**
	 * Should the database automatically increment this variable when an new element is inserted in the database
	 */
	boolean autoIncrement() default false;
	
	/**
	 * Defines the Type of this data Object
	 */
	EntryType entryType() default EntryType.Normal;
	
	/**
	 * When using Maps this field defines the Value Data Type
	 */
	DataType entryDataType() default DataType.String;
	
	/**
	 * Allows to order the columns so the database driver creates an fancy table without random order<br />
	 * At least the order must begin with 1.
	 * @return
	 */
	int order() default -1;
}