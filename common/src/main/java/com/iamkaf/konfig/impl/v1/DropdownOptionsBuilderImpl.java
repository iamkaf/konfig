package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.DropdownOptionBuilder;
import com.iamkaf.konfig.api.v1.DropdownOptionsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

@ApiStatus.Internal
final class DropdownOptionsBuilderImpl implements DropdownOptionsBuilder {
    private final LinkedHashMap<String, DropdownOptionMetadata> options = new LinkedHashMap<String, DropdownOptionMetadata>();

    @Override
    public DropdownOptionsBuilder option(String value) {
        return this.option(value, builder -> {
        });
    }

    @Override
    public DropdownOptionsBuilder option(String value, String label) {
        return this.option(value, label, builder -> {
        });
    }

    @Override
    public DropdownOptionsBuilder option(String value, Consumer<DropdownOptionBuilder> builder) {
        return this.add(value, "", builder);
    }

    @Override
    public DropdownOptionsBuilder option(String value, String label, Consumer<DropdownOptionBuilder> builder) {
        return this.add(value, label, builder);
    }

    List<DropdownOptionMetadata> build() {
        if (this.options.isEmpty()) {
            throw new IllegalArgumentException("Dropdown options cannot be empty");
        }
        return java.util.Collections.unmodifiableList(new ArrayList<DropdownOptionMetadata>(this.options.values()));
    }

    private DropdownOptionsBuilder add(String value, String label, Consumer<DropdownOptionBuilder> builder) {
        String normalized = ConfigBuilderImpl.normalizeDropdownOption(value, "dropdown option");
        if (this.options.containsKey(normalized)) {
            throw new IllegalArgumentException("Duplicate dropdown option: " + normalized);
        }

        DropdownOptionBuilderImpl option = new DropdownOptionBuilderImpl(normalized);
        if (!isBlank(label)) {
            option.label(label);
        }
        if (builder != null) {
            builder.accept(option);
        }
        this.options.put(normalized, option.build());
        return this;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
