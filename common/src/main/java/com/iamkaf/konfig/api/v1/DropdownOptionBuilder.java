package com.iamkaf.konfig.api.v1;

import java.util.function.Consumer;

public interface DropdownOptionBuilder {
    DropdownOptionBuilder label(String label);

    DropdownOptionBuilder labelKey(String translationKey);

    DropdownOptionBuilder tooltip(String tooltip);

    DropdownOptionBuilder tooltipKey(String translationKey);

    DropdownOptionBuilder info(Consumer<InfoPanelBuilder> builder);
}
