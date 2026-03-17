package net.xenrao.playerplaces;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LocationManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static LocationManager instance;

	private final Path dataFile;
	private final List<Location> locations = new ArrayList<>();
	private final List<LocationCategory> categories = new ArrayList<>();
	private int maxLocationsPerPlayer = 5;
	private int maxNameLength = 32;
	private int maxDescLength = 64;

	private LocationManager(MinecraftServer server) {
		Path worldDir = server.getWorldPath(LevelResource.ROOT);
		this.dataFile = worldDir.resolve("playerplaces").resolve("places.json");
		PlayerPlacesMod.LOGGER.info("PlayerPlaces data file: {}", dataFile.toAbsolutePath());
		load();
	}

	public static void init(MinecraftServer server) {
		instance = new LocationManager(server);
	}

	public static void shutdown() {
		if (instance != null) {
			instance.save();
			instance = null;
		}
	}

	public static LocationManager get() {
		return instance;
	}

	public void reload() {
		load();
	}

	public void forceSave() {
		save();
	}

	// --- Locations CRUD ---

	public boolean addLocation(Location location) {
		long count = locations.stream()
				.filter(l -> l.getOwnerName().equalsIgnoreCase(location.getOwnerName()))
				.count();
		if (count >= maxLocationsPerPlayer) {
			return false;
		}
		locations.add(location);
		save();
		return true;
	}

	public boolean removeLocation(String locationId, String playerName, boolean isAdmin) {
		Iterator<Location> it = locations.iterator();
		while (it.hasNext()) {
			Location loc = it.next();
			if (loc.getId().equals(locationId)) {
				if (isAdmin || loc.getOwnerName().equalsIgnoreCase(playerName)) {
					it.remove();
					save();
					return true;
				}
				return false;
			}
		}
		return false;
	}

	public Location getLocation(String locationId) {
		return locations.stream()
				.filter(l -> l.getId().equals(locationId))
				.findFirst().orElse(null);
	}

	public List<Location> getAllLocations() {
		return Collections.unmodifiableList(locations);
	}

	public List<Location> getLocationsByPlayer(String playerName) {
		return locations.stream()
				.filter(l -> l.getOwnerName().equalsIgnoreCase(playerName))
				.collect(Collectors.toList());
	}

	public void updateLocation(String locationId, String newName, String newDescription, String newCategoryId) {
		Location loc = getLocation(locationId);
		if (loc != null) {
			if (newName != null) loc.setName(newName);
			if (newDescription != null) loc.setDescription(newDescription);
			if (newCategoryId != null) loc.setCategoryId(newCategoryId);
			save();
		}
	}

	public boolean updateLocationByPlayer(String locationId, String playerName, String newName, String newDescription, String newCategoryId) {
		Location loc = getLocation(locationId);
		if (loc != null && loc.getOwnerName().equalsIgnoreCase(playerName)) {
			if (newName != null) loc.setName(newName);
			if (newDescription != null) loc.setDescription(newDescription);
			if (newCategoryId != null) loc.setCategoryId(newCategoryId);
			save();
			return true;
		}
		return false;
	}

	// --- Categories CRUD ---

	public void addCategory(LocationCategory category) {
		categories.removeIf(c -> c.getId().equals(category.getId()));
		categories.add(category);
		save();
	}

	public void removeCategory(String categoryId) {
		categories.removeIf(c -> c.getId().equals(categoryId));
		locations.removeIf(l -> l.getCategoryId().equals(categoryId));
		save();
	}

	public List<LocationCategory> getCategories() {
		return Collections.unmodifiableList(categories);
	}

	public LocationCategory getCategory(String categoryId) {
		return categories.stream()
				.filter(c -> c.getId().equals(categoryId))
				.findFirst().orElse(null);
	}

	// --- Config ---

	public int getMaxLocationsPerPlayer() { return maxLocationsPerPlayer; }
	public void setMaxLocationsPerPlayer(int max) { this.maxLocationsPerPlayer = max; save(); }
	public int getMaxNameLength() { return maxNameLength; }
	public void setMaxNameLength(int max) { this.maxNameLength = max; save(); }
	public int getMaxDescLength() { return maxDescLength; }
	public void setMaxDescLength(int max) { this.maxDescLength = max; save(); }

	// --- Persistence ---

	private void save() {
		try {
			JsonObject root = new JsonObject();
			JsonObject config = new JsonObject();
			config.addProperty("maxLocationsPerPlayer", maxLocationsPerPlayer);
			config.addProperty("maxNameLength", maxNameLength);
			config.addProperty("maxDescLength", maxDescLength);
			root.add("config", config);
			JsonArray catArray = new JsonArray();
			for (LocationCategory cat : categories) catArray.add(cat.toJson());
			root.add("categories", catArray);
			JsonArray locArray = new JsonArray();
			for (Location loc : locations) locArray.add(loc.toJson());
			root.add("locations", locArray);
			Files.createDirectories(dataFile.getParent());
			try (Writer writer = new OutputStreamWriter(new FileOutputStream(dataFile.toFile()), StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			PlayerPlacesMod.LOGGER.error("Failed to save PlayerPlaces data", e);
		}
	}

	private void load() {
		if (!Files.exists(dataFile)) {
			createDefaults();
			save();
			return;
		}
		try (Reader reader = new InputStreamReader(new FileInputStream(dataFile.toFile()), StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root.has("config")) {
				JsonObject config = root.getAsJsonObject("config");
				if (config.has("maxLocationsPerPlayer")) maxLocationsPerPlayer = config.get("maxLocationsPerPlayer").getAsInt();
				if (config.has("maxNameLength")) maxNameLength = config.get("maxNameLength").getAsInt();
				if (config.has("maxDescLength")) maxDescLength = config.get("maxDescLength").getAsInt();
			}
			categories.clear();
			if (root.has("categories"))
				for (JsonElement el : root.getAsJsonArray("categories"))
					categories.add(LocationCategory.fromJson(el.getAsJsonObject()));
			locations.clear();
			if (root.has("locations"))
				for (JsonElement el : root.getAsJsonArray("locations"))
					locations.add(Location.fromJson(el.getAsJsonObject()));
			PlayerPlacesMod.LOGGER.info("Loaded {} locations, {} categories", locations.size(), categories.size());
		} catch (Exception e) {
			PlayerPlacesMod.LOGGER.error("Failed to load PlayerPlaces data", e);
			createDefaults();
		}
	}

	private void createDefaults() {
		categories.clear();
		categories.add(new LocationCategory("market", "Market", "minecraft:emerald"));
		categories.add(new LocationCategory("c1", "Category 2", "minecraft:wheat"));
		categories.add(new LocationCategory("c2", "Category 3", "minecraft:oak_door"));
		categories.add(new LocationCategory("c4", "Category 4", "minecraft:spyglass"));
	}
}