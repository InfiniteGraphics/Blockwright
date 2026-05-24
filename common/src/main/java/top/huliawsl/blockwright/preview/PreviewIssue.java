package top.huliawsl.blockwright.preview;

public final class PreviewIssue {
    private final PreviewSeverity severity;
    private final String message;

    public PreviewIssue(PreviewSeverity severity, String message) {
        this.severity = severity;
        this.message = message;
    }

    public PreviewSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }
}
