package net.xenrao.playerplaces;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class PlacesKeyBindings {
	public static final KeyMapping OPEN_GUI = new KeyMapping(
			"key.player_places.open_gui",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_H,
			"key.categories.player_places"
	);

	@Mod.EventBusSubscriber(modid = PlayerPlacesMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ModBusEvents {
		@SubscribeEvent
		public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
			event.register(OPEN_GUI);
		}
	}

	@Mod.EventBusSubscriber(modid = PlayerPlacesMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class ForgeBusEvents {
		@SubscribeEvent
		public static void onKeyInput(InputEvent.Key event) {
			if (OPEN_GUI.consumeClick()) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.screen == null) {
					mc.setScreen(new LocationListScreen());
				}
			}
		}
	}
}