package com.github.timecubed.hittimer;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.timecubed.tulip.TulipConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

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
			
			if (lastAttackedPlayer == null) {
				damageTicks = 10;
			} else {
				// This is 10 - hurtTime because the progress bar would actually
				// go backwards (from full to empty) instead of going forwards
				// (from empty to full). This solves that issue
				damageTicks = 10 - lastAttackedPlayer.hurtTime;
			}
			
			// 191 is approximately 75% of 255 (255 * 75 / 100 = 191.25)
			// This is to get a transparent black color with 75% opacity
			
			int transparentBlack = rgba(25, 25, 25, 191);
			
			int width;
			
			//width = Math.max(mc.textRenderer.getWidth(lastAttackedPlayer.getDisplayName()) + 3, 72);
			width = 72;
			
			// Draw a rectangle where all of the UI elements will go
			// TODO: Make the position configurable
			lekeretitettkocka(matrices,tulipInstance.getInt("x"), tulipInstance.getInt("y"), tulipInstance.getInt("x") + width, tulipInstance.getInt("y") + 25,3, 10, transparentBlack);
			//DrawableHelper.fill(matrices, tulipInstance.getInt("x"), tulipInstance.getInt("y"), tulipInstance.getInt("x") + width, tulipInstance.getInt("y") + 25, transparentBlack);
			
			// Draw a gray box where the progress bar should go
			//kockafej(matrices, );
			lekeretitettkocka(matrices, tulipInstance.getInt("x") + 3, tulipInstance.getInt("y") + 19, tulipInstance.getInt("x") + (width - 3), tulipInstance.getInt("y") + 23, 3, 10,rgba(200, 200, 200, 255));
			//DrawableHelper.fill(matrices, tulipInstance.getInt("x") + 3, tulipInstance.getInt("y") + 19, tulipInstance.getInt("x") + (width - 3), tulipInstance.getInt("y") + 23, rgba(200, 200, 200, 255));
			
			// Draw the progress bar
			DrawableHelper.fill(matrices, tulipInstance.getInt("x") + 3, tulipInstance.getInt("y") + 19, (int) (tulipInstance.getInt("x") + Math.max(((damageTicks / 10.0) * (width - 3)), 3)), tulipInstance.getInt("y") + 23, blendColors(rgba(255, 0, 0, 255), rgba(0, 255, 0, 255), damageTicks / 10.0));
			
			// Draw the player's username.
			
			mc.textRenderer.draw(matrices, "No Target", (float) tulipInstance.getInt("x") + 3, (float) tulipInstance.getInt("y") + 3, rgba(255, 255, 255, 255));
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
	public static void lekeretitettkocka(@NotNull MatrixStack matrices, double fromX, double fromY, double toX, double toY, double rad, double samples, int c) {
		int color = c;
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		float f = (float) (color >> 24 & 255) / 255.0F;
		float g = (float) (color >> 16 & 255) / 255.0F;
		float h = (float) (color >> 8 & 255) / 255.0F;
		float k = (float) (color & 255) / 255.0F;
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);

		abelsocucc(matrix, g, h, k, f, fromX, fromY, toX, toY, rad, samples);

		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}
	public static void abelsocucc(Matrix4f matrix, float cr, float cg, float cb, float ca, double fromX, double fromY, double toX, double toY, double rad, double samples) {
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

		double toX1 = toX - rad;
		double toY1 = toY - rad;
		double fromX1 = fromX + rad;
		double fromY1 = fromY + rad;
		double[][] map = new double[][]{new double[]{toX1, toY1}, new double[]{toX1, fromY1}, new double[]{fromX1, fromY1}, new double[]{fromX1, toY1}};
		for (int i = 0; i < 4; i++) {
			double[] current = map[i];
			for (double r = i * 90d; r < (360 / 4d + i * 90d); r += (90 / samples)) {
				float rad1 = (float) Math.toRadians(r);
				float sin = (float) (Math.sin(rad1) * rad);
				float cos = (float) (Math.cos(rad1) * rad);
				bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr, cg, cb, ca).next();
			}
		}
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
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