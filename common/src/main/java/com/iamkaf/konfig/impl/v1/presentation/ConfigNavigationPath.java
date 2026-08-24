package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public final class ConfigNavigationPath {
    public enum Kind {
        CONFIG,
        PAGE,
        TAB,
        GROUP,
        DISPLAY_NODE
    }

    private final List<Breadcrumb> breadcrumbs;
    private final ConfigPresentationIdentity target;

    public ConfigNavigationPath(List<Breadcrumb> breadcrumbs, ConfigPresentationIdentity target) {
        this.breadcrumbs = breadcrumbs == null || breadcrumbs.isEmpty()
                ? Collections.<Breadcrumb>emptyList()
                : Collections.unmodifiableList(new ArrayList<Breadcrumb>(breadcrumbs));
        this.target = Objects.requireNonNull(target, "target");
    }

    public List<Breadcrumb> breadcrumbs() {
        return this.breadcrumbs;
    }

    public ConfigPresentationIdentity target() {
        return this.target;
    }

    @ApiStatus.Internal
    public static final class Breadcrumb {
        private final Kind kind;
        private final ConfigPresentationIdentity identity;
        private final ConfigText label;

        public Breadcrumb(Kind kind, ConfigPresentationIdentity identity, ConfigText label) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.identity = Objects.requireNonNull(identity, "identity");
            this.label = label == null ? ConfigText.empty() : label;
        }

        public Kind kind() {
            return this.kind;
        }

        public ConfigPresentationIdentity identity() {
            return this.identity;
        }

        public ConfigText label() {
            return this.label;
        }
    }
}
