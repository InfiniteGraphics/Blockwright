package top.huliawsl.blockwright.client;

public final class PcgEditorLogEntry {
    public enum Severity {
        INFO,
        SUCCESS,
        WARNING,
        ERROR,
        DEBUG
    }

    private final Severity severity;
    private final String message;
    private final long timestamp;

    public PcgEditorLogEntry(Severity severity, String message, long timestamp) {
        this.severity = severity;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
