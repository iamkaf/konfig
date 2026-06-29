package com.iamkaf.konfig.impl.v1.client.legacy;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@ApiStatus.Internal
public final class LegacyRegistrySuggestions {
    private LegacyRegistrySuggestions() {
    }

    public static List<String> filter(List<String> allSuggestions, String query, int limit) {
        if (allSuggestions.isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<String> exact = new ArrayList<String>();
        List<String> prefix = new ArrayList<String>();
        List<String> contains = new ArrayList<String>();

        for (String candidate : allSuggestions) {
            String lowerCandidate = candidate.toLowerCase(Locale.ROOT);
            String pathCandidate = registryPath(lowerCandidate);
            if (normalized.isEmpty()) {
                prefix.add(candidate);
                continue;
            }
            if (lowerCandidate.equals(normalized) || pathCandidate.equals(normalized)) {
                exact.add(candidate);
            } else if (lowerCandidate.startsWith(normalized) || pathCandidate.startsWith(normalized)) {
                prefix.add(candidate);
            } else if (lowerCandidate.contains(normalized) || pathCandidate.contains(normalized)) {
                contains.add(candidate);
            }
        }

        List<String> result = new ArrayList<String>(limit);
        append(result, exact, limit);
        append(result, prefix, limit);
        append(result, contains, limit);
        return result;
    }

    public static String suffix(String currentValue, String suggestion) {
        if (isBlank(suggestion)) {
            return "";
        }
        String current = currentValue == null ? "" : currentValue;
        if (current.isEmpty()) {
            return suggestion;
        }
        if (suggestion.regionMatches(true, 0, current, 0, current.length())) {
            return suggestion.substring(current.length());
        }
        return "";
    }

    private static void append(List<String> target, List<String> source, int limit) {
        for (String value : source) {
            if (target.size() >= limit) {
                return;
            }
            target.add(value);
        }
    }

    private static String registryPath(String registryId) {
        int separator = registryId.indexOf(':');
        return separator >= 0 ? registryId.substring(separator + 1) : registryId;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
