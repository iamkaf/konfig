//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.value.ValueParseResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@ApiStatus.Internal
public final class ConfigSession implements AutoCloseable {
    private final String configId;
    private final Map<String, Slot<?>> fields;
    private final ConfigSessionCommitter committer;
    private final List<ConfigSessionObserver> observers = new CopyOnWriteArrayList<>();
    private long revision;
    private long pendingRequestId = -1L;
    private List<String> pendingFields = List.of();
    private boolean closed;

    public ConfigSession(
            String configId,
            long revision,
            List<ConfigSessionField<?>> fields,
            ConfigSessionCommitter committer
    ) {
        this.configId = requireText(configId, "configId");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        this.revision = revision;
        this.fields = resolveFields(fields);
        this.committer = Objects.requireNonNull(committer, "committer");
    }

    public static ConfigSession local(
            String configId,
            long revision,
            List<ConfigSessionField<?>> fields,
            Runnable save
    ) {
        Objects.requireNonNull(save, "save");
        var resolvedFields = List.copyOf(fields);
        ConfigSessionCommitter committer = request -> commitLocally(request, resolvedFields, save);
        return new ConfigSession(configId, revision, resolvedFields, committer);
    }

    public synchronized String configId() {
        return this.configId;
    }

    public synchronized long revision() {
        return this.revision;
    }

    public synchronized ConfigFieldState<?> field(String fieldId) {
        return requireField(fieldId).snapshot();
    }

    public synchronized ConfigValidation validateDraft(String fieldId, Object input) {
        return requireField(fieldId).validate(input);
    }

    public synchronized ConfigSessionSnapshot snapshot() {
        var states = new ArrayList<ConfigFieldState<?>>();
        this.fields.values().forEach(field -> states.add(field.snapshot()));
        var validations = states.stream().map(ConfigFieldState::validation).toList();
        boolean dirty = states.stream().anyMatch(ConfigFieldState::dirty);
        return new ConfigSessionSnapshot(
                this.configId,
                this.revision,
                states,
                dirty,
                ConfigValidation.combine(validations),
                this.pendingRequestId,
                this.closed
        );
    }

    public ConfigChangeResult mutate(ConfigMutation mutation) {
        Objects.requireNonNull(mutation, "mutation");
        ConfigSessionObserver.Kind kind;
        ConfigChangeResult result;
        synchronized (this) {
            if (this.closed) {
                result = result(ConfigChangeResult.Status.REJECTED_CLOSED, List.of(), ConfigValidation.valid(), "Session is closed");
                kind = ConfigSessionObserver.Kind.REJECTED;
            } else if (this.pendingRequestId >= 0L) {
                result = result(ConfigChangeResult.Status.PENDING, this.pendingFields, ConfigValidation.valid(), "A config apply is pending");
                kind = ConfigSessionObserver.Kind.APPLY_PENDING;
            } else if (mutation instanceof ConfigMutation.SetDraft setDraft) {
                result = setDraft(setDraft);
                kind = result.accepted() ? ConfigSessionObserver.Kind.DRAFT_CHANGED : ConfigSessionObserver.Kind.REJECTED;
            } else if (mutation instanceof ConfigMutation.ResetField resetField) {
                result = resetField(resetField.fieldId());
                kind = result.accepted() ? ConfigSessionObserver.Kind.RESET : ConfigSessionObserver.Kind.REJECTED;
            } else if (mutation instanceof ConfigMutation.ResetAll) {
                result = resetAll();
                kind = result.accepted() ? ConfigSessionObserver.Kind.RESET : ConfigSessionObserver.Kind.REJECTED;
            } else if (mutation instanceof ConfigMutation.RestoreField restoreField) {
                result = restoreField(restoreField.fieldId());
                kind = result.accepted() ? ConfigSessionObserver.Kind.RESET : ConfigSessionObserver.Kind.REJECTED;
            } else if (mutation instanceof ConfigMutation.RestoreAll) {
                result = restoreAll();
                kind = result.accepted() ? ConfigSessionObserver.Kind.RESET : ConfigSessionObserver.Kind.REJECTED;
            } else if (mutation instanceof ConfigMutation.Rollback) {
                result = rollback();
                kind = ConfigSessionObserver.Kind.ROLLED_BACK;
            } else {
                throw new IllegalArgumentException("Unsupported config mutation " + mutation.getClass().getName());
            }
        }
        notifyObservers(kind, result);
        return result;
    }

