package com.github.timecubed.hittimer;

import com.github.timecubed.hittimer.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

public class ConfigScreen extends Screen {
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	
	public ConfigScreen(Text title) {
		super(title);
	}
	
	@Override
	public void init() {
		ButtonWidget position = ButtonWidget.builder(Text.of("Edit position"), button ->
			mc.setScreen(new PositionScreen(Text.of("positionScreen")))
		).dimensions(this.width / 2 - 75, this.height / 2 - 25, 150, 20).build();
		
		TextFieldWidget color1 = new TextFieldWidget(
				mc.textRenderer,
				this.width / 2,
				this.height / 2,
				75,
				20,
				Text.of("intfield")
		);
		
		TextFieldWidget color2 = new TextFieldWidget(
				mc.textRenderer,
				this.width / 2,
				this.height / 2 + 25,
				75,
				20,
				Text.of("intfield")
		);
		
		ButtonWidget done = ButtonWidget.builder(Text.of("Done"), button -> {
			mc.setScreen(null);
			
			Color c1;
			Color c2;
			
			// Parse color fields
			try {
				c1 = Color.decode(color1.getText());
				c2 = Color.decode(color2.getText());
			} catch (NumberFormatException e) {
				if (mc.player != null) {
					mc.player.sendMessage(Text.of("That was not a correct hex color code!"));
				}
				return;
			}
			
			MainClient.tulipInstance.saveProperty("color1", c1.getRGB());
			MainClient.tulipInstance.saveProperty("color2", c2.getRGB());
			
			MainClient.tulipInstance.save();
		}).dimensions(this.width / 2 - 75, (int) (this.height / 1.1), 150, 20).build();
		
		this.addDrawableChild(color1);
		this.addDrawableChild(color2);
		this.addDrawableChild(position);
		this.addDrawableChild(done);
	}
	
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackgroundTexture(matrices);
		
		mc.textRenderer.drawWithShadow(matrices, "Color 1: ", (float) this.width / 2 - 15 - mc.textRenderer.getWidth("Color 1: "), (float) this.height / 2 + 5, ColorUtil.Colors.WHITE.color);
		mc.textRenderer.drawWithShadow(matrices, "Color 2: ", (float) this.width / 2 - 15 - mc.textRenderer.getWidth("Color 2: "), (float) this.height / 2 + 30, ColorUtil.Colors.WHITE.color);
		
		super.render(matrices, mouseX, mouseY, delta);
	}
}