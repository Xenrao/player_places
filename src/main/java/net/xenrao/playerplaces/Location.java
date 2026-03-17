package net.xenrao.playerplaces;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Location {
	private final String id;
	private String name;
	private String description;
	private String categoryId;
	private UUID ownerUUID;
	private String ownerName;
	private int x;
	private int y;
	private int z;
	private String dimension;
	private final long createdAt;

	public Location(String id, String name, String description, String categoryId, UUID ownerUUID, String ownerName, int x, int y, int z, String dimension, long createdAt) {
		this.id = id;
		this.name = name;
		this.description = description != null ? description : "";
		this.categoryId = categoryId;
		this.ownerUUID = ownerUUID;
		this.ownerName = ownerName;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.createdAt = createdAt;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", id);
		obj.addProperty("name", name);
		obj.addProperty("description", description);
		obj.addProperty("categoryId", categoryId);
		obj.addProperty("ownerUUID", ownerUUID.toString());
		obj.addProperty("ownerName", ownerName);
		obj.addProperty("x", x);
		obj.addProperty("y", y);
		obj.addProperty("z", z);
		obj.addProperty("dimension", dimension);
		obj.addProperty("createdAt", createdAt);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		obj.addProperty("createdAtFormatted", sdf.format(new Date(createdAt)));
		return obj;
	}

	public static Location fromJson(JsonObject obj) {
		String desc = obj.has("description") ? obj.get("description").getAsString() : "";
		return new Location(
				obj.get("id").getAsString(),
				obj.get("name").getAsString(),
				desc,
				obj.get("categoryId").getAsString(),
				UUID.fromString(obj.get("ownerUUID").getAsString()),
				obj.get("ownerName").getAsString(),
				obj.get("x").getAsInt(),
				obj.get("y").getAsInt(),
				obj.get("z").getAsInt(),
				obj.get("dimension").getAsString(),
				obj.get("createdAt").getAsLong()
		);
	}

	public void toNetwork(FriendlyByteBuf buf) {
		buf.writeUtf(id);
		buf.writeUtf(name);
		buf.writeUtf(description);
		buf.writeUtf(categoryId);
		buf.writeUUID(ownerUUID);
		buf.writeUtf(ownerName);
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeUtf(dimension);
		buf.writeLong(createdAt);
	}

	public static Location fromNetwork(FriendlyByteBuf buf) {
		return new Location(
				buf.readUtf(),
				buf.readUtf(),
				buf.readUtf(),
				buf.readUtf(),
				buf.readUUID(),
				buf.readUtf(),
				buf.readInt(),
				buf.readInt(),
				buf.readInt(),
				buf.readUtf(),
				buf.readLong()
		);
	}

	public String getId() { return id; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description != null ? description : ""; }
	public String getCategoryId() { return categoryId; }
	public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
	public UUID getOwnerUUID() { return ownerUUID; }
	public void setOwnerUUID(UUID ownerUUID) { this.ownerUUID = ownerUUID; }
	public String getOwnerName() { return ownerName; }
	public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
	public int getX() { return x; }
	public void setX(int x) { this.x = x; }
	public int getY() { return y; }
	public void setY(int y) { this.y = y; }
	public int getZ() { return z; }
	public void setZ(int z) { this.z = z; }
	public String getDimension() { return dimension; }
	public void setDimension(String dimension) { this.dimension = dimension; }
	public long getCreatedAt() { return createdAt; }
}