    public ConfigChangeResult apply(long expectedRevision) {
        ConfigChangeResult result;
        synchronized (this) {
            if (this.closed) {
                result = result(ConfigChangeResult.Status.REJECTED_CLOSED, List.of(), ConfigValidation.valid(), "Session is closed");
            } else if (this.pendingRequestId >= 0L) {
                result = result(ConfigChangeResult.Status.PENDING, this.pendingFields, ConfigValidation.valid(), "A config apply is pending");
            } else if (expectedRevision != this.revision) {
                result = result(
                        ConfigChangeResult.Status.REJECTED_STALE,
                        List.of(),
                        ConfigValidation.valid(),
                        "Expected revision " + expectedRevision + " but session is at revision " + this.revision
                );
            } else {
                result = applyDirtyFields(expectedRevision);
            }
        }
        ConfigSessionObserver.Kind kind = result.status() == ConfigChangeResult.Status.PENDING
                ? ConfigSessionObserver.Kind.APPLY_PENDING
                : result.accepted() ? ConfigSessionObserver.Kind.APPLIED : ConfigSessionObserver.Kind.REJECTED;
        notifyObservers(kind, result);
        return result;
    }

    public ConfigChangeResult completePending(long requestId, ConfigCommitResult outcome) {
        Objects.requireNonNull(outcome, "outcome");
        ConfigChangeResult result;
        synchronized (this) {
            if (this.closed) {
                result = result(ConfigChangeResult.Status.REJECTED_CLOSED, List.of(), ConfigValidation.valid(), "Session is closed");
            } else if (requestId < 0L || requestId != this.pendingRequestId) {
                result = result(ConfigChangeResult.Status.REJECTED_STALE, List.of(), ConfigValidation.valid(), "Response does not match the pending request");
            } else if (outcome instanceof ConfigCommitResult.Pending) {
                result = result(ConfigChangeResult.Status.PENDING, this.pendingFields, ConfigValidation.valid(), "Config apply is still pending");
            } else {
                List<String> completedFields = this.pendingFields;
                this.pendingRequestId = -1L;
                this.pendingFields = List.of();
                result = finishCommit(outcome, completedFields);
            }
        }
        ConfigSessionObserver.Kind kind = result.status() == ConfigChangeResult.Status.PENDING
                ? ConfigSessionObserver.Kind.APPLY_PENDING
                : result.accepted() ? ConfigSessionObserver.Kind.APPLIED : ConfigSessionObserver.Kind.REJECTED;
        notifyObservers(kind, result);
        return result;
    }

    public ConfigChangeResult refreshAuthoritative(long authoritativeRevision) {
        ConfigChangeResult result;
        synchronized (this) {
            if (this.closed) {
                result = result(ConfigChangeResult.Status.REJECTED_CLOSED, List.of(), ConfigValidation.valid(), "Session is closed");
            } else if (authoritativeRevision < this.revision) {
                result = result(ConfigChangeResult.Status.REJECTED_STALE, List.of(), ConfigValidation.valid(), "Ignored an older authoritative revision");
            } else {
                this.revision = authoritativeRevision;
                this.fields.values().forEach(field -> field.refreshStored(true));
                result = result(ConfigChangeResult.Status.ACCEPTED, List.of(), snapshot().validation(), "Authoritative values refreshed");
            }
        }
        notifyObservers(
                result.accepted() ? ConfigSessionObserver.Kind.AUTHORITATIVE_REFRESH : ConfigSessionObserver.Kind.REJECTED,
                result
        );
        return result;
    }

    public synchronized ConfigSessionObserver.Subscription observe(ConfigSessionObserver observer) {
        Objects.requireNonNull(observer, "observer");
        this.observers.add(observer);
        return () -> this.observers.remove(observer);
    }

