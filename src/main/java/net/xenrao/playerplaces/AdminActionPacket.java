package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.mojang.authlib.GameProfile;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class AdminActionPacket {
	public static final int ACTION_EDIT_LOCATION = 0;
	public static final int ACTION_DELETE_LOCATION = 1;
	public static final int ACTION_ADD_CATEGORY = 2;
	public static final int ACTION_REMOVE_CATEGORY = 3;
	public static final int ACTION_EDIT_CATEGORY = 4;
	public static final int ACTION_SET_MAX_LOCATIONS = 5;
	public static final int ACTION_FULL_EDIT_LOCATION = 6;

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
				player.sendSystemMessage(Component.literal("\u00A7cAdmin permission required!"));
				return;
			}
			LocationManager manager = LocationManager.get();
			if (manager == null)
				return;
			switch (msg.action) {
				case ACTION_EDIT_LOCATION -> {
					String newName = msg.param2.isEmpty() ? null : msg.param2;
					String newCat = msg.param3.isEmpty() ? null : msg.param3;
					manager.updateLocation(msg.param1, newName, null, newCat);
					player.sendSystemMessage(Component.literal("\u00A7aLocation edited."));
				}
				case ACTION_DELETE_LOCATION -> {
					manager.removeLocation(msg.param1, player.getUUID(), true);
					player.sendSystemMessage(Component.literal("\u00A7aLocation deleted."));
				}
				case ACTION_ADD_CATEGORY -> {
					if (msg.param1.isEmpty() || msg.param2.isEmpty()) {
						player.sendSystemMessage(Component.literal("\u00A7cCategory ID and name required!"));
						return;
					}
					manager.addCategory(new LocationCategory(msg.param1, msg.param2, msg.param3.isEmpty() ? "minecraft:paper" : msg.param3));
					player.sendSystemMessage(Component.literal("\u00A7aCategory added: " + msg.param2));
				}
				case ACTION_REMOVE_CATEGORY -> {
					manager.removeCategory(msg.param1);
					player.sendSystemMessage(Component.literal("\u00A7aCategory removed."));
				}
				case ACTION_EDIT_CATEGORY -> {
					LocationCategory cat = manager.getCategory(msg.param1);
					if (cat != null) {
						if (!msg.param2.isEmpty()) cat.setName(msg.param2);
						if (!msg.param3.isEmpty()) cat.setIcon(msg.param3);
						manager.addCategory(cat);
						player.sendSystemMessage(Component.literal("\u00A7aCategory edited."));
					} else {
						player.sendSystemMessage(Component.literal("\u00A7cCategory not found!"));
					}
				}
				case ACTION_SET_MAX_LOCATIONS -> {
					// param1 = key, param2 = value
					String key = msg.param1;
					String value = msg.param2;
					try {
						int val = Integer.parseInt(value);
						switch (key) {
							case "maxLocations" -> {
								if (val < 1 || val > 100) { player.sendSystemMessage(Component.literal("\u00A7cValue must be 1-100!")); return; }
								manager.setMaxLocationsPerPlayer(val);
								player.sendSystemMessage(Component.literal("\u00A7aMax locations updated: " + val));
							}
							case "maxNameLength" -> {
								if (val < 3 || val > 64) { player.sendSystemMessage(Component.literal("\u00A7cValue must be 3-64!")); return; }
								manager.setMaxNameLength(val);
								player.sendSystemMessage(Component.literal("\u00A7aMax name length updated: " + val));
							}
							case "maxDescLength" -> {
								if (val < 0 || val > 256) { player.sendSystemMessage(Component.literal("\u00A7cValue must be 0-256!")); return; }
								manager.setMaxDescLength(val);
								player.sendSystemMessage(Component.literal("\u00A7aMax description length updated: " + val));
							}
							default -> player.sendSystemMessage(Component.literal("\u00A7cUnknown setting: " + key));
						}
					} catch (NumberFormatException e) {
						player.sendSystemMessage(Component.literal("\u00A7cInvalid number!"));
					}
				}
				case ACTION_FULL_EDIT_LOCATION -> {
					// param1 = locationId
					// param2 = packed: name|desc|category|ownerName|x|y|z|dimension
					Location loc = manager.getLocation(msg.param1);
					if (loc == null) {
						player.sendSystemMessage(Component.literal("\u00A7cLocation not found!"));
						return;
					}
					try {
						String[] parts = msg.param2.split("\\|", -1);
						if (parts.length < 8) {
							player.sendSystemMessage(Component.literal("\u00A7cInvalid data format!"));
							return;
						}
						String newName = parts[0].trim();
						String newDesc = parts[1].trim();
						String newCat = parts[2].trim();
						String newOwnerName = parts[3].trim();
						String newX = parts[4].trim();
						String newY = parts[5].trim();
						String newZ = parts[6].trim();
						String newDim = parts[7].trim();

						if (!newName.isEmpty()) loc.setName(newName);
						loc.setDescription(newDesc);
						if (!newCat.isEmpty() && manager.getCategory(newCat) != null) loc.setCategoryId(newCat);

						// Owner name change - resolve UUID from server
						if (!newOwnerName.isEmpty() && !newOwnerName.equals(loc.getOwnerName())) {
							MinecraftServer server = player.getServer();
							if (server != null) {
								// Try online player first
								ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(newOwnerName);
								if (targetPlayer != null) {
									loc.setOwnerName(targetPlayer.getGameProfile().getName());
									loc.setOwnerUUID(targetPlayer.getUUID());
								} else {
									// Try offline lookup
									Optional<GameProfile> profile = server.getProfileCache().get(newOwnerName);
									if (profile.isPresent()) {
										loc.setOwnerName(profile.get().getName());
										loc.setOwnerUUID(profile.get().getId());
									} else {
										loc.setOwnerName(newOwnerName);
										player.sendSystemMessage(Component.literal("\u00A7eWarning: Player '" + newOwnerName + "' not found. UUID unchanged."));
									}
								}
							}
						}

						if (!newX.isEmpty()) loc.setX(Integer.parseInt(newX));
						if (!newY.isEmpty()) loc.setY(Integer.parseInt(newY));
						if (!newZ.isEmpty()) loc.setZ(Integer.parseInt(newZ));
						if (!newDim.isEmpty()) loc.setDimension(newDim);

						manager.forceSave();
						player.sendSystemMessage(Component.literal("\u00A7aLocation fully edited."));
					} catch (NumberFormatException e) {
						player.sendSystemMessage(Component.literal("\u00A7cInvalid number in position fields!"));
					}
				}
			}
			PlacesEvents.syncAllToEveryone(player.getServer());
		});
		ctx.get().setPacketHandled(true);
	}
}