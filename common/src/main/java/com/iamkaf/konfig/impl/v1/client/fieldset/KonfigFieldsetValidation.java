//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public final class KonfigFieldsetValidation {
    private static final KonfigFieldsetValidation VALID = new KonfigFieldsetValidation(List.of());

    private final List<Issue> issues;

    public KonfigFieldsetValidation(List<Issue> issues) {
        this.issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public static KonfigFieldsetValidation valid() {
        return VALID;
    }

    public List<Issue> issues() {
        return this.issues;
    }

    public boolean isValid() {
        return this.errorCount() == 0;
    }

    public int errorCount() {
        return this.count(Severity.ERROR);
    }

    public int warningCount() {
        return this.count(Severity.WARNING);
    }

    public KonfigFieldsetValidation forEntry(String entryId) {
        return this.filtered(entryId, null);
    }

    public KonfigFieldsetValidation forField(String entryId, String fieldPath) {
        return this.filtered(entryId, fieldPath);
    }

    public Component summary() {
        int errors = this.errorCount();
        int warnings = this.warningCount();
        if (errors > 0 && warnings > 0) {
            return Component.literal(errors + plural(errors, " error", " errors")
                    + ", " + warnings + plural(warnings, " warning", " warnings"));
        }
        if (errors > 0) {
            return Component.literal(errors + plural(errors, " error", " errors"));
        }
        if (warnings > 0) {
            return Component.literal(warnings + plural(warnings, " warning", " warnings"));
        }
        return Component.empty();
    }

    private int count(Severity severity) {
        int count = 0;
        for (Issue issue : this.issues) {
            if (issue.severity() == severity) {
                count++;
            }
        }
        return count;
    }

    private KonfigFieldsetValidation filtered(String entryId, String fieldPath) {
        List<Issue> matches = new ArrayList<>();
        for (Issue issue : this.issues) {
            if (!issue.entryId().equals(entryId)) {
                continue;
            }
            if (fieldPath != null && !issue.fieldPath().equals(fieldPath)) {
                continue;
            }
            matches.add(issue);
        }
        return matches.isEmpty() ? valid() : new KonfigFieldsetValidation(matches);
    }

    private static String plural(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    public enum Severity {
        ERROR,
        WARNING
    }

    public record Issue(Severity severity, String entryId, String fieldPath, Component message) {
        public Issue {
            Objects.requireNonNull(severity, "severity");
            entryId = entryId == null ? "" : entryId;
            fieldPath = fieldPath == null ? "" : fieldPath;
            Objects.requireNonNull(message, "message");
        }

        public static Issue fieldError(String entryId, String fieldPath, Component message) {
            return new Issue(Severity.ERROR, entryId, fieldPath, message);
        }

        public static Issue entryError(String entryId, Component message) {
            return new Issue(Severity.ERROR, entryId, "", message);
        }

        public static Issue fieldWarning(String entryId, String fieldPath, Component message) {
            return new Issue(Severity.WARNING, entryId, fieldPath, message);
        }
    }
}
//?}
