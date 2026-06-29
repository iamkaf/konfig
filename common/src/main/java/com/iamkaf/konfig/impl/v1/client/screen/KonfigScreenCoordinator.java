//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.screen;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.builtInRegistry;

import com.iamkaf.konfig.impl.v1.client.editor.KonfigStringListEditorState;
import com.iamkaf.konfig.impl.v1.client.field.KonfigField;
import com.iamkaf.konfig.impl.v1.client.field.KonfigFieldSession;
import com.iamkaf.konfig.impl.v1.client.info.KonfigInfoPanelBounds;
import com.iamkaf.konfig.impl.v1.client.info.KonfigInfoPanelState;
import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
import com.iamkaf.konfig.impl.v1.client.row.DropdownRowHandle;
import com.iamkaf.konfig.impl.v1.client.row.RegistryTextInputRowHandle;
import com.iamkaf.konfig.impl.v1.client.toast.KonfigToastSupport;
import com.iamkaf.konfig.impl.v1.config.model.DropdownOptionMetadata;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import com.iamkaf.konfig.impl.v1.config.model.InfoPanelItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
final class KonfigScreenCoordinator {
    private final List<EntryRef> entries;
    private final KonfigFieldSession fields;
    private final KonfigInfoPanelState infoPanel;
    private final Map<ResourceKey<? extends Registry<?>>, List<String>> registrySuggestionCache = new LinkedHashMap<ResourceKey<? extends Registry<?>>, List<String>>();

    private RegistryTextInputRowHandle activeRegistryRow;
    private RegistryTextInputRowHandle renderedRegistryRow;
    private DropdownRowHandle activeDropdownRow;
    private DropdownRowHandle renderedDropdownRow;
    private String pendingTooltip;
    private int pendingTooltipMouseX;
    private int pendingTooltipMouseY;

    KonfigScreenCoordinator(List<EntryRef> entries) {
        this.entries = entries;
        this.fields = new KonfigFieldSession(entries);
        this.infoPanel = new KonfigInfoPanelState(entries);
    }

    List<EntryRef> entries() {
        return this.entries;
    }

    KonfigInfoPanelState infoPanel() {
        return this.infoPanel;
    }

    void clearRebuiltWidgetState() {
        this.activeDropdownRow = null;
        this.renderedDropdownRow = null;
    }

    void beginFrame(KonfigInfoPanelBounds infoPanelBounds, int mouseX, int mouseY) {
        this.renderedRegistryRow = null;
        this.renderedDropdownRow = null;
        this.pendingTooltip = null;
        this.infoPanel.beginFrame(infoPanelBounds, mouseX, mouseY);
    }

    void renderFloatingLayers(KonfigRenderContext context, Screen screen, Font font, int mouseX, int mouseY) {
        context.renderFloatingLayers(
                layer -> {
                    if (this.renderedRegistryRow != null) {
                        this.renderedRegistryRow.renderSuggestions(layer, mouseX, mouseY);
                    }
                    if (this.renderedDropdownRow != null) {
                        this.renderedDropdownRow.renderDropdown(layer, mouseX, mouseY);
                    }
                },
                layer -> layer.renderTooltipNow(screen, font, this.pendingTooltip, this.pendingTooltipMouseX, this.pendingTooltipMouseY)
        );
    }

    void queueTooltip(String tooltip, int mouseX, int mouseY) {
        this.pendingTooltip = tooltip;
        this.pendingTooltipMouseX = mouseX;
        this.pendingTooltipMouseY = mouseY;
    }

    boolean persistEntry(EntryRef entry) {
        try {
            this.field(entry).persist();
            return true;
        } catch (RuntimeException exception) {
            KonfigToastSupport.saveFailed(exceptionMessage(exception));
            return false;
        }
    }

    void resetAll() {
        try {
            this.fields.resetAll();
        } catch (RuntimeException exception) {
            KonfigToastSupport.resetFailed(exceptionMessage(exception));
        }
    }

    boolean resetEntry(EntryRef entry) {
        try {
            this.field(entry).resetToSessionStart();
            return true;
        } catch (RuntimeException exception) {
            KonfigToastSupport.resetFailed(exceptionMessage(exception));
            return false;
        }
    }

    KonfigField field(EntryRef entry) {
        return this.fields.field(entry);
    }

