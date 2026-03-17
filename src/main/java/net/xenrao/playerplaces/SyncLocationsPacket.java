package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncLocationsPacket {
	private final List<Location> locations;

	public SyncLocationsPacket(List<Location> locations) {
		this.locations = locations;
	}

	public static void encode(SyncLocationsPacket msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.locations.size());
		for (Location loc : msg.locations) {
			loc.toNetwork(buf);
		}
	}

	public static SyncLocationsPacket decode(FriendlyByteBuf buf) {
		int size = buf.readInt();
		List<Location> list = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			list.add(Location.fromNetwork(buf));
		}
		return new SyncLocationsPacket(list);
	}

	public static void handle(SyncLocationsPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ClientLocationData.setLocations(msg.locations);
		});
		ctx.get().setPacketHandled(true);
	}
}