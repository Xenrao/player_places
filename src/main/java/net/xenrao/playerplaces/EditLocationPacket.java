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
			if (player == null)
				return;
			LocationManager manager = LocationManager.get();
			if (manager == null)
				return;
			if (msg.newName == null || msg.newName.trim().isEmpty()) {
				player.sendSystemMessage(Component.literal("\u00A7cName cannot be empty!"));
				return;
			}
			if (msg.newName.length() > 32) {
				player.sendSystemMessage(Component.literal("\u00A7cName max 32 characters!"));
				return;
			}
			String name = msg.newName.trim();
			String desc = msg.newDescription != null ? msg.newDescription.trim() : "";
			if (desc.length() > 64) desc = desc.substring(0, 64);
			String cat = msg.newCategoryId;
			if (manager.getCategory(cat) == null) {
				player.sendSystemMessage(Component.literal("\u00A7cInvalid category!"));
				return;
			}
			boolean success = manager.updateLocationByPlayer(msg.locationId, player.getUUID(), name, desc, cat);
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