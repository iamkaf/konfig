//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.config.model.DropdownOptionMetadata;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

@ApiStatus.Internal
public interface DropdownRowHandle {
    EntryRef entry();

    void closeDropdown();

    boolean isButtonFocused();

    DropdownOptionMetadata activeInfoOption(int mouseX, int mouseY);

//? if >=1.21.9 {
    boolean handleDropdownClick(MouseButtonEvent event);

    boolean handleDropdownKey(KeyEvent event);
//?} else {
    boolean handleDropdownClick(double mouseX, double mouseY);

    boolean handleDropdownKey(int keyCode);
//?}

    boolean handleClosedDropdownKey(int keyCode);

    boolean handleDropdownChar(int codePoint);

    boolean handleDropdownScroll(double mouseX, double mouseY, double scrollY);

    boolean isPointInsideButton(double mouseX, double mouseY);

    boolean isPointInsideDropdown(double mouseX, double mouseY);

    void renderDropdown(KonfigRenderContext context, int mouseX, int mouseY);
}
//?}
