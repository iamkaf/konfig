//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public record ConfigValidation(List<Issue> issues) {
    public ConfigValidation {
        issues = List.copyOf(issues);
    }

    public static ConfigValidation valid() {
        return new ConfigValidation(List.of());
    }

    public static ConfigValidation error(String path, String code, String message) {
        return of(new Issue(path, code, Severity.ERROR, message, ""));
    }

    public static ConfigValidation warning(String path, String code, String message) {
        return of(new Issue(path, code, Severity.WARNING, message, ""));
    }

    public static ConfigValidation note(String path, String code, String message) {
        return of(new Issue(path, code, Severity.NOTE, message, ""));
    }

    public static ConfigValidation of(Issue issue) {
        return new ConfigValidation(List.of(issue));
    }

    public static ConfigValidation combine(Collection<ConfigValidation> validations) {
        var issues = new ArrayList<Issue>();
        for (ConfigValidation validation : validations) {
            issues.addAll(validation.issues());
        }
        return new ConfigValidation(issues);
    }

    public boolean hasErrors() {
        return this.issues.stream().anyMatch(issue -> issue.severity() == Severity.ERROR);
    }

    public boolean hasWarnings() {
        return this.issues.stream().anyMatch(issue -> issue.severity() == Severity.WARNING);
    }

    public enum Severity {
        ERROR,
        WARNING,
        NOTE
    }

    public record Issue(String path, String code, Severity severity, String message, String translationKey) {
        public Issue {
            path = requireText(path, "path");
            code = requireText(code, "code");
            severity = Objects.requireNonNull(severity, "severity");
            message = requireText(message, "message");
            translationKey = translationKey == null ? "" : translationKey.trim();
        }

        public Issue withTranslationKey(String key) {
            return new Issue(this.path, this.code, this.severity, this.message, key);
        }
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }
}
//?}
