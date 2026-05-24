package top.huliawsl.blockwright.util;

public final class ValidationIssue {
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    private final Severity severity;
    private final String location;
    private final String message;

    public ValidationIssue(Severity severity, String location, String message) {
        this.severity = severity;
        this.location = location;
        this.message = message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getLocation() {
        return location;
    }

    public String getMessage() {
        return message;
    }
}
