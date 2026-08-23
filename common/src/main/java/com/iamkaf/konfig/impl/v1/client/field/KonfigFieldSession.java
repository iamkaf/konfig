//? if >=1.17 {
package com.iamkaf.konfig.impl.v1.client.field;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenHandle;
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenValue;
//? if >=1.21.11 {
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iamkaf.konfig.impl.v1.config.io.PathJson;
import com.iamkaf.konfig.impl.v1.state.ConfigChangeResult;
import com.iamkaf.konfig.impl.v1.state.ConfigCommitResult;
import com.iamkaf.konfig.impl.v1.state.ConfigMutation;
import com.iamkaf.konfig.impl.v1.state.ConfigPermission;
import com.iamkaf.konfig.impl.v1.state.ConfigSession;
import com.iamkaf.konfig.impl.v1.state.ConfigSessionCommitter;
import com.iamkaf.konfig.impl.v1.state.ConfigSessionField;
import com.iamkaf.konfig.impl.v1.state.ConfigValidation;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditCapabilities;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditResult;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditSnapshot;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import com.iamkaf.konfig.impl.v1.value.ConfigValueSemantics;
//?}

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApiStatus.Internal
public final class KonfigFieldSession implements AutoCloseable {
    private final List<EntryRef> entries;
    private final Map<ConfigScreenValue<?>, KonfigField> fields = new LinkedHashMap<ConfigScreenValue<?>, KonfigField>();

//? if >=1.21.11 {
    private static final Gson GSON = new Gson();

    private final Map<SessionKey, ConfigSession> sessions = new LinkedHashMap<>();
    private final Map<ConfigScreenValue<?>, ConfigSession> sessionsByValue = new LinkedHashMap<>();
    private final Map<String, ConfigSession> remoteSessions = new LinkedHashMap<>();
    private final KonfigSync.ClientEditListener remoteListener = new KonfigSync.ClientEditListener() {
        @Override
        public void onResult(ConfigEditResult result) {
            ConfigSession session = KonfigFieldSession.this.remoteSessions.get(result.configId());
            if (session != null && session.snapshot().applyPending()) {
                session.completePending(result.requestId(), commitResult(result));
            }
        }

        @Override
        public void onCapabilities(ConfigEditCapabilities capabilities) {
            // Permissions are suppliers and pick up capability changes on the next interaction.
        }

        @Override
        public void onSnapshot(ConfigEditSnapshot snapshot) {
            ConfigSession session = KonfigFieldSession.this.remoteSessions.get(snapshot.configId());
            if (session != null && !session.snapshot().applyPending()) {
                session.refreshAuthoritative(snapshot.revision());
            }
        }

        @Override
        public void onDisconnected() {
            for (ConfigSession session : KonfigFieldSession.this.remoteSessions.values()) {
                if (!session.snapshot().applyPending()) {
                    session.refreshAuthoritative(session.revision());
                }
            }
        }
    };
//?}

    public KonfigFieldSession(List<EntryRef> entries) {
        this.entries = List.copyOf(entries);
//? if >=1.21.11 {
        initializeSessions();
        if (!this.remoteSessions.isEmpty()) {
            KonfigSync.addClientEditListener(this.remoteListener);
        }
//?} else {
        for (EntryRef entry : entries) {
            this.fields.put(entry.value, new KonfigField(entry));
        }
//?}
    }

    public KonfigField field(EntryRef entry) {
        return field(entry.value);
    }

    public KonfigField field(ConfigScreenValue<?> value) {
        KonfigField field = this.fields.get(value);
        if (field == null) {
            throw new IllegalArgumentException("Unknown config field '" + value.path() + "'.");
        }
        return field;
    }

//? if >=1.21.11 {
    public ConfigChangeResult persist(EntryRef entry) {
        ConfigSession session = requireSession(entry);
        return session.apply(session.revision());
    }

