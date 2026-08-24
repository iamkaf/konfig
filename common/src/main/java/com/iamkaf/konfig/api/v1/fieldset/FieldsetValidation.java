//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable validation result for one fieldset value or entry.
 */
@ApiStatus.Experimental
public final class FieldsetValidation {
    private static final FieldsetValidation VALID = new FieldsetValidation(Collections.emptyList());

    private final List<FieldsetValidationIssue> issues;

    private FieldsetValidation(List<FieldsetValidationIssue> issues) {
        this.issues = Collections.unmodifiableList(new ArrayList<FieldsetValidationIssue>(issues));
    }

    public static FieldsetValidation validResult() {
        return VALID;
    }

    public static FieldsetValidation of(List<FieldsetValidationIssue> issues) {
        return issues == null || issues.isEmpty() ? VALID : new FieldsetValidation(issues);
    }

    public boolean valid() {
        return this.issues.isEmpty();
    }

    public List<FieldsetValidationIssue> issues() {
        return this.issues;
    }
}
//?}
