package net.dertod2.DatabaseHandler.Exceptions;

public class UnhandledDataTypeException extends RuntimeException {
    private static final long serialVersionUID = -208305598190317378L;

    private Class<?> dataType;

    public UnhandledDataTypeException(Class<?> dataType) {
        this.dataType = dataType;
    }

    public String getMessage() {
        return "The DataType " + dataType.getName() + " can not be handled by the Database driver";
    }
}