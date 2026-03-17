package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class AddLocationPacket {
	private final String name;
	private final String categoryId;

	public AddLocationPacket(String name, String categoryId) {
		this.name = name;
		this.categoryId = categoryId;
	}

	public static void encode(AddLocationPacket msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.name);
		buf.writeUtf(msg.categoryId);
	}

	public static AddLocationPacket decode(FriendlyByteBuf buf) {
		return new AddLocationPacket(buf.readUtf(), buf.readUtf());
	}

	public static void handle(AddLocationPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null)
				return;
			LocationManager manager = LocationManager.get();
			if (manager == null)
				return;
			if (msg.name == null || msg.name.trim().isEmpty()) {
				player.sendSystemMessage(Component.literal("§cMekan ismi boş olamaz!"));
				return;
			}
			if (msg.name.length() > 32) {
				player.sendSystemMessage(Component.literal("§cMekan ismi en fazla 32 karakter olabilir!"));
				return;
			}
			if (manager.getCategory(msg.categoryId) == null) {
				player.sendSystemMessage(Component.literal("§cGeçersiz kategori!"));
				return;
			}
			String dimension = player.level().dimension().location().toString();
			Location location = new Location(
					UUID.randomUUID().toString(),
					msg.name.trim(),
					msg.categoryId,
					player.getUUID(),
					player.getGameProfile().getName(),
					player.blockPosition().getX(),
					player.blockPosition().getY(),
					player.blockPosition().getZ(),
					dimension,
					System.currentTimeMillis()
			);
			boolean success = manager.addLocation(location);
			if (success) {
				player.sendSystemMessage(Component.literal("§aMekan kaydedildi: " + msg.name.trim()));
				PlacesEvents.syncAllToEveryone(player.getServer());
			} else {
				player.sendSystemMessage(Component.literal("§cMekan limitine ulaştınız! (Max: " + manager.getMaxLocationsPerPlayer() + ")"));
			}
		});
		ctx.get().setPacketHandled(true);
	}
}