package com.iamkaf.konfig.api.v1;

import java.util.function.Consumer;

public interface DropdownOptionsBuilder {
    DropdownOptionsBuilder option(String value);

    DropdownOptionsBuilder option(String value, String label);

    DropdownOptionsBuilder option(String value, Consumer<DropdownOptionBuilder> builder);

    DropdownOptionsBuilder option(String value, String label, Consumer<DropdownOptionBuilder> builder);
}
