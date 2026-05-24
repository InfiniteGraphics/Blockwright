package top.huliawsl.blockwright.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationReport {
    private final List<ValidationIssue> issues = new ArrayList<>();

    public void info(String location, String message) {
        issues.add(new ValidationIssue(ValidationIssue.Severity.INFO, location, message));
    }

    public void warning(String location, String message) {
        issues.add(new ValidationIssue(ValidationIssue.Severity.WARNING, location, message));
    }

    public void error(String location, String message) {
        issues.add(new ValidationIssue(ValidationIssue.Severity.ERROR, location, message));
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.getSeverity() == ValidationIssue.Severity.ERROR);
    }

    public List<ValidationIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }
}
