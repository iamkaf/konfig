//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.chat.Component;

import java.util.Objects;

@ApiStatus.Internal
public final class KonfigFieldsetAccess {
    private final boolean canAdd;
    private final boolean canEdit;
    private final boolean canDuplicate;
    private final boolean canDelete;
    private final boolean canReorder;
    private final Component reason;

    private KonfigFieldsetAccess(
            boolean canAdd,
            boolean canEdit,
            boolean canDuplicate,
            boolean canDelete,
            boolean canReorder,
            Component reason
    ) {
        this.canAdd = canAdd;
        this.canEdit = canEdit;
        this.canDuplicate = canDuplicate;
        this.canDelete = canDelete;
        this.canReorder = canReorder;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public static KonfigFieldsetAccess editable() {
        return new KonfigFieldsetAccess(true, true, true, true, true, Component.empty());
    }

    public static KonfigFieldsetAccess readOnly(Component reason) {
        return new KonfigFieldsetAccess(false, false, false, false, false, reason);
    }

    public static KonfigFieldsetAccess builtinReadOnly() {
        return new KonfigFieldsetAccess(
                false,
                false,
                true,
                false,
                false,
                Component.literal("This entry is built in. Duplicate it to make an editable copy.")
        );
    }

    public static KonfigFieldsetAccess remoteReadOnly() {
        return readOnly(Component.literal("This server allows viewing this config, but not editing it."));
    }

    public static KonfigFieldsetAccess of(
            boolean canAdd,
            boolean canEdit,
            boolean canDuplicate,
            boolean canDelete,
            boolean canReorder,
            Component reason
    ) {
        return new KonfigFieldsetAccess(canAdd, canEdit, canDuplicate, canDelete, canReorder, reason);
    }

    public boolean canAdd() {
        return this.canAdd;
    }

    public boolean canEdit() {
        return this.canEdit;
    }

    public boolean canDuplicate() {
        return this.canDuplicate;
    }

    public boolean canDelete() {
        return this.canDelete;
    }

    public boolean canReorder() {
        return this.canReorder;
    }

    public boolean isReadOnly() {
        return !this.canEdit;
    }

    public Component reason() {
        return this.reason;
    }
}
//?}
