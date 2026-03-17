package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class AddLocationPacket {
	private final String name;
	private final String description;
	private final String categoryId;

	public AddLocationPacket(String name, String description, String categoryId) {
		this.name = name;
		this.description = description;
		this.categoryId = categoryId;
	}

	public static void encode(AddLocationPacket msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.name);
		buf.writeUtf(msg.description);
		buf.writeUtf(msg.categoryId);
	}

	public static AddLocationPacket decode(FriendlyByteBuf buf) {
		return new AddLocationPacket(buf.readUtf(), buf.readUtf(), buf.readUtf());
	}

	public static void handle(AddLocationPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			LocationManager manager = LocationManager.get();
			if (manager == null) return;
			if (msg.name == null || msg.name.trim().isEmpty()) {
				player.sendSystemMessage(Component.literal("\u00A7cLocation name cannot be empty!"));
				return;
			}
			if (msg.name.trim().length() > manager.getMaxNameLength()) {
				player.sendSystemMessage(Component.literal("\u00A7cName too long! Max: " + manager.getMaxNameLength()));
				return;
			}
			if (manager.getCategory(msg.categoryId) == null) {
				player.sendSystemMessage(Component.literal("\u00A7cInvalid category!"));
				return;
			}
			String dimension = player.level().dimension().location().toString();
			String desc = msg.description != null ? msg.description.trim() : "";
			if (desc.length() > manager.getMaxDescLength()) desc = desc.substring(0, manager.getMaxDescLength());

			String playerName = player.getGameProfile().getName();
			Location location = new Location(
					UUID.randomUUID().toString(),
					msg.name.trim(),
					desc,
					msg.categoryId,
					playerName,
					player.blockPosition().getX(),
					player.blockPosition().getY(),
					player.blockPosition().getZ(),
					dimension,
					System.currentTimeMillis()
			);
			boolean success = manager.addLocation(location);
			if (success) {
				player.sendSystemMessage(Component.literal("\u00A7aLocation saved: " + msg.name.trim()));
				PlacesEvents.syncAllToEveryone(player.getServer());
			} else {
				player.sendSystemMessage(Component.literal("\u00A7cLocation limit reached! (Max: " + manager.getMaxLocationsPerPlayer() + ")"));
			}
		});
		ctx.get().setPacketHandled(true);
	}
}