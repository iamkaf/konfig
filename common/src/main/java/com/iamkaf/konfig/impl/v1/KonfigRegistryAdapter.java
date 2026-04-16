package com.iamkaf.konfig.impl.v1;

//? if >=1.17 {
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//?} elif >=1.19.3 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
//?}
import net.minecraft.core.Registry;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class KonfigRegistryAdapter {
    static final int SUGGESTION_LIMIT = 7;

    private KonfigRegistryAdapter() {
    }

//? if >=1.21.11 {
    private static Identifier parseIdentifier(String value) {
        if (KonfigScreenSupport.isBlank(value)) {
            return null;
        }

        String normalized = value.trim();
        int separator = normalized.indexOf(':');
        String namespace = separator >= 0 ? normalized.substring(0, separator) : "minecraft";
        String path = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        if (KonfigScreenSupport.isBlank(namespace) || KonfigScreenSupport.isBlank(path)) {
            return null;
        }

        try {
            return Identifier.fromNamespaceAndPath(namespace, path);
        } catch (Exception ignored) {
            return null;
        }
    }
//?} elif >=1.21.1 {
    private static ResourceLocation parseIdentifier(String value) {
        if (KonfigScreenSupport.isBlank(value)) {
            return null;
        }

        String normalized = value.trim();
        int separator = normalized.indexOf(':');
        String namespace = separator >= 0 ? normalized.substring(0, separator) : "minecraft";
        String path = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        if (KonfigScreenSupport.isBlank(namespace) || KonfigScreenSupport.isBlank(path)) {
            return null;
        }

        try {
            return ResourceLocation.fromNamespaceAndPath(namespace, path);
        } catch (Exception ignored) {
            return null;
        }
    }
//?} else {
    private static ResourceLocation parseIdentifier(String value) {
        if (KonfigScreenSupport.isBlank(value)) {
            return null;
        }

        try {
            return ResourceLocation.tryParse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }
//?}

    static boolean supportsRegistryIcon(ResourceKey<? extends Registry<?>> registryKey) {
//? if >=1.19.3 {
        return registryKey == Registries.ITEM || registryKey == Registries.BLOCK;
//?} else {
        return registryKey == Registry.ITEM_REGISTRY || registryKey == Registry.BLOCK_REGISTRY;
//?}
    }

    private static ItemStack registryIconStack(ResourceKey<? extends Registry<?>> registryKey, String value) {
        if (!supportsRegistryIcon(registryKey)) {
            return ItemStack.EMPTY;
        }

//? if >=1.21.11 {
        Identifier identifier = parseIdentifier(value);
//?} else {
        ResourceLocation identifier = parseIdentifier(value);
//?}
        if (identifier == null) {
            return ItemStack.EMPTY;
        }

//? if >=1.19.3 {
        if (registryKey == Registries.ITEM) {
//? if >=1.21.2 {
            Item item = BuiltInRegistries.ITEM.getValue(identifier);
//?} else {
            Item item = BuiltInRegistries.ITEM.get(identifier);
//?}
//?} else {
        if (registryKey == Registry.ITEM_REGISTRY) {
            Item item = Registry.ITEM.get(identifier);
//?}
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
            return ItemStack.EMPTY;
        }

//? if >=1.19.3 {
//? if >=1.21.2 {
        Block block = BuiltInRegistries.BLOCK.getValue(identifier);
//?} else {
        Block block = BuiltInRegistries.BLOCK.get(identifier);
//?}
//?} else {
        Block block = Registry.BLOCK.get(identifier);
//?}
        if (block == null) {
            return ItemStack.EMPTY;
        }

        Item item = block.asItem();
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

//? if >=26.1 {
    static void renderRegistryIcon(GuiGraphicsExtractor guiGraphics, ResourceKey<? extends Registry<?>> registryKey, String value, int x, int y) {
        ItemStack stack = registryIconStack(registryKey, value);
        if (!stack.isEmpty()) {
            guiGraphics.item(stack, x, y);
        }
    }
//?} elif >=1.20 {
    static void renderRegistryIcon(GuiGraphics guiGraphics, ResourceKey<? extends Registry<?>> registryKey, String value, int x, int y) {
        ItemStack stack = registryIconStack(registryKey, value);
        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, x, y);
        }
    }
//?} else {
    static void renderRegistryIcon(PoseStack guiGraphics, ResourceKey<? extends Registry<?>> registryKey, String value, int x, int y) {
        ItemStack stack = registryIconStack(registryKey, value);
        if (!stack.isEmpty()) {
//? if >=1.19.4 {
            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(guiGraphics, stack, x, y);
//?} else {
            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y);
//?}
        }
    }
//?}

    @SuppressWarnings("unchecked")
    static Registry<?> builtInRegistry(ResourceKey<? extends Registry<?>> registryKey) {
//? if >=1.19.3 {
//? if >=1.21.11 {
        if (registryKey == null || !BuiltInRegistries.REGISTRY.containsKey(registryKey.identifier())) {
            return null;
        }
        return (Registry<?>) BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
//?} elif >=1.21.2 {
        if (registryKey == null || !BuiltInRegistries.REGISTRY.containsKey(registryKey.location())) {
            return null;
        }
        return (Registry<?>) BuiltInRegistries.REGISTRY.getValue(registryKey.location());
//?} else {
        if (registryKey == null || !BuiltInRegistries.REGISTRY.containsKey(registryKey.location())) {
            return null;
        }
        return (Registry<?>) BuiltInRegistries.REGISTRY.get(registryKey.location());
//?}
//?} else {
        if (registryKey == null || !Registry.REGISTRY.containsKey(registryKey.location())) {
            return null;
        }
        return (Registry<?>) Registry.REGISTRY.get(registryKey.location());
//?}
    }

    static List<String> filterRegistrySuggestions(List<String> allSuggestions, String query) {
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

        List<String> result = new ArrayList<String>(SUGGESTION_LIMIT);
        appendSuggestions(result, exact);
        appendSuggestions(result, prefix);
        appendSuggestions(result, contains);
        return result;
    }

    private static void appendSuggestions(List<String> target, List<String> source) {
        for (String value : source) {
            if (target.size() >= SUGGESTION_LIMIT) {
                return;
            }
            target.add(value);
        }
    }

    private static String registryPath(String registryId) {
        int separator = registryId.indexOf(':');
        return separator >= 0 ? registryId.substring(separator + 1) : registryId;
    }

    static String suggestionSuffix(String currentValue, String suggestion) {
        if (KonfigScreenSupport.isBlank(suggestion)) {
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
}
//?}
