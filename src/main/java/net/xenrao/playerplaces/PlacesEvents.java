package net.xenrao.playerplaces;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
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

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(Commands.literal("playerplaces")
				.then(Commands.literal("reload")
						.requires(src -> src.hasPermission(2))
						.executes(ctx -> {
							LocationManager manager = LocationManager.get();
							if (manager != null) {
								manager.reload();
								MinecraftServer server = ctx.getSource().getServer();
								syncAllToEveryone(server);
								ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aPlayerPlaces data reloaded."), true);
							} else {
								ctx.getSource().sendFailure(Component.literal("\u00A7cLocationManager not initialized!"));
							}
							return 1;
						})));
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