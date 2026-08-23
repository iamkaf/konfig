//? if >=1.21.11 {
package com.iamkaf.konfig;

import com.google.gson.JsonParser;
import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.KonfigNode;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetBuilder;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntryOwnership;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
import com.iamkaf.konfig.impl.v1.bootstrap.RuntimeEnvironment;
import com.iamkaf.konfig.impl.v1.fieldset.FieldsetCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FieldsetPersistenceTest {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    @TempDir
    Path configDirectory;

    @BeforeEach
    void useIsolatedServerConfigDirectory() {
        RuntimeEnvironment.initialize(this.configDirectory, false);
    }

    @Test
    void tomlRoundTripRestoresBuiltinsAndPersistsOnlyUserEntries() throws IOException {
        FieldsetFixture fixture = fixture();
        FieldsetCodec codec = new FieldsetCodec(fixture.defaults);
        ConfigBuilder builder = Konfig.builder("headless", "fieldsets" + NEXT_ID.incrementAndGet());
        ConfigValue<FieldsetValue> rules = builder.custom("gear", fixture.defaults, codec)
                .validate(value -> value.validate().valid(), "Invalid gear rule")
                .build();
        ConfigHandle handle = builder.build();

        FieldsetEntry changedUser = FieldsetEntry.user("custom")
                .with(fixture.item, "minecraft:netherite_pickaxe")
                .with(fixture.repairCap, 42)
                .with(fixture.note, Optional.of("late game"));
        rules.set(FieldsetValue.of(fixture.defaults.schema(), Arrays.asList(fixture.builtin, changedUser)));
        handle.save();

        String stored = Files.readString(handle.path());
        assertTrue(stored.contains("[[gear]]"));
        assertTrue(stored.contains("_konfig_id = \"custom\""));
        assertTrue(stored.contains("item = \"minecraft:netherite_pickaxe\""));
        assertFalse(stored.contains("diamond_builtin"));

        rules.set(fixture.defaults);
        handle.reload();

        assertEquals(2, rules.get().entries().size());
        assertEquals(fixture.builtin, rules.get().entries().get(0));
        FieldsetEntry restoredUser = rules.get().entries().get(1);
        assertEquals(FieldsetEntryOwnership.USER, restoredUser.ownership());
        assertEquals("custom", restoredUser.identity());
        assertEquals("minecraft:netherite_pickaxe", restoredUser.value(fixture.item));
        assertEquals(42, restoredUser.value(fixture.repairCap));
        assertEquals(Optional.of("late game"), restoredUser.value(fixture.note));
    }

    @Test
    void codecRejectsBuiltinIdentityCollisionsAndUnknownFields() {
        FieldsetFixture fixture = fixture();
        FieldsetCodec codec = new FieldsetCodec(fixture.defaults);

        IllegalArgumentException collision = assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode(node("[{\"_konfig_id\":\"diamond_builtin\"}]"))
        );
        assertTrue(collision.getMessage().contains("Duplicate fieldset entry identity"));

        IllegalArgumentException unknown = assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode(node("[{\"_konfig_id\":\"custom\",\"mystery\":true}]"))
        );
        assertTrue(unknown.getMessage().contains("unknown field: mystery"));
    }

    @Test
    void codecUsesCurrentBuiltinDefaultsInsteadOfPersistedBuiltinData() {
        FieldsetFixture fixture = fixture();
        FieldsetCodec codec = new FieldsetCodec(fixture.defaults);

        FieldsetValue decoded = codec.decode(node(
                "[{\"_konfig_id\":\"custom\",\"item\":\"minecraft:iron_pickaxe\",\"repair_cap\":12}]"
        ));

        assertEquals(fixture.builtin, decoded.entries().get(0));
        assertEquals(FieldsetEntryOwnership.BUILTIN, decoded.entries().get(0).ownership());
        assertEquals(FieldsetEntryOwnership.USER, decoded.entries().get(1).ownership());
    }

    private static KonfigNode node(String json) {
        return new KonfigNode(JsonParser.parseString(json));
    }

    private static FieldsetFixture fixture() {
        FieldsetField<String> item = FieldsetField.string("item", "minecraft:air");
        FieldsetField<Integer> repairCap = FieldsetField.intRange("repair_cap", 10, 0, 100);
        FieldsetField<Optional<String>> note = FieldsetField.optionalString("note");
        FieldsetEntry builtin = FieldsetEntry.builtin("diamond_builtin")
                .with(item, "minecraft:diamond_pickaxe")
                .with(repairCap, 25);
        FieldsetEntry user = FieldsetEntry.user("custom")
                .with(item, "minecraft:iron_pickaxe")
                .with(repairCap, 12);
        FieldsetValue defaults = FieldsetBuilder.create()
                .field(item)
                .field(repairCap)
                .field(note)
                .entry(builtin)
                .entry(user)
                .build();
        return new FieldsetFixture(item, repairCap, note, builtin, defaults);
    }

    private static final class FieldsetFixture {
        private final FieldsetField<String> item;
        private final FieldsetField<Integer> repairCap;
        private final FieldsetField<Optional<String>> note;
        private final FieldsetEntry builtin;
        private final FieldsetValue defaults;

        private FieldsetFixture(
                FieldsetField<String> item,
                FieldsetField<Integer> repairCap,
                FieldsetField<Optional<String>> note,
                FieldsetEntry builtin,
                FieldsetValue defaults
        ) {
            this.item = item;
            this.repairCap = repairCap;
            this.note = note;
            this.builtin = builtin;
            this.defaults = defaults;
        }
    }
}
//?}
