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
	
	
	private final int scaledHeight = mc.getWindow().getScaledHeight();
	private final int scaledWidth = mc.getWindow().getScaledWidth();
	
	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("hittimer").executes(context -> {
			mc.send(() -> mc.setScreen(new ConfigScreen(Text.of("config"))));
			return 1;
		})));
		
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player != mc.player) {
				return ActionResult.PASS;
			}
			if (entity.isPlayer()) {
				lastAttackedPlayer = (PlayerEntity) entity;
			}
			
			return ActionResult.PASS;
		});
		
		HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
			loadConfigs();
			
			if (mc.player == null) {
				return;
			}
			
			if (mc.player.isDead()) {
				lastAttackedPlayer = null;
			}
			
			if (lastAttackedPlayer == null) {
				return;
			} else {
				damageTicks = 10 - lastAttackedPlayer.hurtTime;
			}
			
			if (lastAttackedPlayer.isDead()) {
				lastAttackedPlayer = null;
				return;
			}
			
			if (mc.currentScreen instanceof ChatScreen) {
				return;
			}
			
			if (tulipInstance.getBoolean("render-hud")) {
				renderTargetHUD(matrices, tickDelta);
				return;
			}
			
			renderCircularHUD(matrices, tickDelta);
			});
		
		MainServer.LOGGER.info("Initialized hit timer successfully!");
	}
	
	private void renderCircularHUD(MatrixStack matrices, float tickDelta) {
		// TODO: figure out how to do this pls
	}
	
	private void renderTargetHUD(MatrixStack matrices, float tickDelta) {
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
		matrices.scale(0.5f, 0.5f, 1.0f);
		
		DrawableHelper.drawTextWithShadow(matrices, mc.textRenderer, "Damage ticks: " + (10 - damageTicks), ((int) (tulipInstance.getDouble("x") * scaledWidth) + 26) * 2, ((int) (tulipInstance.getDouble("y") * scaledHeight) + mc.textRenderer.getWrappedLinesHeight(lastAttackedPlayer.getDisplayName().getString(), mc.textRenderer.getWidth(lastAttackedPlayer.getDisplayName().getString())) + 4) * 2, ColorUtil.Colors.WHITE.color);
		
		matrices.pop();
		
		drawPlayerHead(matrices, lastAttackedPlayer, (int) (tulipInstance.getDouble("x") * scaledWidth) + 3, (int) (tulipInstance.getDouble("y") * scaledHeight) + 3, 20);
	}
	
	boolean configsLoaded = false;
	private void loadConfigs() {
		if (configsLoaded) {
			return;
		}
		
		// Tulip config setup
		tulipInstance.saveProperty("x", 0.5d);
		tulipInstance.saveProperty("y", 0.5d);
		tulipInstance.saveProperty("color1", ColorUtil.Colors.RED.color);
		tulipInstance.saveProperty("color2", ColorUtil.Colors.GREEN.color);
		tulipInstance.saveProperty("render-hud", true);
		
		// Try to load config file
		tulipInstance.load();
		
		if (tulipInstance.getDouble("x") > 1.0d || tulipInstance.getDouble("x") < 0.0d || tulipInstance.getDouble("y") > 1.0d || tulipInstance.getDouble("y") < 0.0d) {
			tulipInstance.saveProperty("x", 0.5d);
			tulipInstance.saveProperty("y", 0.5d);
			
			tulipInstance.save();
			
			MainServer.LOGGER.warn("Double property 'X' or 'Y' was found to be above 1.0/below 0.0! Resetting X and Y back to the default values of 0.5");
		}
		
		configsLoaded = true;
	}
	
	// credits to BetyarBaszo for giving me this method
	public static void drawPlayerHead(MatrixStack matrices, PlayerEntity player, float x, float y, int size) {
		MinecraftClient mc = MinecraftClient.getInstance();
		
		if (mc.player != null && player != null) {
			GameProfile gameProfile = new GameProfile(player.getUuid(), player.getName().getString());
			PlayerListEntry playerListEntry = mc.player.networkHandler.getPlayerListEntry(gameProfile.getId());
			boolean bl22 = LivingEntityRenderer.shouldFlipUpsideDown(player);
			boolean bl3 = player.isPartVisible(PlayerModelPart.HAT);
			
			if (playerListEntry != null)
				RenderSystem.setShaderTexture(0, playerListEntry.getSkinTexture());
			PlayerSkinDrawer.draw(matrices, (int) x, (int) y, size, bl3, bl22);
		}
	}
}