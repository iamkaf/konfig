//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetCatalog;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public final class KonfigFieldsetCatalogModel {
    private static final String BUILTIN_PROFILE = "Built in";
    private static final String USER_PROFILE_KEY = "user";
    private static final String SOURCE_PROFILE_PREFIX = "source:";

    private final FieldsetValue value;
    private final FieldsetCatalog catalog;

    public KonfigFieldsetCatalogModel(FieldsetValue value) {
        this.value = Objects.requireNonNull(value, "value");
        this.catalog = value.schema().catalog()
                .orElseThrow(() -> new IllegalArgumentException("Fieldset does not declare a catalog"));
    }

    public List<Profile> profiles() {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (FieldsetEntry entry : this.value.visibleEntries()) {
            if (entry.editable()) {
                continue;
            }
            counts.merge(this.profileLabel(entry), 1, Integer::sum);
        }
        ArrayList<Profile> profiles = new ArrayList<Profile>();
        counts.forEach((label, count) -> profiles.add(new Profile(
                SOURCE_PROFILE_PREFIX + label,
                label,
                count.intValue(),
                false
        )));

        int editableCount = 0;
        for (FieldsetEntry entry : this.value.visibleEntries()) {
            if (entry.editable()) {
                editableCount++;
            }
        }
        profiles.add(new Profile(USER_PROFILE_KEY, this.catalog.editableProfileLabel(), editableCount, true));
        return List.copyOf(profiles);
    }

    public List<FieldsetEntry> entries(String profileKey, String query, String filterValue) {
        String normalizedQuery = normalize(query);
        String normalizedFilter = normalize(filterValue);
        ArrayList<FieldsetEntry> matches = new ArrayList<FieldsetEntry>();
        for (FieldsetEntry entry : this.value.visibleEntries()) {
            if (!this.profileKey(entry).equals(profileKey)) {
                continue;
            }
            if (!normalizedQuery.isEmpty() && !matches(entry, normalizedQuery, this.value.schema().fields())) {
                continue;
            }
            if (!normalizedFilter.isEmpty() && !this.matchesFilter(entry, normalizedFilter)) {
                continue;
            }
            matches.add(entry);
        }
        return List.copyOf(matches);
    }

    public List<String> filterValues(String profileKey) {
        Optional<FieldsetField<?>> filter = this.catalog.filterField();
        if (filter.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        for (FieldsetEntry entry : this.value.visibleEntries()) {
            if (this.profileKey(entry).equals(profileKey)) {
                values.add(display(read(entry, filter.get())));
            }
        }
        values.remove("");
        return List.copyOf(values);
    }

    public String profileLabel(FieldsetEntry entry) {
        if (entry.editable()) {
            return this.catalog.editableProfileLabel();
        }
        return entry.source().orElse(BUILTIN_PROFILE);
    }

    public String profileKey(FieldsetEntry entry) {
        return entry.editable() ? USER_PROFILE_KEY : SOURCE_PROFILE_PREFIX + this.profileLabel(entry);
    }

    public String editableProfileKey() {
        return USER_PROFILE_KEY;
    }

    private boolean matchesFilter(FieldsetEntry entry, String filterValue) {
        Optional<FieldsetField<?>> filter = this.catalog.filterField();
        return filter.isEmpty() || normalize(display(read(entry, filter.get()))).equals(filterValue);
    }

    private static boolean matches(FieldsetEntry entry, String query, List<FieldsetField<?>> fields) {
        if (normalize(entry.identity()).contains(query)
                || entry.source().map(KonfigFieldsetCatalogModel::normalize).orElse("").contains(query)) {
            return true;
        }
        for (FieldsetField<?> field : fields) {
            if (normalize(display(read(entry, field))).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private static Object read(FieldsetEntry entry, FieldsetField<?> field) {
        return readCaptured(entry, field);
    }

    private static <T> T readCaptured(FieldsetEntry entry, FieldsetField<T> field) {
        return entry.value(field);
    }

    private static String display(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.map(String::valueOf).orElse("");
        }
        return String.valueOf(value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Profile(String key, String label, int entryCount, boolean editable) {
        public Profile {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Catalog profile key cannot be blank");
            }
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("Catalog profile label cannot be blank");
            }
            if (entryCount < 0) {
                throw new IllegalArgumentException("Catalog profile count cannot be negative");
            }
        }
    }
}
//?}
