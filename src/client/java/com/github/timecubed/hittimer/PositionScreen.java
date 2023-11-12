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
		
		if (ticks >= 3) {
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
}