    public ConfigChangeResult restoreEntry(EntryRef entry) {
        ConfigSession session = requireSession(entry);
        ConfigChangeResult changed = session.mutate(new ConfigMutation.RestoreField(entry.value.path()));
        return changed.accepted() ? session.apply(session.revision()) : changed;
    }

    public List<ConfigChangeResult> restoreAll() {
        var results = new ArrayList<ConfigChangeResult>();
        for (ConfigSession session : this.sessions.values()) {
            ConfigChangeResult changed = session.mutate(new ConfigMutation.RestoreAll());
            results.add(changed.accepted() ? session.apply(session.revision()) : changed);
        }
        return List.copyOf(results);
    }

    public void discard() {
        this.sessions.values().forEach(session -> session.mutate(new ConfigMutation.Rollback()));
    }

    @Override
    public void close() {
        KonfigSync.removeClientEditListener(this.remoteListener);
        discard();
        this.sessions.values().forEach(ConfigSession::close);
    }

    private void initializeSessions() {
        var grouped = new LinkedHashMap<SessionKey, List<EntryRef>>();
        for (EntryRef entry : this.entries) {
            if (!entry.value.isDecoration()) {
                boolean remote = isRemote(entry);
                grouped.computeIfAbsent(new SessionKey(entry.handle, remote), ignored -> new ArrayList<>()).add(entry);
            }
        }

        for (Map.Entry<SessionKey, List<EntryRef>> group : grouped.entrySet()) {
            SessionKey key = group.getKey();
            List<ConfigSessionField<?>> sessionFields = new ArrayList<>();
            group.getValue().forEach(entry -> sessionFields.add(sessionField(entry, key.remote)));
            long revision = key.remote ? Math.max(0L, KonfigSync.clientRevision(key.handle.id())) : key.handle.revision();
            ConfigSession session = key.remote
                    ? new ConfigSession(key.handle.id(), revision, sessionFields, remoteCommitter(key.handle, group.getValue()))
                    : ConfigSession.local(key.handle.id(), revision, sessionFields, key.handle::save);
            this.sessions.put(key, session);
            if (key.remote) {
                this.remoteSessions.put(key.handle.id(), session);
            }
            for (EntryRef entry : group.getValue()) {
                this.sessionsByValue.put(entry.value, session);
                this.fields.put(entry.value, new KonfigField(entry, session));
            }
        }
    }

    private ConfigSession requireSession(EntryRef entry) {
        ConfigSession session = this.sessionsByValue.get(entry.value);
        if (session == null) {
            throw new IllegalArgumentException("Entry has no config session: " + entry.value.path());
        }
        return session;
    }

    private static boolean isRemote(EntryRef entry) {
        return KonfigSync.clientConnected() && entry.value.sync() && !entry.value.clientOnly();
    }

    private static ConfigSessionField<?> sessionField(EntryRef entry, boolean remote) {
        return captureSessionField(entry, remote);
    }

    private static <T> ConfigSessionField<T> captureSessionField(EntryRef entry, boolean remote) {
        @SuppressWarnings("unchecked")
        ConfigScreenValue<T> value = (ConfigScreenValue<T>) entry.value;
        return new ConfigSessionField<>(
                value.path(),
                value.defaultValue(),
                value::get,
                value::get,
                remote ? ignored -> { } : value::set,
                new ConfigValueSemantics<>(value),
                () -> permission(entry, remote)
        );
    }

    private static ConfigPermission permission(EntryRef entry, boolean remote) {
        if (!entry.editable) {
            return ConfigPermission.readOnly(ConfigPermission.Reason.RUNTIME_READ_ONLY, "This field is read-only");
        }
        if (entry.handle.newerSchemaReadOnly()) {
            return ConfigPermission.readOnly(ConfigPermission.Reason.RUNTIME_READ_ONLY, "This config uses a newer schema");
        }
        if (!remote || KonfigSync.remoteEditsAvailable(entry.handle.id())) {
            return ConfigPermission.editablePermission();
        }
        return ConfigPermission.readOnly(
                ConfigPermission.Reason.UNSUPPORTED_PEER,
                "The server does not allow remote config changes"
        );
    }

