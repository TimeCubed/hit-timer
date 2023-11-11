package com.github.timecubed.hittimer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	
	public ConfigScreen(Text title) {
		super(title);
	}
	
	@Override
	public void init() {
		ButtonWidget position = ButtonWidget.builder(Text.of("Edit position"), button ->
			mc.setScreen(new PositionScreen(Text.of("positionScreen")))
		).dimensions(this.width / 2 - 75, this.height / 2, 150, 20).build();
		this.addDrawableChild(position);
		
		ButtonWidget done = ButtonWidget.builder(Text.of("Done"), button ->
			mc.setScreen(null)
		).dimensions(this.width / 2 - 75, (int) (this.height / 1.1), 150, 20).build();
		this.addDrawableChild(done);
	}
	
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		
		super.render(matrices, mouseX, mouseY, delta);
	}
}