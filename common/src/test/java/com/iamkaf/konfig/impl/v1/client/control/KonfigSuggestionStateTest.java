//? if >=1.17 {
package com.iamkaf.konfig.impl.v1.client.control;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KonfigSuggestionStateTest {
    @Test
    void hashPrefixSwitchesSuggestionsToTags() {
        List<String> candidates = List.of(
                "minecraft:diamond",
                "#minecraft:diamond_tool_materials",
                "#c:gems/diamond"
        );

        assertEquals(List.of("minecraft:diamond"),
                KonfigSuggestionState.filterRegistrySuggestions(candidates, "diamond"));
        assertEquals(List.of("#minecraft:diamond_tool_materials", "#c:gems/diamond"),
                KonfigSuggestionState.filterRegistrySuggestions(candidates, "#diamond"));
        assertEquals(List.of("#minecraft:diamond_tool_materials"),
                KonfigSuggestionState.filterRegistrySuggestions(candidates, "#minecraft:diamond"));
    }
}
//?}
