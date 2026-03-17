package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RemoveLocationPacket {
	private final String locationId;

	public RemoveLocationPacket(String locationId) {
		this.locationId = locationId;
	}

	public static void encode(RemoveLocationPacket msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.locationId);
	}

	public static RemoveLocationPacket decode(FriendlyByteBuf buf) {
		return new RemoveLocationPacket(buf.readUtf());
	}

	public static void handle(RemoveLocationPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null)
				return;
			LocationManager manager = LocationManager.get();
			if (manager == null)
				return;
			boolean isAdmin = player.hasPermissions(2);
			boolean success = manager.removeLocation(msg.locationId, player.getUUID(), isAdmin);
			if (success) {
				player.sendSystemMessage(Component.literal("§aMekan silindi."));
				PlacesEvents.syncAllToEveryone(player.getServer());
			} else {
				player.sendSystemMessage(Component.literal("§cMekan silinemedi! Yetkiniz yok veya mekan bulunamadı."));
			}
		});
		ctx.get().setPacketHandled(true);
	}
}