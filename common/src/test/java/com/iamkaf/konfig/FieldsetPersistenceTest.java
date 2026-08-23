//? if >=1.21.11 {
package com.iamkaf.konfig;

import com.google.gson.JsonParser;
import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.KonfigNode;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetBuilder;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetCatalog;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntryOwnership;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValidationIssue;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
import com.iamkaf.konfig.impl.v1.bootstrap.RuntimeEnvironment;
import com.iamkaf.konfig.impl.v1.fieldset.FieldsetCodec;
import com.iamkaf.konfig.impl.v1.client.fieldset.KonfigFieldsetCatalogModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
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
    void missingRegistryIdentifiersSurviveReloadAndUnrelatedEdits() throws IOException {
        FieldsetFixture fixture = fixture();
        ConfigBuilder builder = Konfig.builder("headless", "fieldsets" + NEXT_ID.incrementAndGet());
        ConfigValue<FieldsetValue> rules = builder.fieldset("gear", fixture.defaults).build();
        ConfigHandle handle = builder.build();
        FieldsetEntry dormant = FieldsetEntry.user("removed-mod-rule")
                .with(fixture.item, "removed_mod:old_hammer")
                .with(fixture.repairCap, 42);

        rules.set(FieldsetValue.of(fixture.defaults.schema(), List.of(fixture.builtin, dormant)));
        handle.save();
        rules.set(fixture.defaults);
        handle.reload();

        FieldsetEntry restored = rules.get().entry("removed-mod-rule").orElseThrow();
        assertEquals("removed_mod:old_hammer", restored.value(fixture.item));
        rules.set(rules.get().replaceUserEntry(restored.with(fixture.repairCap, 43)));
        handle.save();
        rules.set(fixture.defaults);
        handle.reload();

        FieldsetEntry edited = rules.get().entry("removed-mod-rule").orElseThrow();
        assertEquals("removed_mod:old_hammer", edited.value(fixture.item));
        assertEquals(43, edited.value(fixture.repairCap));
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

    @Test
    void keepsPresentationFieldsInTheSchema() {
        FieldsetField<String> item = FieldsetField.string("item", "minecraft:air");
        FieldsetField<Integer> priority = FieldsetField.intRange("priority", 1, 1, 10);

        FieldsetValue value = FieldsetBuilder.create()
                .field(item)
                .field(priority)
                .title(item)
                .key(item)
                .summary(priority)
                .build();

        assertEquals(Optional.of(item), value.schema().titleField());
        assertEquals(Optional.empty(), value.schema().iconField());
        assertEquals(Optional.of(item), value.schema().keyField());
        assertEquals(List.of(priority), value.schema().summaryFields());
    }

    @Test
    void keyedUserEntryReplacesBuiltinInVisibleViewsUntilDeleted() {
        FieldsetField<String> item = FieldsetField.string("item", "minecraft:air");
        FieldsetField<Integer> repairCap = FieldsetField.intRange("repair_cap", 10, 0, 100);
        FieldsetEntry builtin = FieldsetEntry.builtin("diamond_builtin", "Bonded")
                .with(item, "minecraft:diamond_pickaxe")
                .with(repairCap, 25);
        FieldsetEntry replacement = FieldsetEntry.user("diamond_override")
                .with(item, "minecraft:diamond_pickaxe")
                .with(repairCap, 40);
        FieldsetValue defaults = FieldsetBuilder.create()
                .field(item)
                .field(repairCap)
                .key(item)
                .entry(builtin)
                .build();
        FieldsetCodec codec = new FieldsetCodec(defaults);
        FieldsetValue value = defaults.add(replacement);

        assertEquals(List.of(builtin), defaults.search("bonded"));
        assertEquals(List.of(builtin, replacement), value.entries());
        assertEquals(List.of(replacement), value.visibleEntries());
        assertTrue(value.search("diamond_builtin").isEmpty());
        assertTrue(value.search("bonded").isEmpty());
        assertEquals(List.of(replacement), value.search("40"));

        String encoded = codec.encode(value).json().toString();
        assertTrue(encoded.contains("diamond_override"));
        assertFalse(encoded.contains("diamond_builtin"));
        assertFalse(encoded.contains("Bonded"));

        value = codec.decode(codec.encode(value));
        assertEquals(List.of(builtin, replacement), value.entries());
        assertEquals(List.of(replacement), value.visibleEntries());

        FieldsetValue revealed = value.deleteUserEntry(replacement.identity());
        assertEquals(List.of(builtin), revealed.visibleEntries());
        assertEquals(Optional.of("Bonded"), builtin.source());
    }

    @Test
    void copyingSourcedBuiltinCopiesValuesButNotOwnershipOrSource() {
        FieldsetField<String> item = FieldsetField.string("item", "minecraft:air");
        FieldsetField<Integer> repairCap = FieldsetField.intRange("repair_cap", 10, 0, 100);
        FieldsetEntry builtin = FieldsetEntry.builtin("basic_weapons_spear", "Basic Weapons")
                .with(item, "basicweapons:iron_spear")
                .with(repairCap, 18);
        FieldsetValue value = FieldsetBuilder.create()
                .field(item)
                .field(repairCap)
                .key(item)
                .entry(builtin)
                .build()
                .duplicateAsUser(builtin.identity(), "spear_override");

        FieldsetEntry copy = value.entry("spear_override").orElseThrow();
        assertEquals(FieldsetEntryOwnership.USER, copy.ownership());
        assertEquals(Optional.empty(), copy.source());
        assertEquals("basicweapons:iron_spear", copy.value(item));
        assertEquals(18, copy.value(repairCap));
        assertEquals(List.of(copy), value.visibleEntries());
    }

    @Test
    void fieldsetConfigKeepsSemanticallyInvalidRowsForConsumerIsolation() throws IOException {
        FieldsetField<String> item = FieldsetField.string("item", "minecraft:air");
        FieldsetField<Integer> repairCap = FieldsetField.intRange("repair_cap", 10, 0, 100);
        FieldsetValue defaults = FieldsetBuilder.create()
                .field(item)
                .field(repairCap)
                .build();
        FieldsetEntry valid = FieldsetEntry.user("valid")
                .with(item, "minecraft:iron_pickaxe")
                .with(repairCap, 12);
        FieldsetEntry invalid = FieldsetEntry.user("invalid")
                .with(item, "minecraft:golden_pickaxe")
                .with(repairCap, 101);

        ConfigBuilder builder = Konfig.builder("headless", "fieldsets" + NEXT_ID.incrementAndGet());
        ConfigValue<FieldsetValue> rules = builder.fieldset("gear", defaults).build();
        ConfigHandle handle = builder.build();
        rules.set(FieldsetValue.of(defaults.schema(), List.of(valid, invalid)));
        handle.save();

        rules.set(defaults);
        handle.reload();

        assertEquals(List.of(valid, invalid), rules.get().entries());
        List<FieldsetValidationIssue> issues = rules.get().validate().issues();
        assertEquals(1, issues.size());
        assertEquals("invalid", issues.get(0).entryIdentity());
        assertEquals(Optional.of("repair_cap"), issues.get(0).fieldKey());
    }

    @Test
    void rejectsPresentationFieldsNotDeclaredByTheBuilder() {
        FieldsetField<String> declared = FieldsetField.string("declared", "value");
        FieldsetField<String> foreign = FieldsetField.string("foreign", "value");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> FieldsetBuilder.create()
                .field(declared)
                .title(foreign)
                .build());

        assertTrue(exception.getMessage().contains("title field is not declared"));

        IllegalStateException keyException = assertThrows(IllegalStateException.class, () -> FieldsetBuilder.create()
                .field(declared)
                .key(foreign)
                .build());

        assertTrue(keyException.getMessage().contains("key field is not declared"));
    }

    @Test
    void catalogGroupsOnlyActiveSourcesAndKeepsUserDeclarationsSeparate() {
        FieldsetField<String> item = FieldsetField.string("item", "minecraft:air");
        FieldsetField<String> type = FieldsetField.dropdown(
                "type",
                "melee",
                List.of("melee", "mining")
        );
        FieldsetCatalog catalog = FieldsetCatalog.create()
                .editableProfile("User Overrides")
                .filter(type)
                .section("Rule", item, type)
                .warning(entry -> entry.editable()
                        ? Optional.of("Dormant until its item is available")
                        : Optional.empty())
                .build();
        FieldsetEntry vanilla = FieldsetEntry.builtin("vanilla", "Vanilla")
                .with(item, "minecraft:iron_sword")
                .with(type, "melee");
        FieldsetEntry basicWeapons = FieldsetEntry.builtin("basic", "Basic Weapons")
                .with(item, "basicweapons:iron_spear")
                .with(type, "melee");
        FieldsetEntry tagOverride = FieldsetEntry.user("tag_override")
                .with(item, "#c:tools/mining_tool")
                .with(type, "mining");
        FieldsetValue value = FieldsetBuilder.create()
                .field(item)
                .field(type)
                .catalog(catalog)
                .entry(vanilla)
                .entry(basicWeapons)
                .entry(tagOverride)
                .build();

        KonfigFieldsetCatalogModel model = new KonfigFieldsetCatalogModel(value);

        assertEquals(List.of(
                new KonfigFieldsetCatalogModel.Profile("source:Vanilla", "Vanilla", 1, false),
                new KonfigFieldsetCatalogModel.Profile("source:Basic Weapons", "Basic Weapons", 1, false),
                new KonfigFieldsetCatalogModel.Profile("user", "User Overrides", 1, true)
        ), model.profiles());
        assertEquals(List.of(basicWeapons), model.entries("source:Basic Weapons", "spear", "melee"));
        assertEquals(List.of(tagOverride), model.entries("user", "#c:tools", "mining"));
        assertEquals(List.of("melee"), model.filterValues("source:Basic Weapons"));
        assertEquals(Optional.empty(), catalog.warning(vanilla));
        assertEquals(Optional.of("Dormant until its item is available"), catalog.warning(tagOverride));
    }

    @Test
    void catalogRejectsForeignFilterAndSectionFields() {
        FieldsetField<String> declared = FieldsetField.string("declared", "value");
        FieldsetField<String> foreign = FieldsetField.string("foreign", "value");

        IllegalStateException filter = assertThrows(IllegalStateException.class, () -> FieldsetBuilder.create()
                .field(declared)
                .catalog(FieldsetCatalog.create().filter(foreign).build())
                .build());
        assertTrue(filter.getMessage().contains("catalog filter field is not declared"));

        IllegalStateException section = assertThrows(IllegalStateException.class, () -> FieldsetBuilder.create()
                .field(declared)
                .catalog(FieldsetCatalog.create().section("Foreign", foreign).build())
                .build());
        assertTrue(section.getMessage().contains("catalog section field is not declared"));
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
