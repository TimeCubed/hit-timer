package com.github.timecubed.hittimer;

import io.github.timecubed.tulip.TulipConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class MainClient implements ClientModInitializer {
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	private PlayerEntity lastAttackedPlayer;
	private int damageTicks = 0;
	
	// Tulip instance for config stuff
	public static TulipConfigManager tulipInstance = new TulipConfigManager("hit-timer", false);
	
	@Override
	public void onInitializeClient() {
		// Set up config command
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("hittimer").executes(context -> {
			mc.send(() -> mc.setScreen(new ConfigScreen(Text.of("config"))));
			return 1;
		})));
		
		// This is apparently not for the client player but for *all* players
		// So the `player != mc.player` check is in fact vital
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player != mc.player) {
				return ActionResult.PASS;
			}
			if (entity.isPlayer()) {
				lastAttackedPlayer = (PlayerEntity) entity;
			}
			
			return ActionResult.PASS;
		});
		
		// I hate that I have to do this disgusting solution, but it is what it is
		final boolean[] hasRendered = {false};
		
		HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
			int scaledHeight = mc.getWindow().getScaledHeight();
			int scaledWidth = mc.getWindow().getScaledWidth();
			
			// Dear god I'm unhappy with this solution. It's so awful
			if (!hasRendered[0]) {
				// Tulip config setup
				tulipInstance.saveProperty("x", scaledHeight / 2);
				tulipInstance.saveProperty("y", scaledWidth / 2);
				
				// Try to load config file
				tulipInstance.load();
				
				hasRendered[0] = true;
			}
			
			// This is so Intellij stops complaining and giving me warnings
			if (mc.player == null) {
				return;
			}
			
			// Check if the player is dead, if so, reset the last attacked player
			if (mc.player.isDead()) {
				lastAttackedPlayer = null;
			}
			
			// Neat trick with the last check, if the player dies this check will
			// go off, and we won't actually render anything. Super cool
			if (lastAttackedPlayer == null) {
				return;
			} else {
				// This is 10 - hurtTime because the progress bar would actually
				// go backwards (from full to empty) instead of going forwards
				// (from empty to full). This solves that issue
				damageTicks = 10 - lastAttackedPlayer.hurtTime;
			}
			
			// This check has to be after we check if the lastAttackedPlayer was null
			// because it *may* be null if we check earlier (e.g. if the player dies
			// and the earlier check unsets the variable and *then* we check if the
			// last attacked player was dead, which would result in a crash)
			if (lastAttackedPlayer.isDead()) {
				lastAttackedPlayer = null;
				return; // Return so we don't render stuff and access null values and such
			}
			
			// Check if the chat screen is up, if it is we won't render anything
			// This is to prevent some poor visibility problems while reading the chat.
			if (mc.currentScreen instanceof ChatScreen) {
				return;
			}
			
			// 191 is approximately 75% of 255 (255 * 75 / 100 = 191.25)
			// This is to get a transparent black color with 75% opacity
			int transparentBlack = rgba(25, 25, 25, 191);
			
			int width;
			width = Math.max(mc.textRenderer.getWidth(lastAttackedPlayer.getDisplayName()) + 3, 72);
			
			// Draw a rectangle where all the UI elements will go
			DrawableHelper.fill(matrices, tulipInstance.getInt("x"), tulipInstance.getInt("y"), tulipInstance.getInt("x") + width, tulipInstance.getInt("y") + 25, transparentBlack);
			
			// Draw a gray box where the progress bar should go
			DrawableHelper.fill(matrices, tulipInstance.getInt("x") + 3, tulipInstance.getInt("y") + 19, tulipInstance.getInt("x") + (width - 3), tulipInstance.getInt("y") + 23, rgba(200, 200, 200, 255));
			
			// Draw the progress bar
			DrawableHelper.fill(matrices, tulipInstance.getInt("x") + 3, tulipInstance.getInt("y") + 19, (int) (tulipInstance.getInt("x") + Math.max(((damageTicks / 10.0) * (width - 3)), 3)), tulipInstance.getInt("y") + 23, blendColors(rgba(255, 0, 0, 255), rgba(0, 255, 0, 255), damageTicks / 10.0));
			
			// Draw the player's username.
			mc.textRenderer.draw(matrices, lastAttackedPlayer.getDisplayName().getString(), (float) tulipInstance.getInt("x") + 3, (float) tulipInstance.getInt("y") + 3, rgba(255, 255, 255, 255));
		});
	}
	
	private static int rgba(int r, int g, int b, int a) {
		return (a << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
	}
	
	// thanks, ChatGPT, never could've done this without ya
	
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