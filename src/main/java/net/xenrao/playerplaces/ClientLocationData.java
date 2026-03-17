package net.xenrao.playerplaces;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientLocationData {
	private static List<Location> locations = new ArrayList<>();
	private static List<LocationCategory> categories = new ArrayList<>();
	private static int maxLocationsPerPlayer = 5;

	@Nullable
	private static Location trackedLocation = null;

	public static void setLocations(List<Location> locs) {
		locations = new ArrayList<>(locs);
	}

	public static List<Location> getLocations() {
		return Collections.unmodifiableList(locations);
	}

	public static void setCategories(List<LocationCategory> cats) {
		categories = new ArrayList<>(cats);
	}

	public static List<LocationCategory> getCategories() {
		return Collections.unmodifiableList(categories);
	}

	public static void setMaxLocationsPerPlayer(int max) {
		maxLocationsPerPlayer = max;
	}

	public static int getMaxLocationsPerPlayer() {
		return maxLocationsPerPlayer;
	}

	public static void setTrackedLocation(@Nullable Location loc) {
		trackedLocation = loc;
	}

	@Nullable
	public static Location getTrackedLocation() {
		return trackedLocation;
	}

	public static void clearTracking() {
		trackedLocation = null;
	}

	@Nullable
	public static LocationCategory getCategoryById(String id) {
		for (LocationCategory cat : categories) {
			if (cat.getId().equals(id)) {
				return cat;
			}
		}
		return null;
	}
}