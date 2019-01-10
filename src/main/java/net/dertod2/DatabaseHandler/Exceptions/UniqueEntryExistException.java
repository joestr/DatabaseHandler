package net.dertod2.DatabaseHandler.Exceptions;

public class UniqueEntryExistException extends RuntimeException {
    private static final long serialVersionUID = 5639979434567450015L;

    private String tableName;

    private String uniqueColumn;
    private String uniqueValue;

    public UniqueEntryExistException(String tableName, String uniqueColumn, String uniqueValue) {
        this.tableName = tableName;

        this.uniqueColumn = uniqueColumn;
        this.uniqueValue = uniqueValue;
    }

    public String getMessage() {
        return "The Table '" + this.tableName + "' already contains the value '" + this.uniqueValue
                + "' for the unique column '" + this.uniqueColumn + "'!";
    }
}