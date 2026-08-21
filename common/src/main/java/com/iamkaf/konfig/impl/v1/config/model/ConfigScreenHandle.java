package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigHandle;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@ApiStatus.Internal
public interface ConfigScreenHandle extends ConfigHandle {
    Collection<ConfigScreenValue<?>> screenEntries();

    List<InfoPanelItem> globalInfo();

    List<InfoPanelItem> categoryInfo(String path);

    List<InfoPanelItem> entryInfo(String path);

    String id();

    String tooltip(String path, Function<String, String> translationResolver);
}
