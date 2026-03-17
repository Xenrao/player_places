package net.xenrao.playerplaces;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = PlayerPlacesMod.MODID)
public class PlacesEvents {

	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent event) {
		LocationManager.init(event.getServer());
		PlayerPlacesMod.LOGGER.info("PlayerPlaces LocationManager initialized");
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		LocationManager.shutdown();
		PlayerPlacesMod.LOGGER.info("PlayerPlaces LocationManager shutdown");
	}

	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			syncAllToPlayer(serverPlayer);
		}
	}

	public static void syncAllToPlayer(ServerPlayer player) {
		LocationManager manager = LocationManager.get();
		if (manager == null)
			return;
		PlayerPlacesMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player),
				new SyncLocationsPacket(manager.getAllLocations()));
		PlayerPlacesMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player),
				new SyncCategoriesPacket(manager.getCategories()));
		PlayerPlacesMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player),
				new SyncConfigPacket(manager.getMaxLocationsPerPlayer()));
	}

	public static void syncAllToEveryone(MinecraftServer server) {
		LocationManager manager = LocationManager.get();
		if (manager == null)
			return;
		PlayerPlacesMod.PACKET_HANDLER.send(
				PacketDistributor.ALL.noArg(),
				new SyncLocationsPacket(manager.getAllLocations()));
		PlayerPlacesMod.PACKET_HANDLER.send(
				PacketDistributor.ALL.noArg(),
				new SyncCategoriesPacket(manager.getCategories()));
		PlayerPlacesMod.PACKET_HANDLER.send(
				PacketDistributor.ALL.noArg(),
				new SyncConfigPacket(manager.getMaxLocationsPerPlayer()));
	}
}