    private static ConfigSessionCommitter remoteCommitter(ConfigScreenHandle handle, List<EntryRef> entries) {
        var definitions = new LinkedHashMap<String, ConfigScreenValue<?>>();
        entries.forEach(entry -> definitions.put(entry.value.path(), entry.value));
        return request -> {
            JsonObject root = new JsonObject();
            try {
                for (Map.Entry<String, Object> item : request.values().entrySet()) {
                    ConfigScreenValue<?> definition = definitions.get(item.getKey());
                    if (definition == null) {
                        return rejected(ConfigCommitResult.Reason.VALIDATION, item.getKey(), "Unknown config field");
                    }
                    PathJson.put(root, item.getKey(), encode(definition, item.getValue()));
                }
            } catch (RuntimeException exception) {
                return rejected(ConfigCommitResult.Reason.VALIDATION, handle.id(), message(exception));
            }
            long requestId = KonfigSync.submitRemoteDraft(handle.id(), GSON.toJson(root));
            return requestId < 0L
                    ? rejected(ConfigCommitResult.Reason.UNSUPPORTED, handle.id(), "Remote config changes are unavailable")
                    : new ConfigCommitResult.Pending(requestId);
        };
    }

    private static ConfigCommitResult commitResult(ConfigEditResult result) {
        String detail = result.detail().isBlank()
                ? result.status().name().toLowerCase(java.util.Locale.ROOT)
                : result.detail();
        return switch (result.status()) {
            case ACCEPTED -> new ConfigCommitResult.Accepted(result.revision());
            case NO_OP -> new ConfigCommitResult.NoOp(result.revision());
            case STALE -> new ConfigCommitResult.Stale(result.revision(), detail);
            case UNAUTHORIZED -> rejected(ConfigCommitResult.Reason.PERMISSION, result.configId(), detail);
            case READ_ONLY -> rejected(ConfigCommitResult.Reason.READ_ONLY, result.configId(), detail);
            case UNSUPPORTED -> rejected(ConfigCommitResult.Reason.UNSUPPORTED, result.configId(), detail);
            case INVALID, UNKNOWN_CONFIG, TOO_LARGE -> rejected(ConfigCommitResult.Reason.VALIDATION, result.configId(), detail);
        };
    }

    private static ConfigCommitResult.Rejected rejected(ConfigCommitResult.Reason reason, String path, String message) {
        ConfigValidation validation = reason == ConfigCommitResult.Reason.VALIDATION
                ? ConfigValidation.error(path, "remote_edit_rejected", message)
                : ConfigValidation.valid();
        return new ConfigCommitResult.Rejected(reason, validation, message);
    }

    @SuppressWarnings("unchecked")
    private static <T> JsonElement encode(ConfigScreenValue<T> value, Object input) {
        return value.encodeValue((T) input);
    }

    private static String message(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private record SessionKey(ConfigScreenHandle handle, boolean remote) {
    }
//?} else {
    public void resetAll() {
        Map<KonfigField, Object> previousValues = new LinkedHashMap<KonfigField, Object>();
        Set<ConfigScreenHandle> handles = new LinkedHashSet<ConfigScreenHandle>();
        try {
            for (EntryRef entry : this.entries) {
                if (!entry.editable) {
                    continue;
                }
                KonfigField field = field(entry);
                Object resetValue = field.sessionStartValue();
                previousValues.put(field, KonfigFieldValues.snapshotValue(entry.value, entry.value.get()));
                field.setDraft(resetValue);
                KonfigFieldValues.setRawValue(entry.value, resetValue);
                handles.add(entry.handle);
            }
            for (ConfigScreenHandle handle : handles) {
                handle.save();
            }
        } catch (RuntimeException exception) {
            for (Map.Entry<KonfigField, Object> previousValue : previousValues.entrySet()) {
                previousValue.getKey().restoreStoredValue(previousValue.getValue());
            }
            throw exception;
        }
    }

    @Override
    public void close() {
    }
//?}
}
//?}
