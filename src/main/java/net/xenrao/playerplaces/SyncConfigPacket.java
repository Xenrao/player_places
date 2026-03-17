package net.xenrao.playerplaces;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncConfigPacket {
	private final int maxLocationsPerPlayer;
	private final int maxNameLength;
	private final int maxDescLength;

	public SyncConfigPacket(int maxLocationsPerPlayer, int maxNameLength, int maxDescLength) {
		this.maxLocationsPerPlayer = maxLocationsPerPlayer;
		this.maxNameLength = maxNameLength;
		this.maxDescLength = maxDescLength;
	}

	public static void encode(SyncConfigPacket msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.maxLocationsPerPlayer);
		buf.writeInt(msg.maxNameLength);
		buf.writeInt(msg.maxDescLength);
	}

	public static SyncConfigPacket decode(FriendlyByteBuf buf) {
		return new SyncConfigPacket(buf.readInt(), buf.readInt(), buf.readInt());
	}

	public static void handle(SyncConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ClientLocationData.setMaxLocationsPerPlayer(msg.maxLocationsPerPlayer);
			ClientLocationData.setMaxNameLength(msg.maxNameLength);
			ClientLocationData.setMaxDescLength(msg.maxDescLength);
		});
		ctx.get().setPacketHandled(true);
	}
}