    KonfigStringListEditorState stringListEditorState(EntryRef entry, KonfigStringListEditorState.PersistAction persistAction) {
        return new KonfigStringListEditorState(this.field(entry), persistAction);
    }

    void updateHoveredEntry(EntryRef entry, boolean hovered) {
        this.infoPanel.updateHoveredEntry(entry, hovered);
    }

    void updateActiveDropdownOptionInfo(int mouseX, int mouseY) {
        if (this.activeDropdownRow == null) {
            this.infoPanel.setActiveDropdownOptionInfo(null, Collections.emptyList());
            return;
        }

        DropdownOptionMetadata option = this.activeDropdownRow.activeInfoOption(mouseX, mouseY);
        if (option == null || option.info().isEmpty()) {
            this.infoPanel.setActiveDropdownOptionInfo(null, Collections.emptyList());
            return;
        }

        this.infoPanel.setActiveDropdownOptionInfo(this.activeDropdownRow.entry(), option.info());
    }

    List<InfoPanelItem> selectedDropdownOptionInfo(EntryRef entry) {
        if (entry.value.kind() != EntryKind.DROPDOWN) {
            return Collections.emptyList();
        }
        if (this.activeDropdownRow != null && this.activeDropdownRow.entry() == entry) {
            return Collections.emptyList();
        }

        DropdownOptionMetadata option = this.field(entry).currentDropdownOption();
        return option == null ? Collections.emptyList() : option.info();
    }

    String clickedInfoPanelLink(KonfigInfoPanelBounds infoPanelBounds, double mouseX, double mouseY) {
        return this.infoPanel.clickedLink(infoPanelBounds, mouseX, mouseY);
    }

    boolean handleInfoPanelScroll(KonfigInfoPanelBounds infoPanelBounds, double mouseX, double mouseY, double scrollY) {
        return this.infoPanel.handleScroll(infoPanelBounds, mouseX, mouseY, scrollY);
    }

    DropdownRowHandle activeDropdownRow() {
        return this.activeDropdownRow;
    }

    RegistryTextInputRowHandle activeRegistryRow() {
        return this.activeRegistryRow;
    }

    void setActiveRegistryRow(RegistryTextInputRowHandle row) {
        if (this.activeDropdownRow != null) {
            this.activeDropdownRow.closeDropdown();
        }
        if (this.activeRegistryRow == row) {
            return;
        }
        if (this.activeRegistryRow != null) {
            this.activeRegistryRow.closeSuggestions();
        }
        this.activeRegistryRow = row;
    }

    boolean isActiveRegistryRow(RegistryTextInputRowHandle row) {
        return this.activeRegistryRow == row;
    }

    void clearActiveRegistryRow(RegistryTextInputRowHandle row) {
        if (this.activeRegistryRow == row) {
            this.activeRegistryRow = null;
        }
    }

    void markRenderedRegistryRow(RegistryTextInputRowHandle row) {
        this.renderedRegistryRow = row;
    }

    void setActiveDropdownRow(DropdownRowHandle row) {
        if (this.activeDropdownRow == row) {
            return;
        }
        if (this.activeRegistryRow != null) {
            this.activeRegistryRow.closeSuggestions();
        }
        if (this.activeDropdownRow != null) {
            this.activeDropdownRow.closeDropdown();
        }
        this.activeDropdownRow = row;
    }

    void clearActiveDropdownRow(DropdownRowHandle row) {
        if (this.activeDropdownRow == row) {
            this.activeDropdownRow = null;
        }
    }

    void clearRenderedDropdownRow(DropdownRowHandle row) {
        if (this.renderedDropdownRow == row) {
            this.renderedDropdownRow = null;
        }
    }

    void markRenderedDropdownRow(DropdownRowHandle row) {
        this.renderedDropdownRow = row;
    }

    List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
        List<String> cached = this.registrySuggestionCache.get(registryKey);
        if (cached != null) {
            return cached;
        }

        List<String> values = new ArrayList<String>();
        Registry<?> registry = builtInRegistry(registryKey);
        if (registry != null) {
            for (Object key : registry.keySet()) {
                values.add(String.valueOf(key));
            }
            Collections.sort(values);
        }

        List<String> immutable = Collections.unmodifiableList(values);
        this.registrySuggestionCache.put(registryKey, immutable);
        return immutable;
    }

    private static String exceptionMessage(Exception exception) {
        return exception.getMessage() == null ? "" : exception.getMessage();
    }
}
//?}
