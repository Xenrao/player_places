package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncConfigPacket {
	private final int maxLocationsPerPlayer;

	public SyncConfigPacket(int maxLocationsPerPlayer) {
		this.maxLocationsPerPlayer = maxLocationsPerPlayer;
	}

	public static void encode(SyncConfigPacket msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.maxLocationsPerPlayer);
	}

	public static SyncConfigPacket decode(FriendlyByteBuf buf) {
		return new SyncConfigPacket(buf.readInt());
	}

	public static void handle(SyncConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ClientLocationData.setMaxLocationsPerPlayer(msg.maxLocationsPerPlayer);
		});
		ctx.get().setPacketHandled(true);
	}
}