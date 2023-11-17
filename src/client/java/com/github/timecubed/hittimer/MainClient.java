package com.github.timecubed.hittimer;

import com.github.timecubed.hittimer.util.ColorUtil;
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
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.math.MatrixStack;
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
				tulipInstance.saveProperty("x", 0.5d);
				tulipInstance.saveProperty("y", 0.5d);
				tulipInstance.saveProperty("color1", ColorUtil.Colors.RED.color);
				tulipInstance.saveProperty("color2", ColorUtil.Colors.GREEN.color);
				
				// Try to load config file
				tulipInstance.load();
				
				if (tulipInstance.getDouble("x") > 1.0d || tulipInstance.getDouble("x") < 0.0d) {
					tulipInstance.saveProperty("x", 0.5d);
					tulipInstance.saveProperty("y", 0.5d);
					
					tulipInstance.save();
					
					MainServer.LOGGER.error("Double property 'X' or 'Y' was found to be above 1.0/below 0.0! Resetting X and Y back to the default values of 0.5");
				}
				
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
			width = Math.max(mc.textRenderer.getWidth(lastAttackedPlayer.getDisplayName().asOrderedText()) + 29, 92);
			
			// Draw a rectangle where all the UI elements will go
			DrawableHelper.fill(matrices, (int) (tulipInstance.getDouble("x") * scaledWidth), (int) (tulipInstance.getDouble("y") * scaledHeight), (int) (tulipInstance.getDouble("x") * scaledWidth) + width, (int) (tulipInstance.getDouble("y") * scaledHeight) + 26, ColorUtil.Colors.TRANSPARENT_BLACK.color);
			
			// Draw a gray box where the progress bar should go
			DrawableHelper.fill(matrices, (int) (tulipInstance.getDouble("x") * scaledWidth) + 26, (int) (tulipInstance.getDouble("y") * scaledHeight) + 19, (int) (tulipInstance.getDouble("x") * scaledWidth) + (width - 3), (int) (tulipInstance.getDouble("y") * scaledHeight) + 23, ColorUtil.Colors.GRAY.color);
			
			// Draw the progress bar
			DrawableHelper.fill(matrices, (int) (tulipInstance.getDouble("x") * scaledWidth) + 26, (int) (tulipInstance.getDouble("y") * scaledHeight) + 19, (int) ((tulipInstance.getDouble("x") * scaledWidth) + Math.max(((damageTicks / 10.0) * (width - 3)), 26)), (int) (tulipInstance.getDouble("y") * scaledHeight) + 23, ColorUtil.blendColors(tulipInstance.getInt("color1"), tulipInstance.getInt("color2"), damageTicks / 10.0));
			
			// Draw the player's username.
			mc.textRenderer.draw(matrices, lastAttackedPlayer.getDisplayName().getString(), (float) (tulipInstance.getDouble("x") * scaledWidth) + 26, (float) (tulipInstance.getDouble("y") * scaledHeight) + 3, ColorUtil.Colors.WHITE.color);
			
			matrices.push();
			
			// Scale the matrix stack to get smaller text
			matrices.scale(0.5f, 0.5f, 1.0f); // half scale text
			
			DrawableHelper.drawTextWithShadow(matrices, mc.textRenderer, "Damage ticks: " + (10 - damageTicks), ((int) (tulipInstance.getDouble("x") * scaledWidth) + 26) * 2, ((int) (tulipInstance.getDouble("y") * scaledHeight) + mc.textRenderer.getWrappedLinesHeight(lastAttackedPlayer.getDisplayName().getString(), mc.textRenderer.getWidth(lastAttackedPlayer.getDisplayName().getString())) + 4) * 2, ColorUtil.Colors.WHITE.color);
		
			matrices.pop();
			
			drawPlayerHead(matrices, lastAttackedPlayer, (int) (tulipInstance.getDouble("x") * scaledWidth) + 3, (int) (tulipInstance.getDouble("y") * scaledHeight) + 3, 20);
		});
		
		MainServer.LOGGER.info("Initialized hit timer successfully!");
	}
	
	// credits to BetyarBaszo for giving me this method
	public static void drawPlayerHead(MatrixStack matrices, PlayerEntity player, float x, float y, int size) {
		MinecraftClient mc = MinecraftClient.getInstance();
		
		// because intellij is stupid and annoying I have to go put this dumb check, so it can shut up
		// for once
		// IT'S COMPLAINING ABOUT THE GRAMMAR AS WELL
		if (mc.player != null && player != null) {
			GameProfile gameProfile = new GameProfile(player.getUuid(), player.getName().getString());
			PlayerListEntry playerListEntry = mc.player.networkHandler.getPlayerListEntry(gameProfile.getId());
			boolean bl22 = LivingEntityRenderer.shouldFlipUpsideDown(player);
			boolean bl3 = player.isPartVisible(PlayerModelPart.HAT);
			
			// again intellij won't shut up but this time it's really, really stupid and won't shut up
			// about my grammar mistakes, and also it won't shut up about this dumb warning but this would
			// never throw a null pointer exception because mc.player is not null anyway and the target isn't
			// null either. I'm not going to bother doing anything about it either
			// nvm I have to, it's stopping me from pushing. for god's sake, intellij this isn't an issue I
			// swear to you. it keeps telling me what to do with my grammar man shut up
			if (playerListEntry != null)
				RenderSystem.setShaderTexture(0, playerListEntry.getSkinTexture());
			PlayerSkinDrawer.draw(matrices, (int) x, (int) y, size, bl3, bl22);
		}
	}
}