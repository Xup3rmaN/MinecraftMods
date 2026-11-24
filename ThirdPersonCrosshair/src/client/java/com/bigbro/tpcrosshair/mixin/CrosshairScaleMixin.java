package com.bigbro.tpcrosshair.mixin;

import com.bigbro.tpcrosshair.TpCrosshairConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Прицел рисуется своим размером в пикселях экрана, независимо от масштаба GUI. */
@Mixin(Gui.class)
public class CrosshairScaleMixin {
	@Unique
	private boolean tpcrosshair$scaled;

	@Inject(method = "renderCrosshair", at = @At("HEAD"))
	private void tpcrosshair$scaleStart(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		float factor = TpCrosshairConfig.get().crosshairSize / guiScale;
		tpcrosshair$scaled = factor != 1f;
		if (!tpcrosshair$scaled) return;
		float cx = graphics.guiWidth() / 2f, cy = graphics.guiHeight() / 2f;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.scale(factor, factor);
		pose.translate(-cx, -cy);
	}

	@Inject(method = "renderCrosshair", at = @At("RETURN"))
	private void tpcrosshair$scaleEnd(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		if (tpcrosshair$scaled) graphics.pose().popMatrix();
		tpcrosshair$scaled = false;
	}
}
