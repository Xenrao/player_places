package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AdminActionPacket {
	public static final int ACTION_EDIT_LOCATION = 0;
	public static final int ACTION_DELETE_LOCATION = 1;
	public static final int ACTION_ADD_CATEGORY = 2;
	public static final int ACTION_REMOVE_CATEGORY = 3;
	public static final int ACTION_EDIT_CATEGORY = 4;
	public static final int ACTION_SET_MAX_LOCATIONS = 5;

	private final int action;
	private final String param1;
	private final String param2;
	private final String param3;

	public AdminActionPacket(int action, String param1, String param2, String param3) {
		this.action = action;
		this.param1 = param1 != null ? param1 : "";
		this.param2 = param2 != null ? param2 : "";
		this.param3 = param3 != null ? param3 : "";
	}

	public static void encode(AdminActionPacket msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.action);
		buf.writeUtf(msg.param1);
		buf.writeUtf(msg.param2);
		buf.writeUtf(msg.param3);
	}

	public static AdminActionPacket decode(FriendlyByteBuf buf) {
		return new AdminActionPacket(buf.readInt(), buf.readUtf(), buf.readUtf(), buf.readUtf());
	}

	public static void handle(AdminActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null)
				return;
			if (!player.hasPermissions(2)) {
				player.sendSystemMessage(Component.literal("§cBu işlem için admin yetkisi gerekli!"));
				return;
			}
			LocationManager manager = LocationManager.get();
			if (manager == null)
				return;
			switch (msg.action) {
				case ACTION_EDIT_LOCATION -> {
					// param1 = locationId, param2 = newName, param3 = newCategoryId
					String newName = msg.param2.isEmpty() ? null : msg.param2;
					String newCat = msg.param3.isEmpty() ? null : msg.param3;
					manager.updateLocation(msg.param1, newName, newCat);
					player.sendSystemMessage(Component.literal("§aMekan düzenlendi."));
				}
				case ACTION_DELETE_LOCATION -> {
					// param1 = locationId
					manager.removeLocation(msg.param1, player.getUUID(), true);
					player.sendSystemMessage(Component.literal("§aMekan silindi."));
				}
				case ACTION_ADD_CATEGORY -> {
					// param1 = id, param2 = name, param3 = icon
					if (msg.param1.isEmpty() || msg.param2.isEmpty()) {
						player.sendSystemMessage(Component.literal("§cKategori ID ve isim boş olamaz!"));
						return;
					}
					manager.addCategory(new LocationCategory(msg.param1, msg.param2, msg.param3.isEmpty() ? "minecraft:paper" : msg.param3));
					player.sendSystemMessage(Component.literal("§aKategori eklendi: " + msg.param2));
				}
				case ACTION_REMOVE_CATEGORY -> {
					// param1 = categoryId
					manager.removeCategory(msg.param1);
					player.sendSystemMessage(Component.literal("§aKategori silindi. İlgili mekanlar da kaldırıldı."));
				}
				case ACTION_EDIT_CATEGORY -> {
					// param1 = categoryId, param2 = newName, param3 = newIcon
					LocationCategory cat = manager.getCategory(msg.param1);
					if (cat != null) {
						if (!msg.param2.isEmpty())
							cat.setName(msg.param2);
						if (!msg.param3.isEmpty())
							cat.setIcon(msg.param3);
						manager.addCategory(cat);
						player.sendSystemMessage(Component.literal("§aKategori düzenlendi."));
					} else {
						player.sendSystemMessage(Component.literal("§cKategori bulunamadı!"));
					}
				}
				case ACTION_SET_MAX_LOCATIONS -> {
					// param1 = max count as string
					try {
						int max = Integer.parseInt(msg.param1);
						if (max < 1 || max > 100) {
							player.sendSystemMessage(Component.literal("§cDeğer 1-100 arasında olmalı!"));
							return;
						}
						manager.setMaxLocationsPerPlayer(max);
						player.sendSystemMessage(Component.literal("§aMax mekan sayısı güncellendi: " + max));
					} catch (NumberFormatException e) {
						player.sendSystemMessage(Component.literal("§cGeçersiz sayı!"));
					}
				}
			}
			PlacesEvents.syncAllToEveryone(player.getServer());
		});
		ctx.get().setPacketHandled(true);
	}
}