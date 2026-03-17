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

	private LocationManager(MinecraftServer server) {
		Path worldDir = server.getWorldPath(LevelResource.ROOT);
		this.dataFile = worldDir.resolve("playerplaces_data.json");
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

	// --- Locations CRUD ---

	public boolean addLocation(Location location) {
		long count = locations.stream()
				.filter(l -> l.getOwnerUUID().equals(location.getOwnerUUID()))
				.count();
		if (count >= maxLocationsPerPlayer) {
			return false;
		}
		locations.add(location);
		save();
		return true;
	}

	public boolean removeLocation(String locationId, UUID requester, boolean isAdmin) {
		Iterator<Location> it = locations.iterator();
		while (it.hasNext()) {
			Location loc = it.next();
			if (loc.getId().equals(locationId)) {
				if (isAdmin || loc.getOwnerUUID().equals(requester)) {
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

	public List<Location> getLocationsByPlayer(UUID playerUUID) {
		return locations.stream()
				.filter(l -> l.getOwnerUUID().equals(playerUUID))
				.collect(Collectors.toList());
	}

	public List<Location> getLocationsByCategory(String categoryId) {
		return locations.stream()
				.filter(l -> l.getCategoryId().equals(categoryId))
				.collect(Collectors.toList());
	}

	public List<Location> getLocationsByDimension(String dimension) {
		return locations.stream()
				.filter(l -> l.getDimension().equals(dimension))
				.collect(Collectors.toList());
	}

	public void updateLocation(String locationId, String newName, String newCategoryId) {
		Location loc = getLocation(locationId);
		if (loc != null) {
			if (newName != null)
				loc.setName(newName);
			if (newCategoryId != null)
				loc.setCategoryId(newCategoryId);
			save();
		}
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

	public int getMaxLocationsPerPlayer() {
		return maxLocationsPerPlayer;
	}

	public void setMaxLocationsPerPlayer(int max) {
		this.maxLocationsPerPlayer = max;
		save();
	}

	// --- Persistence ---

	private void save() {
		try {
			JsonObject root = new JsonObject();
			JsonObject config = new JsonObject();
			config.addProperty("maxLocationsPerPlayer", maxLocationsPerPlayer);
			root.add("config", config);
			JsonArray catArray = new JsonArray();
			for (LocationCategory cat : categories) {
				catArray.add(cat.toJson());
			}
			root.add("categories", catArray);
			JsonArray locArray = new JsonArray();
			for (Location loc : locations) {
				locArray.add(loc.toJson());
			}
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
				if (config.has("maxLocationsPerPlayer")) {
					maxLocationsPerPlayer = config.get("maxLocationsPerPlayer").getAsInt();
				}
			}
			categories.clear();
			if (root.has("categories")) {
				for (JsonElement el : root.getAsJsonArray("categories")) {
					categories.add(LocationCategory.fromJson(el.getAsJsonObject()));
				}
			}
			locations.clear();
			if (root.has("locations")) {
				for (JsonElement el : root.getAsJsonArray("locations")) {
					locations.add(Location.fromJson(el.getAsJsonObject()));
				}
			}
			PlayerPlacesMod.LOGGER.info("Loaded {} locations, {} categories", locations.size(), categories.size());
		} catch (Exception e) {
			PlayerPlacesMod.LOGGER.error("Failed to load PlayerPlaces data", e);
			createDefaults();
		}
	}

	private void createDefaults() {
		categories.clear();
		categories.add(new LocationCategory("market", "Market", "minecraft:emerald"));
		categories.add(new LocationCategory("farm", "Farm", "minecraft:wheat"));
		categories.add(new LocationCategory("base", "Üs", "minecraft:oak_door"));
		categories.add(new LocationCategory("poi", "İlgi Noktası", "minecraft:spyglass"));
	}
}