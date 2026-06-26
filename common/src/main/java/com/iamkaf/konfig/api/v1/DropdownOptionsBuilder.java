package com.iamkaf.konfig.api.v1;

import java.util.function.Consumer;

/**
 * Builds the option set for a dropdown value.
 */
public interface DropdownOptionsBuilder {
    /**
     * Adds an option whose label defaults to its stored value.
     *
     * @param value the stored option value
     * @return this builder
     */
    DropdownOptionsBuilder option(String value);

    /**
     * Adds an option with a literal label.
     *
     * @param value the stored option value
     * @param label the visible label
     * @return this builder
     */
    DropdownOptionsBuilder option(String value, String label);

    /**
     * Adds an option with metadata.
     *
     * @param value the stored option value
     * @param builder the option metadata callback
     * @return this builder
     */
    DropdownOptionsBuilder option(String value, Consumer<DropdownOptionBuilder> builder);

    /**
     * Adds an option with a literal label and metadata.
     *
     * @param value the stored option value
     * @param label the visible label
     * @param builder the option metadata callback
     * @return this builder
     */
    DropdownOptionsBuilder option(String value, String label, Consumer<DropdownOptionBuilder> builder);
}
