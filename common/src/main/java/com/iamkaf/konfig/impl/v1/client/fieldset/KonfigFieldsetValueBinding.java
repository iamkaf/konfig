//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface KonfigFieldsetValueBinding<T> {
    T draft();

    boolean dirty();

    KonfigFieldsetAccess access();

    KonfigFieldsetValidation validation();

    KonfigFieldsetEditResult setDraft(T value);

    KonfigFieldsetEditResult reset();
}
//?}
