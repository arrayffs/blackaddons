package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigManager.data.hideStatusEffects) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemHotbar", at = @At(value = "HEAD"), cancellable = true)
    private void renderItemHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigManager.data.hudOptions.hotbar.enabled)
            ci.cancel();
    }

    @Inject(method = "renderHearts", at = @At(value = "HEAD"), cancellable = true)
    private void renderHearts(GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, float f, int m, int n, int o, boolean bl, CallbackInfo ci) {
        if (ConfigManager.data.hudOptions.hotbar.enabled)
            ci.cancel();
    }

    @Inject(method = "renderArmor", at = @At(value = "HEAD"), cancellable = true)
    private static void renderArmor(GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, CallbackInfo ci) {
        if (ConfigManager.data.hudOptions.hotbar.enabled)
            ci.cancel();
    }

    @Inject(method = "renderFood", at = @At(value = "HEAD"), cancellable = true)
    private void renderFood(GuiGraphics guiGraphics, Player player, int i, int j, CallbackInfo ci) {
        if (ConfigManager.data.hudOptions.hotbar.enabled)
            ci.cancel();
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At(value = "HEAD"), cancellable = true)
    private void renderHotbarAndDecorations(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigManager.data.hudOptions.hotbar.enabled)
            ci.cancel();
    }

}
