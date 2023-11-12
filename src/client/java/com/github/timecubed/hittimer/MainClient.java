package com.github.timecubed.hittimer;

import com.github.timecubed.hittimer.util.ColorUtil;
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
			
			int width;
			width = Math.max(mc.textRenderer.getWidth(lastAttackedPlayer.getDisplayName()) + 3, 72);
			
			// Draw a rectangle where all the UI elements will go
			DrawableHelper.fill(matrices, tulipInstance.getInt("x"), tulipInstance.getInt("y"), tulipInstance.getInt("x") + width, tulipInstance.getInt("y") + 25, ColorUtil.Colors.TRANSPARENT_BLACK.color);
			
			// Draw a gray box where the progress bar should go
			DrawableHelper.fill(matrices, tulipInstance.getInt("x") + 3, tulipInstance.getInt("y") + 19, tulipInstance.getInt("x") + (width - 3), tulipInstance.getInt("y") + 23, ColorUtil.Colors.GRAY.color);
			
			// Draw the progress bar
			DrawableHelper.fill(matrices, tulipInstance.getInt("x") + 3, tulipInstance.getInt("y") + 19, (int) (tulipInstance.getInt("x") + Math.max(((damageTicks / 10.0) * (width - 3)), 3)), tulipInstance.getInt("y") + 23, ColorUtil.blendColors(tulipInstance.getInt("color1"), tulipInstance.getInt("color2"), damageTicks / 10.0));
			
			// Draw the player's username.
			mc.textRenderer.draw(matrices, lastAttackedPlayer.getDisplayName().getString(), (float) tulipInstance.getInt("x") + 3, (float) tulipInstance.getInt("y") + 3, ColorUtil.Colors.WHITE.color);
		});
		
		MainServer.LOGGER.info("Initialized hit timer successfully!");
	}
}