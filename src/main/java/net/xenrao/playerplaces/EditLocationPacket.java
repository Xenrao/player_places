package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EditLocationPacket {
	private final String locationId;
	private final String newName;
	private final String newDescription;
	private final String newCategoryId;

	public EditLocationPacket(String locationId, String newName, String newDescription, String newCategoryId) {
		this.locationId = locationId;
		this.newName = newName;
		this.newDescription = newDescription;
		this.newCategoryId = newCategoryId;
	}

	public static void encode(EditLocationPacket msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.locationId);
		buf.writeUtf(msg.newName);
		buf.writeUtf(msg.newDescription);
		buf.writeUtf(msg.newCategoryId);
	}

	public static EditLocationPacket decode(FriendlyByteBuf buf) {
		return new EditLocationPacket(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
	}

	public static void handle(EditLocationPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			LocationManager manager = LocationManager.get();
			if (manager == null) return;
			if (msg.newName == null || msg.newName.trim().isEmpty()) {
				player.sendSystemMessage(Component.literal("\u00A7cName cannot be empty!"));
				return;
			}
			if (msg.newName.trim().length() > manager.getMaxNameLength()) {
				player.sendSystemMessage(Component.literal("\u00A7cName too long! Max: " + manager.getMaxNameLength()));
				return;
			}
			String desc = msg.newDescription != null ? msg.newDescription.trim() : "";
			if (desc.length() > manager.getMaxDescLength()) desc = desc.substring(0, manager.getMaxDescLength());
			if (manager.getCategory(msg.newCategoryId) == null) {
				player.sendSystemMessage(Component.literal("\u00A7cInvalid category!"));
				return;
			}
			String playerName = player.getGameProfile().getName();
			boolean success = manager.updateLocationByPlayer(msg.locationId, playerName, msg.newName.trim(), desc, msg.newCategoryId);
			if (success) {
				player.sendSystemMessage(Component.literal("\u00A7aLocation updated."));
				PlacesEvents.syncAllToEveryone(player.getServer());
			} else {
				player.sendSystemMessage(Component.literal("\u00A7cYou can only edit your own locations!"));
			}
		});
		ctx.get().setPacketHandled(true);
	}
}