package com.github.timecubed.hittimer;

import com.github.timecubed.hittimer.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class PositionScreen extends Screen {
	private int x = MainClient.tulipInstance.getInt("x"),
			y = MainClient.tulipInstance.getInt("y"),
			damageTicks = 0,
			ticks = 0;
	
	private final MinecraftClient mc = MinecraftClient.getInstance();
	
	protected PositionScreen(Text title) {
		super(title);
	}
	
	@Override
	public void init() {
		ButtonWidget done = ButtonWidget.builder(Text.of("Done"), button -> {
			mc.setScreen(new ConfigScreen(Text.of("config")));
			
			MainClient.tulipInstance.save();
		}).dimensions(this.width / 2 - 75, (int) (this.height / 1.1), 150, 20).build();
		
		this.addDrawableChild(done);
	}
	
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices); // transparent background reasons
		
		int width = 72;
		
		// Slowing down the progress bar a bit to make it a little more
		// visible in the config screen
		
		ticks++;
		
		if (ticks >= 5) {
			damageTicks++;
			if (damageTicks > 10) {
				damageTicks = 0;
			}
			
			ticks = 0;
		}
		
		DrawableHelper.fill(matrices, x, y, x + width, y + 25, ColorUtil.Colors.TRANSPARENT_BLACK.color);
		
		// Draw a gray box where the progress bar should go
		DrawableHelper.fill(matrices, x + 3, y + 19, x + (width - 3), y + 23, ColorUtil.Colors.GRAY.color);
		
		// Draw the progress bar
		DrawableHelper.fill(matrices, x + 3, y + 19, (int) (x + Math.max(((damageTicks / 10.0) * (width - 3)), 3)), y + 23, ColorUtil.blendColors(MainClient.tulipInstance.getInt("color1"), MainClient.tulipInstance.getInt("color2"), damageTicks / 10.0));
		
		mc.textRenderer.drawWithShadow(matrices, "No Target", (float) x + 3, (float) y + 3, ColorUtil.Colors.WHITE.color);
		
		drawHorizontalLine(matrices, 0, this.width, mouseY, ColorUtil.Colors.WHITE.color);
		drawVerticalLine(matrices, mouseX, 0, this.height, ColorUtil.Colors.WHITE.color);
		super.render(matrices, mouseX, mouseY, delta);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!super.mouseClicked(mouseX, mouseY, button)) {
			MainClient.tulipInstance.saveProperty("x", (int) mouseX);
			MainClient.tulipInstance.saveProperty("y", (int) mouseY);
			
			x = (int) mouseX;
			y = (int) mouseY;
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	// Have to use this again, should probably put it in an utilities class somewhere
	// instead of copy-pasting it everywhere
	
	private static int rgba(int r, int g, int b, int a) {
		return (a << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
	}
	
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
