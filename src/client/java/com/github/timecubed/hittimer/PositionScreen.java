package com.github.timecubed.hittimer;

import com.github.timecubed.hittimer.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PositionScreen extends Screen {
	private final MinecraftClient mc = MinecraftClient.getInstance();
	private final int scaledWidth = mc.getWindow().getScaledWidth();
	private final int scaledHeight = mc.getWindow().getScaledHeight();
	private double scaledX = MainClient.tulipInstance.getDouble("x");
	private double scaledY = MainClient.tulipInstance.getDouble("y");
	private int damageTicks = 0;
	private int ticks = 0;
	private final int widthUI = 72;
	private final int heightUI = 25;
	
	private boolean dragging = false;
	
	private int diffX;
	private int diffY;
	
	protected PositionScreen(Text title) {
		super(title);
	}
	
	@Override
	public void init() {
		
		ButtonWidget done = ButtonWidget.builder(Text.of("Done"), button -> {
			mc.setScreen(new ConfigScreen(Text.of("config")));
			
			MainClient.tulipInstance.saveProperty("x", scaledX);
			MainClient.tulipInstance.saveProperty("y", scaledY);
			
			MainClient.tulipInstance.save();
		}).dimensions(this.width / 2 - 75, (int) (this.height / 1.1), 150, 20).build();
		
		this.addDrawableChild(done);
	}
	
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices); // transparent background reasons
		
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
		
		DrawableHelper.fill(matrices, (int) (scaledX * scaledWidth), (int) (scaledY * scaledHeight), (int) ((scaledX * scaledWidth) + widthUI), (int) ((scaledY * scaledHeight) + heightUI), ColorUtil.Colors.TRANSPARENT_BLACK.color);
		
		// Draw a gray box where the progress bar should go
		DrawableHelper.fill(matrices, (int) (scaledX * scaledWidth) + 3, (int) (scaledY * scaledHeight) + 19, (int) (scaledX * scaledWidth) + (widthUI - 3), (int) (scaledY * scaledHeight) + 23, ColorUtil.Colors.GRAY.color);
		
		// Draw the progress bar
		DrawableHelper.fill(matrices, (int) (scaledX * scaledWidth) + 3, (int) (scaledY * scaledHeight) + 19, (int) ((int) (scaledX * scaledWidth) + Math.max(((damageTicks / 10.0) * (widthUI - 3)), 3)), (int) (scaledY * scaledHeight) + 23, ColorUtil.blendColors(MainClient.tulipInstance.getInt("color1"), MainClient.tulipInstance.getInt("color2"), damageTicks / 10.0));
		
		mc.textRenderer.drawWithShadow(matrices, "No Target", (float) (int) (scaledX * scaledWidth) + 3, (float) (int) (scaledY * scaledHeight) + 3, ColorUtil.Colors.WHITE.color);
		
		drawHorizontalLine(matrices, 0, this.width, (int) (scaledY * scaledHeight), ColorUtil.Colors.WHITE.color);
		drawVerticalLine(matrices, (int) (scaledX * scaledWidth), 0, this.height, ColorUtil.Colors.WHITE.color);
		super.render(matrices, mouseX, mouseY, delta);
	}
	
	private boolean hasClicked = false;
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// I'm doing this like this just in case mouseClicked gets called
		// every tick the mouse is clicked for, in which case the UI thing won't move
		if (!hasClicked) {
			diffX = (int) (mouseX - (scaledX * scaledWidth));
			diffY = (int) (mouseY - (scaledY * scaledHeight));
			
			hasClicked = true;
		}
		
		if (isMouseHoveringOver((int) (scaledX * scaledWidth), (int) (scaledY * scaledHeight), (int) (scaledX * scaledWidth) + widthUI, (int) (scaledY * scaledHeight) + heightUI, mouseX, mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_1) {
			dragging = true;
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging) {
			scaledX = (mouseX - diffX) / scaledWidth;
			scaledY = (mouseY - diffY) / scaledHeight;
		}
		
		super.mouseMoved(mouseX, mouseY);
		
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		hasClicked = false;
		dragging = false;
		
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	private boolean isMouseHoveringOver(int x1, int y1, int x2, int y2, double mouseX, double mouseY) {
		return (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2);
	}
}