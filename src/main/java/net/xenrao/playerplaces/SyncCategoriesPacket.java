package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncCategoriesPacket {
	private final List<LocationCategory> categories;

	public SyncCategoriesPacket(List<LocationCategory> categories) {
		this.categories = categories;
	}

	public static void encode(SyncCategoriesPacket msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.categories.size());
		for (LocationCategory cat : msg.categories) {
			cat.toNetwork(buf);
		}
	}

	public static SyncCategoriesPacket decode(FriendlyByteBuf buf) {
		int size = buf.readInt();
		List<LocationCategory> list = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			list.add(LocationCategory.fromNetwork(buf));
		}
		return new SyncCategoriesPacket(list);
	}

	public static void handle(SyncCategoriesPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ClientLocationData.setCategories(msg.categories);
		});
		ctx.get().setPacketHandled(true);
	}
}