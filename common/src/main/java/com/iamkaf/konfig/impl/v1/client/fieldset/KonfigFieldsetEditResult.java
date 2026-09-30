package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.text;

import net.minecraft.network.chat.Component;

import java.util.Objects;

@ApiStatus.Internal
public record KonfigFieldsetEditResult(Status status, Component message) {
    public KonfigFieldsetEditResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(message, "message");
    }

    public static KonfigFieldsetEditResult applied() {
        return new KonfigFieldsetEditResult(Status.APPLIED, text(""));
    }

    public static KonfigFieldsetEditResult noChange() {
        return new KonfigFieldsetEditResult(Status.NO_CHANGE, text(""));
    }

    public static KonfigFieldsetEditResult pending() {
        return new KonfigFieldsetEditResult(Status.PENDING, text("Saving..."));
    }

    public static KonfigFieldsetEditResult readOnly(Component reason) {
        Component message = reason.getString().isBlank()
                ? text("This fieldset is read-only.")
                : reason;
        return new KonfigFieldsetEditResult(Status.READ_ONLY, message);
    }

    public static KonfigFieldsetEditResult permissionDenied() {
        return new KonfigFieldsetEditResult(
                Status.PERMISSION_DENIED,
                text("The server refused this edit because you do not have permission.")
        );
    }

    public static KonfigFieldsetEditResult staleRevision() {
        return new KonfigFieldsetEditResult(
                Status.STALE_REVISION,
                text("This config changed on the server. Reopen it and try again.")
        );
    }

    public static KonfigFieldsetEditResult malformedSubmission() {
        return new KonfigFieldsetEditResult(
                Status.MALFORMED_SUBMISSION,
                text("The server rejected this edit because the submitted data was malformed.")
        );
    }

    public static KonfigFieldsetEditResult invalid(Component message) {
        Component resolved = message.getString().isBlank()
                ? text("The server rejected this edit because one or more values are invalid.")
                : message;
        return new KonfigFieldsetEditResult(Status.INVALID, resolved);
    }

    public static KonfigFieldsetEditResult unsupported() {
        return new KonfigFieldsetEditResult(
                Status.UNSUPPORTED,
                text("This server does not support remote editing for this config.")
        );
    }

    public boolean accepted() {
        return this.status == Status.APPLIED || this.status == Status.NO_CHANGE;
    }

    public boolean submitted() {
        return accepted() || this.status == Status.PENDING;
    }

    public boolean changed() {
        return this.status == Status.APPLIED;
    }

    public enum Status {
        APPLIED,
        NO_CHANGE,
        PENDING,
        READ_ONLY,
        PERMISSION_DENIED,
        STALE_REVISION,
        MALFORMED_SUBMISSION,
        INVALID,
        UNSUPPORTED
    }
}
