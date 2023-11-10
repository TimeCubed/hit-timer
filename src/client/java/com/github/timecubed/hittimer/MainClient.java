package com.github.timecubed.hittimer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

public class MainClient implements ClientModInitializer {
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	private PlayerEntity lastAttackedPlayer;
	private int damageTicks = 0;
	
	@Override
	public void onInitializeClient() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity.isPlayer()) {
				lastAttackedPlayer = (PlayerEntity) entity;
			}
			
			return ActionResult.PASS;
		});
		
		HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
			if (lastAttackedPlayer == null) { // TODO: remove this
				if (damageTicks >= 10) {
					damageTicks = 0;
				} else {
					damageTicks++;
				}
			} else {
				damageTicks = lastAttackedPlayer.hurtTime;
			}
			
			int scaledHeight = mc.getWindow().getScaledHeight();
			int scaledWidth = mc.getWindow().getScaledWidth();
			
			int transparentBlack = rgba(25, 25, 25, 191); // 191 is approximately 75% of 255
														             // This is to get a transparent black color with 75% opacity
			int width;
			
			if (lastAttackedPlayer != null) { // TODO: remove this
				width = Math.max(mc.textRenderer.getWidth(lastAttackedPlayer.getDisplayName()) + 3, 72);
			} else {
				width = mc.textRenderer.getWidth("PLACEHOLDERPLACEHOLDERPLACEHOLDER") + 6;
			}
			
			// Draw a rectangle where all of the UI elements will go
			// TODO: Make the position configurable
			DrawableHelper.fill(matrices, scaledWidth / 2, scaledHeight / 2, scaledWidth / 2 + width, scaledHeight / 2 + 25, transparentBlack);
			
			// Draw a gray box where the progress bar should go
			DrawableHelper.fill(matrices, scaledWidth / 2 + 3, scaledHeight / 2 + 19, scaledWidth / 2 + (width - 3), scaledHeight / 2 + 23, rgba(200, 200, 200, 255));
			
			// Draw the progress bar
			DrawableHelper.fill(matrices, scaledWidth / 2 + 3, scaledHeight / 2 + 19, (int) (scaledWidth / 2 + Math.max(((damageTicks / 10.0) * (width - 3)), 3)), scaledHeight / 2 + 23, blendColors(rgba(255, 0, 0, 255), rgba(0, 255, 0, 255), damageTicks / 10.0));
			
			// Draw the player's username if we have attacked a player previously.
			mc.textRenderer.drawWithShadow(matrices, (lastAttackedPlayer != null ? lastAttackedPlayer.getDisplayName().toString() : "PLACEHOLDERPLACEHOLDERPLACEHOLDER"), (float) scaledWidth / 2 + 3, (float) scaledHeight / 2 + 3, rgba(255, 255, 255, 255));
		});
	}
	
	private static int rgba(int r, int g, int b, int a) {
		return (a << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
	}
	
	// thanks chatgpt, never could've done this without ya
	
	private static int blendColors(int color1, int color2, double progression) {
		float[] hsl1 = rgbToHsl(color1);
		float[] hsl2 = rgbToHsl(color2);
		
		float blendedH = interpolateHue(hsl1[0], hsl2[0], progression);
		float blendedS = interpolate(hsl1[1], hsl2[1], progression);
		float blendedL = interpolate(hsl1[2], hsl2[2], progression);
		
		return hslToRgb(blendedH, blendedS, blendedL);
	}
	
	private static float[] rgbToHsl(int color) {
		float r = ((color >> 16) & 255) / 255f;
		float g = ((color >> 8) & 255) / 255f;
		float b = (color & 255) / 255f;
		
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float h, s, l;
		
		l = (max + min) / 2;
		
		if (max == min) {
			h = s = 0; // achromatic
		} else {
			float d = max - min;
			s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
			
			if (max == r) {
				h = (g - b) / d + (g < b ? 6 : 0);
			} else if (max == g) {
				h = (b - r) / d + 2;
			} else {
				h = (r - g) / d + 4;
			}
			
			h /= 6;
		}
		
		return new float[]{h, s, l};
	}
	
	private static int hslToRgb(float h, float s, float l) {
		float r, g, b;
		
		if (s == 0) {
			r = g = b = l; // achromatic
		} else {
			float q = l < 0.5 ? l * (1 + s) : l + s - l * s;
			float p = 2 * l - q;
			r = hueToRgb(p, q, h + 1f / 3);
			g = hueToRgb(p, q, h);
			b = hueToRgb(p, q, h - 1f / 3);
		}
		
		int red = Math.round(r * 255);
		int green = Math.round(g * 255);
		int blue = Math.round(b * 255);
		
		return rgba(red, green, blue, 255);
	}
	
	private static float hueToRgb(float p, float q, float t) {
		if (t < 0) t += 1;
		if (t > 1) t -= 1;
		if (t < 1f / 6) return p + (q - p) * 6 * t;
		if (t < 1f / 2) return q;
		if (t < 2f / 3) return p + (q - p) * (2f / 3 - t) * 6;
		return p;
	}
	
	private static float interpolate(float value1, float value2, double progression) {
		return (float) (value1 + (value2 - value1) * progression);
	}
	
	private static float interpolateHue(float hue1, float hue2, double progression) {
		if (Math.abs(hue1 - hue2) > 0.5f) {
			if (hue1 > hue2) {
				hue2 += 1.0f;
			} else {
				hue1 += 1.0f;
			}
		}
		
		return interpolate(hue1, hue2, progression);
	}
}