package net.xenrao.playerplaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PlayerPlacesMod.MODID, value = Dist.CLIENT)
public class TrackingOverlay {

	@SubscribeEvent
	public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
		if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type())
			return;

		Location tracked = ClientLocationData.getTrackedLocation();
		if (tracked == null)
			return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null)
			return;

		GuiGraphics graphics = event.getGuiGraphics();
		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();

		String playerDim = player.level().dimension().location().toString();
		boolean sameDimension = playerDim.equals(tracked.getDimension());

		int y = screenHeight - 55;

		if (!sameDimension) {
			// Different dimension
			String dimName = formatDimension(tracked.getDimension());
			String line1 = ">> " + tracked.getName() + " <<";
			String line2 = "Go to: " + dimName;
			String line3 = "X:" + tracked.getX() + " Y:" + tracked.getY() + " Z:" + tracked.getZ();

			drawCenteredStringWithShadow(graphics, mc, line1, screenWidth / 2, y - 12, 0xFFAA00);
			drawCenteredStringWithShadow(graphics, mc, line2, screenWidth / 2, y, 0xFF5555);
			drawCenteredStringWithShadow(graphics, mc, line3, screenWidth / 2, y + 12, 0xAAAAAA);
		} else {
			// Same dimension - show direction and distance
			double dx = tracked.getX() - player.getX();
			double dz = tracked.getZ() - player.getZ();
			double distance = Math.sqrt(dx * dx + dz * dz);

			String direction = getDirection(player.getYRot(), dx, dz);
			String distStr = String.valueOf((int) distance);

			if (distance < 10) {
				String line1 = ">> " + tracked.getName() + " <<";
				String line2 = "You have arrived!";
				drawCenteredStringWithShadow(graphics, mc, line1, screenWidth / 2, y - 6, 0x55FF55);
				drawCenteredStringWithShadow(graphics, mc, line2, screenWidth / 2, y + 6, 0x55FF55);
			} else {
				String line1 = ">> " + tracked.getName() + " <<";
				String line2 = direction + " | " + distStr + " blocks";
				String line3 = "X:" + tracked.getX() + " Y:" + tracked.getY() + " Z:" + tracked.getZ();

				drawCenteredStringWithShadow(graphics, mc, line1, screenWidth / 2, y - 12, 0xFFAA00);
				drawCenteredStringWithShadow(graphics, mc, line2, screenWidth / 2, y, 0x55FFFF);
				drawCenteredStringWithShadow(graphics, mc, line3, screenWidth / 2, y + 12, 0xAAAAAA);
			}
		}

		// Stop tracking hint
		drawCenteredStringWithShadow(graphics, mc, "[H to manage]", screenWidth / 2, y + 24, 0x555555);
	}

	private static String getDirection(float playerYaw, double dx, double dz) {
		// Angle from player to target in degrees
		double targetAngle = Math.toDegrees(Math.atan2(-dx, dz));
		double relativeAngle = Mth.wrapDegrees(targetAngle - playerYaw);

		// Convert to compass + arrow
		if (relativeAngle >= -22.5 && relativeAngle < 22.5) return "↑ Ahead";
		if (relativeAngle >= 22.5 && relativeAngle < 67.5) return "↗ Front-Left";
		if (relativeAngle >= 67.5 && relativeAngle < 112.5) return "← Left";
		if (relativeAngle >= 112.5 && relativeAngle < 157.5) return "↙ Back-Left";
		if (relativeAngle >= -67.5 && relativeAngle < -22.5) return "↘ Front-Right";
		if (relativeAngle >= -112.5 && relativeAngle < -67.5) return "→ Right";
		if (relativeAngle >= -157.5 && relativeAngle < -112.5) return "↖ Back-Right";
		return "↓ Behind";
	}

	private static String formatDimension(String dim) {
		if (dim.contains("overworld")) return "Overworld";
		if (dim.contains("the_nether")) return "Nether";
		if (dim.contains("the_end")) return "The End";
		return dim;
	}

	private static void drawCenteredStringWithShadow(GuiGraphics graphics, Minecraft mc, String text, int x, int y, int color) {
		int width = mc.font.width(text);
		// Dark background for readability
		graphics.fill(x - width / 2 - 3, y - 2, x + width / 2 + 3, y + 10, 0x80000000);
		graphics.drawCenteredString(mc.font, text, x, y, color);
	}
}