    @Override
    public void close() {
        ConfigChangeResult result;
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            result = result(ConfigChangeResult.Status.ACCEPTED, List.of(), ConfigValidation.valid(), "Session closed");
        }
        notifyObservers(ConfigSessionObserver.Kind.CLOSED, result);
        this.observers.clear();
    }

    private ConfigChangeResult setDraft(ConfigMutation.SetDraft mutation) {
        Slot<?> field = this.fields.get(mutation.fieldId());
        if (field == null) {
            return result(
                    ConfigChangeResult.Status.REJECTED_VALIDATION,
                    List.of(),
                    ConfigValidation.error(mutation.fieldId(), "unknown_field", "Unknown config field '" + mutation.fieldId() + "'"),
                    "Unknown config field"
            );
        }
        ConfigPermission permission = field.field.permission();
        if (!permission.editable()) {
            return permissionRejected(field.field.id(), permission);
        }
        ConfigValidation validation = field.setDraft(mutation.input());
        if (validation.hasErrors()) {
            return result(ConfigChangeResult.Status.REJECTED_VALIDATION, List.of(field.field.id()), validation, "Draft is invalid");
        }
        if (!field.dirty()) {
            return result(ConfigChangeResult.Status.NO_OP, List.of(), validation, "Draft matches the stored value");
        }
        return result(ConfigChangeResult.Status.ACCEPTED, List.of(field.field.id()), validation, "Draft changed");
    }

    private ConfigChangeResult resetField(String fieldId) {
        Slot<?> field = requireField(fieldId);
        ConfigPermission permission = field.field.permission();
        if (!permission.editable()) {
            return permissionRejected(fieldId, permission);
        }
        boolean changed = field.resetToDefault();
        return result(
                changed ? ConfigChangeResult.Status.ACCEPTED : ConfigChangeResult.Status.NO_OP,
                changed ? List.of(fieldId) : List.of(),
                field.validation,
                changed ? "Field reset to its default" : "Field already uses its default"
        );
    }

    private ConfigChangeResult resetAll() {
        var changed = new ArrayList<String>();
        for (Slot<?> field : this.fields.values()) {
            ConfigPermission permission = field.field.permission();
            if (!permission.editable()) {
                continue;
            }
            if (field.resetToDefault()) {
                changed.add(field.field.id());
            }
        }
        return result(
                changed.isEmpty() ? ConfigChangeResult.Status.NO_OP : ConfigChangeResult.Status.ACCEPTED,
                changed,
                snapshot().validation(),
                changed.isEmpty() ? "All fields already use their defaults" : "Editable fields reset to their defaults"
        );
    }

    private ConfigChangeResult restoreField(String fieldId) {
        Slot<?> field = requireField(fieldId);
        ConfigPermission permission = field.field.permission();
        if (!permission.editable()) {
            return permissionRejected(fieldId, permission);
        }
        boolean changed = field.restoreSessionStart();
        return result(
                changed ? ConfigChangeResult.Status.ACCEPTED : ConfigChangeResult.Status.NO_OP,
                changed ? List.of(fieldId) : List.of(),
                field.validation,
                changed ? "Field restored to its session-start value" : "Field already matches its session-start value"
        );
    }

    private ConfigChangeResult restoreAll() {
        var changed = new ArrayList<String>();
        for (Slot<?> field : this.fields.values()) {
            if (field.field.permission().editable() && field.restoreSessionStart()) {
                changed.add(field.field.id());
            }
        }
        return result(
                changed.isEmpty() ? ConfigChangeResult.Status.NO_OP : ConfigChangeResult.Status.ACCEPTED,
                changed,
                snapshot().validation(),
                changed.isEmpty() ? "All fields match their session-start values" : "Editable fields restored to their session-start values"
        );
    }

    private ConfigChangeResult rollback() {
        var changed = new ArrayList<String>();
        for (Slot<?> field : this.fields.values()) {
            if (field.rollback()) {
                changed.add(field.field.id());
            }
        }
        return result(
                changed.isEmpty() ? ConfigChangeResult.Status.NO_OP : ConfigChangeResult.Status.ACCEPTED,
                changed,
                ConfigValidation.valid(),
                changed.isEmpty() ? "No draft changes to roll back" : "Draft changes rolled back"
        );
    }

    private ConfigChangeResult applyDirtyFields(long expectedRevision) {
        var dirty = new ArrayList<Slot<?>>();
        var validations = new ArrayList<ConfigValidation>();
        for (Slot<?> field : this.fields.values()) {
            if (!field.dirty()) {
                continue;
            }
            dirty.add(field);
            validations.add(field.validation);
            ConfigPermission permission = field.field.permission();
            if (!permission.editable()) {
                return permissionRejected(field.field.id(), permission);
            }
        }
        if (dirty.isEmpty()) {
            return result(ConfigChangeResult.Status.NO_OP, List.of(), ConfigValidation.valid(), "No draft changes to apply");
        }
        ConfigValidation combined = ConfigValidation.combine(validations);
        if (combined.hasErrors()) {
            return result(ConfigChangeResult.Status.REJECTED_VALIDATION, fieldIds(dirty), combined, "Draft contains invalid values");
        }

        var values = new LinkedHashMap<String, Object>();
        for (Slot<?> field : this.fields.values()) {
            values.put(field.field.id(), field.commitValue());
        }

        ConfigCommitResult committed;
        try {
            committed = Objects.requireNonNull(
                    this.committer.commit(new ConfigSessionCommitter.CommitRequest(this.configId, expectedRevision, values)),
                    "committer result"
            );
        } catch (RuntimeException exception) {
            committed = new ConfigCommitResult.Failed("Config commit threw an exception", exception);
        }

        if (committed instanceof ConfigCommitResult.Pending pending) {
            this.pendingRequestId = pending.requestId();
            this.pendingFields = fieldIds(dirty);
            return result(ConfigChangeResult.Status.PENDING, this.pendingFields, combined, "Draft submitted");
        }
        return finishCommit(committed, fieldIds(dirty));
    }

    private ConfigChangeResult finishCommit(ConfigCommitResult committed, List<String> changedFields) {
        if (committed instanceof ConfigCommitResult.Accepted accepted) {
            if (accepted.revision() <= this.revision) {
                return result(
                        ConfigChangeResult.Status.FAILED,
                        changedFields,
                        ConfigValidation.valid(),
                        "Committer returned a non-increasing revision"
                );
            }
            this.revision = accepted.revision();
            this.fields.values().forEach(field -> field.refreshStored(false));
            return result(ConfigChangeResult.Status.ACCEPTED, changedFields, ConfigValidation.valid(), "Draft applied");
        }
        if (committed instanceof ConfigCommitResult.NoOp noOp) {
            if (noOp.revision() < this.revision) {
                return result(ConfigChangeResult.Status.FAILED, changedFields, ConfigValidation.valid(), "Committer returned an older revision");
            }
            this.revision = noOp.revision();
            this.fields.values().forEach(field -> field.refreshStored(false));
            return result(ConfigChangeResult.Status.NO_OP, changedFields, ConfigValidation.valid(), "Authoritative values already match the draft");
        }
        if (committed instanceof ConfigCommitResult.Stale stale) {
            this.revision = Math.max(this.revision, stale.currentRevision());
            this.fields.values().forEach(field -> field.refreshStored(true));
            return new ConfigChangeResult(
                    ConfigChangeResult.Status.REJECTED_STALE,
                    this.revision,
                    changedFields,
                    ConfigValidation.valid(),
                    stale.message()
            );
        }
        if (committed instanceof ConfigCommitResult.Rejected rejected) {
            ConfigChangeResult.Status status = rejected.reason() == ConfigCommitResult.Reason.VALIDATION
                    ? ConfigChangeResult.Status.REJECTED_VALIDATION
                    : ConfigChangeResult.Status.REJECTED_PERMISSION;
            return result(status, changedFields, rejected.validation(), rejected.message());
        }
        ConfigCommitResult.Failed failed = (ConfigCommitResult.Failed) committed;
        return result(ConfigChangeResult.Status.FAILED, changedFields, ConfigValidation.valid(), failed.message());
    }

    private ConfigChangeResult permissionRejected(String fieldId, ConfigPermission permission) {
        String message = permission.message().isEmpty() ? "Field is read-only" : permission.message();
        return result(
                ConfigChangeResult.Status.REJECTED_PERMISSION,
                List.of(fieldId),
                ConfigValidation.valid(),
                message
        );
    }

    private ConfigChangeResult result(
            ConfigChangeResult.Status status,
            List<String> changedFields,
            ConfigValidation validation,
            String message
    ) {
        return new ConfigChangeResult(status, this.revision, changedFields, validation, message);
    }

    private Slot<?> requireField(String fieldId) {
        Slot<?> field = this.fields.get(requireText(fieldId, "fieldId"));
        if (field == null) {
            throw new IllegalArgumentException("Unknown config field '" + fieldId + "'");
        }
        return field;
    }

    private void notifyObservers(ConfigSessionObserver.Kind kind, ConfigChangeResult result) {
        ConfigSessionObserver.Change change = new ConfigSessionObserver.Change(kind, snapshot(), result);
        for (ConfigSessionObserver observer : this.observers) {
            observer.changed(change);
        }
    }

    private static Map<String, Slot<?>> resolveFields(List<ConfigSessionField<?>> fields) {
        var resolved = new LinkedHashMap<String, Slot<?>>();
        for (ConfigSessionField<?> field : List.copyOf(fields)) {
            Slot<?> previous = resolved.put(field.id(), Slot.open(field));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate config field '" + field.id() + "'");
            }
        }
        return Collections.unmodifiableMap(resolved);
    }

    private static ConfigCommitResult commitLocally(
            ConfigSessionCommitter.CommitRequest request,
            List<ConfigSessionField<?>> fields,
            Runnable save
    ) {
        var byId = new LinkedHashMap<String, ConfigSessionField<?>>();
        fields.forEach(field -> byId.put(field.id(), field));
        var previous = new LinkedHashMap<ConfigSessionField<?>, Object>();
        try {
            for (Map.Entry<String, Object> value : request.values().entrySet()) {
                ConfigSessionField<?> field = byId.get(value.getKey());
                if (field == null) {
                    return new ConfigCommitResult.Rejected(
                            ConfigCommitResult.Reason.VALIDATION,
                            ConfigValidation.error(value.getKey(), "unknown_field", "Unknown config field '" + value.getKey() + "'"),
                            "Commit contains an unknown field"
                    );
                }
                previous.put(field, field.storedValue());
                writeUntyped(field, value.getValue());
            }
            save.run();
            return new ConfigCommitResult.Accepted(request.expectedRevision() + 1L);
        } catch (RuntimeException exception) {
            previous.forEach(ConfigSession::writeUntyped);
            return new ConfigCommitResult.Failed("Failed to save config '" + request.configId() + "'", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void writeUntyped(ConfigSessionField<T> field, Object value) {
        field.write((T) value);
    }

    private static List<String> fieldIds(List<Slot<?>> fields) {
        return fields.stream().map(field -> field.field.id()).toList();
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }

    private static final class Slot<T> {
        private final ConfigSessionField<T> field;
        private final T sessionStart;
        private T stored;
        private T draft;
        private Object draftInput;
        private ConfigValidation validation;

        private Slot(ConfigSessionField<T> field) {
            this.field = field;
            this.stored = field.storedValue();
            this.sessionStart = field.copy(this.stored);
            this.draft = field.copy(this.stored);
            this.draftInput = field.copy(this.draft);
            this.validation = field.semantics().validate(field.id(), this.draft);
        }

        static <T> Slot<T> open(ConfigSessionField<T> field) {
            return new Slot<>(field);
        }

        ConfigValidation setDraft(Object input) {
            ValueParseResult<T> parsed = this.field.semantics().parse(this.field.id(), input);
            this.draftInput = immutableInput(input);
            this.validation = parsed.validation();
            parsed.value().ifPresent(value -> {
                this.draft = this.field.copy(value);
                this.draftInput = this.field.copy(value);
            });
            return this.validation;
        }

        ConfigValidation validate(Object input) {
            return this.field.semantics().parse(this.field.id(), input).validation();
        }

        boolean resetToDefault() {
            T reset = this.field.defaultValue();
            boolean changed = !Objects.equals(this.draftInput, reset) || this.validation.hasErrors();
            this.draft = this.field.copy(reset);
            this.draftInput = this.field.copy(reset);
            this.validation = this.field.semantics().validate(this.field.id(), this.draft);
            return changed;
        }

        boolean rollback() {
            boolean changed = dirty();
            this.draft = this.field.copy(this.stored);
            this.draftInput = this.field.copy(this.stored);
            this.validation = this.field.semantics().validate(this.field.id(), this.draft);
            return changed;
        }

        boolean restoreSessionStart() {
            boolean changed = !Objects.equals(this.draftInput, this.sessionStart) || this.validation.hasErrors();
            this.draft = this.field.copy(this.sessionStart);
            this.draftInput = this.field.copy(this.sessionStart);
            this.validation = this.field.semantics().validate(this.field.id(), this.draft);
            return changed;
        }

        boolean dirty() {
            return this.validation.hasErrors() || !Objects.equals(this.stored, this.draft);
        }

        Object commitValue() {
            return this.field.copy(this.draft);
        }

        void refreshStored(boolean preserveDirtyDraft) {
            boolean wasDirty = dirty();
            this.stored = this.field.effectiveValue();
            if (!preserveDirtyDraft || !wasDirty) {
                this.draft = this.field.copy(this.stored);
                this.draftInput = this.field.copy(this.stored);
                this.validation = this.field.semantics().validate(this.field.id(), this.draft);
            }
        }

        ConfigFieldState<T> snapshot() {
            return new ConfigFieldState<>(
                    this.field.id(),
                    this.field.semantics().kind(),
                    this.field.copy(this.stored),
                    this.field.effectiveValue(),
                    this.field.copy(this.draft),
                    immutableInput(this.draftInput),
                    this.field.copy(this.sessionStart),
                    dirty(),
                    this.validation,
                    this.field.permission()
            );
        }

        private static Object immutableInput(Object input) {
            if (input instanceof List<?> list) {
                return List.copyOf(list);
            }
            if (input instanceof Set<?> set) {
                return Collections.unmodifiableSet(new LinkedHashSet<>(set));
            }
            if (input instanceof Map<?, ?> map) {
                return Collections.unmodifiableMap(new LinkedHashMap<>(map));
            }
            return Objects.requireNonNull(input, "input");
        }
    }
}
//?}
