package com.github.timecubed.hittimer.util;

public class ColorUtil {
	public static final int TRANSPARENT_BLACK = rgba(25, 25, 25, 191);
	public static final int RED = rgba(255, 0, 0, 255);
	public static final int GREEN = rgba(0, 255, 0, 255);
	public static final int GRAY = rgba(200, 200, 200, 255);
	public static final int WHITE = rgba(255, 255, 255, 255);
	
	public static int rgba(int r, int g, int b, int a) {
		return (a << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
	}
	
	public static int[] rgbaToRGB(int rgba) {
		int r = (rgba >> 16) & 0xFF;
		int g = (rgba >> 8) & 0xFF;
		int b = rgba & 0xFF;
		
		return new int[] {r, g, b};
	}
	
	public static int blendColors(int color1, int color2, double progression) {
		float[] hsl1 = rgbToHsl(color1);
		float[] hsl2 = rgbToHsl(color2);
		
		float blendedH = interpolateHue(hsl1[0], hsl2[0], progression);
		float blendedS = interpolate(hsl1[1], hsl2[1], progression);
		float blendedL = interpolate(hsl1[2], hsl2[2], progression);
		
		return hslToRgb(blendedH, blendedS, blendedL);
	}
	
	public static float[] rgbToHsl(int color) {
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
	
	public static int hslToRgb(float h, float s, float l) {
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
	
	public static float hueToRgb(float p, float q, float t) {
		if (t < 0) t += 1;
		if (t > 1) t -= 1;
		if (t < 1f / 6) return p + (q - p) * 6 * t;
		if (t < 1f / 2) return q;
		if (t < 2f / 3) return p + (q - p) * (2f / 3 - t) * 6;
		return p;
	}
	
	public static float interpolate(float value1, float value2, double progression) {
		return (float) (value1 + (value2 - value1) * progression);
	}
	
	public static float interpolateHue(float hue1, float hue2, double progression) {
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
