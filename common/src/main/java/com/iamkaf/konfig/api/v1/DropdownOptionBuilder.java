package com.iamkaf.konfig.api.v1;

import java.util.function.Consumer;

/**
 * Builds display metadata for one dropdown option.
 */
public interface DropdownOptionBuilder {
    /**
     * Sets a literal display label.
     *
     * @param label the visible label
     * @return this builder
     */
    DropdownOptionBuilder label(String label);

    /**
     * Sets a translated display label.
     *
     * @param translationKey the label translation key
     * @return this builder
     */
    DropdownOptionBuilder labelKey(String translationKey);

    /**
     * Sets literal tooltip text.
     *
     * @param tooltip the tooltip text
     * @return this builder
     */
    DropdownOptionBuilder tooltip(String tooltip);

    /**
     * Sets translated tooltip text.
     *
     * @param translationKey the tooltip translation key
     * @return this builder
     */
    DropdownOptionBuilder tooltipKey(String translationKey);

    /**
     * Adds information-panel content for this option.
     *
     * @param builder the information-panel builder callback
     * @return this builder
     */
    DropdownOptionBuilder info(Consumer<InfoPanelBuilder> builder);
}
