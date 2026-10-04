package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

@ApiStatus.Internal
public interface RegistryTextInputRowHandle {
    boolean isFocused();

    boolean isPointInsideInput(double mouseX, double mouseY);

    void refreshSuggestions();

    void activateSuggestions();

    void closeSuggestions();

    void renderSuggestions(KonfigRenderContext context, int mouseX, int mouseY);

//? if >=1.21.9 {
    boolean handleSuggestionClick(MouseButtonEvent event);

    boolean handleSuggestionKey(KeyEvent event);
//?} else {
    boolean handleSuggestionClick(double mouseX, double mouseY);

    boolean handleSuggestionKey(int keyCode);
//?}
}
