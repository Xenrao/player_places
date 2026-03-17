package net.xenrao.playerplaces;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

public class LocationCategory {
	private String id;
	private String name;
	private String icon;

	public LocationCategory(String id, String name, String icon) {
		this.id = id;
		this.name = name;
		this.icon = icon;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", id);
		obj.addProperty("name", name);
		obj.addProperty("icon", icon);
		return obj;
	}

	public static LocationCategory fromJson(JsonObject obj) {
		return new LocationCategory(
				obj.get("id").getAsString(),
				obj.get("name").getAsString(),
				obj.get("icon").getAsString()
		);
	}

	public void toNetwork(FriendlyByteBuf buf) {
		buf.writeUtf(id);
		buf.writeUtf(name);
		buf.writeUtf(icon);
	}

	public static LocationCategory fromNetwork(FriendlyByteBuf buf) {
		return new LocationCategory(
				buf.readUtf(),
				buf.readUtf(),
				buf.readUtf()
		);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
}