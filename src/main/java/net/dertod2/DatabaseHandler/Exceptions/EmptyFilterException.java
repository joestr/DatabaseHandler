package net.dertod2.DatabaseHandler.Exceptions;

public class EmptyFilterException extends RuntimeException {
    private static final long serialVersionUID = 1456553168991152329L;

    String variableName;

    public EmptyFilterException(String variableName) {
        this.variableName = variableName;
    }

    public String getMessage() {
        return "The Filter '" + this.variableName + "' is empty!";
    }
}