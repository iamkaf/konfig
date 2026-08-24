package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApiStatus.Internal
public final class ConfigSearchIndex {
    private final Map<ConfigPresentationIdentity, String> textByNode;
    private final Map<ConfigPresentationIdentity, ConfigDisplayNode> nodesById;

    ConfigSearchIndex(List<ConfigDisplayNode> nodes) {
        Map<ConfigPresentationIdentity, String> text = new LinkedHashMap<ConfigPresentationIdentity, String>();
        Map<ConfigPresentationIdentity, ConfigDisplayNode> indexedNodes = new LinkedHashMap<ConfigPresentationIdentity, ConfigDisplayNode>();
        for (ConfigDisplayNode node : nodes) {
            StringBuilder searchable = new StringBuilder();
            for (String value : node.search().searchableText()) {
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }
                if (searchable.length() > 0) {
                    searchable.append('\n');
                }
                searchable.append(value.toLowerCase(Locale.ROOT));
            }
            text.put(node.identity(), searchable.toString());
            indexedNodes.put(node.identity(), node);
        }
        this.textByNode = Collections.unmodifiableMap(text);
        this.nodesById = Collections.unmodifiableMap(indexedNodes);
    }

    public List<ConfigDisplayNode> search(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<ConfigDisplayNode>(this.nodesById.values()));
        }
        String[] terms = normalized.split("\\s+");
        List<ConfigDisplayNode> result = new ArrayList<ConfigDisplayNode>();
        for (Map.Entry<ConfigPresentationIdentity, String> entry : this.textByNode.entrySet()) {
            boolean matches = true;
            for (String term : terms) {
                if (!entry.getValue().contains(term)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                result.add(this.nodesById.get(entry.getKey()));
            }
        }
        return Collections.unmodifiableList(result);
    }
}
