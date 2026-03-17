package net.xenrao.playerplaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LocationListScreen extends Screen {
	private EditBox searchBox;
	private String selectedCategory = null;
	private int scrollOffset = 0;
	private int categoryPageOffset = 0;
	private List<Location> filteredLocations = new ArrayList<>();
	private static final int LIST_ENTRY_HEIGHT = 24;
	private static final int LIST_VISIBLE_COUNT = 6;
	private static final int MAX_VISIBLE_CATS = 4;

	private int lastDataVersion = -1;
	private String savedSearchText = "";

	public LocationListScreen() {
		super(Component.literal("Locations"));
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int startY = 30;
		Minecraft mc = Minecraft.getInstance();
		boolean isAdmin = mc.player != null && mc.player.hasPermissions(2);

		// Search box
		searchBox = new EditBox(this.font, centerX - 100, startY, 200, 18, Component.literal("Search..."));
		searchBox.setMaxLength(32);
		searchBox.setValue(savedSearchText);
		searchBox.setResponder(text -> {
			scrollOffset = 0;
			updateFilteredLocations();
		});
		this.addRenderableWidget(searchBox);

		// --- Category filter buttons with dynamic centering ---
		int catY = startY + 24;
		List<LocationCategory> cats = ClientLocationData.getCategories();
		boolean needsPagination = cats.size() > MAX_VISIBLE_CATS;

		// Calculate total width first
		int totalWidth = 40 + 4; // "All" button + gap
		if (needsPagination && categoryPageOffset > 0) totalWidth += 20; // "<" button

		int endIdx = Math.min(cats.size(), categoryPageOffset + MAX_VISIBLE_CATS);
		if (categoryPageOffset > cats.size()) categoryPageOffset = 0;
		for (int i = categoryPageOffset; i < endIdx; i++) {
			int w = Math.max(font.width(cats.get(i).getName()) + 10, 40);
			totalWidth += w + 4;
		}

		if (needsPagination && categoryPageOffset + MAX_VISIBLE_CATS < cats.size()) totalWidth += 20; // ">" button
		totalWidth -= 4; // remove last gap

		int catX = centerX - totalWidth / 2;

		// "All" button
		this.addRenderableWidget(Button.builder(Component.literal("All"), btn -> {
			selectedCategory = null;
			scrollOffset = 0;
			updateFilteredLocations();
		}).bounds(catX, catY, 40, 16).build());
		catX += 44;

		// "<" prev
		if (needsPagination && categoryPageOffset > 0) {
			this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
				categoryPageOffset = Math.max(0, categoryPageOffset - MAX_VISIBLE_CATS);
				rebuildWidgets();
			}).bounds(catX, catY, 16, 16).build());
			catX += 20;
		}

		// Category buttons
		for (int i = categoryPageOffset; i < endIdx; i++) {
			LocationCategory cat = cats.get(i);
			final String catId = cat.getId();
			String label = cat.getName();
			int w = Math.max(font.width(label) + 10, 40);
			this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
				selectedCategory = catId;
				scrollOffset = 0;
				updateFilteredLocations();
			}).bounds(catX, catY, w, 16).build());
			catX += w + 4;
		}

		// ">" next
		if (needsPagination && categoryPageOffset + MAX_VISIBLE_CATS < cats.size()) {
			this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
				categoryPageOffset += MAX_VISIBLE_CATS;
				rebuildWidgets();
			}).bounds(catX, catY, 16, 16).build());
		}

		// --- Top right buttons ---
		int topRightX = this.width - 10;

		if (isAdmin) {
			topRightX -= 55;
			this.addRenderableWidget(Button.builder(
					Component.literal("Admin"),
					btn -> {
						savedSearchText = searchBox != null ? searchBox.getValue() : "";
						mc.setScreen(new AdminScreen(this));
					}
			).bounds(topRightX, 6, 50, 16).build());
		}

		topRightX -= 85;
		this.addRenderableWidget(Button.builder(
				Component.literal("My Locations"),
				btn -> {
					savedSearchText = searchBox != null ? searchBox.getValue() : "";
					mc.setScreen(new MyLocationsScreen(this));
				}
		).bounds(topRightX, 6, 80, 16).build());

		// Add location button
		long myCount = ClientLocationData.getLocations().stream()
				.filter(l -> mc.player != null && l.getOwnerName().equalsIgnoreCase(mc.player.getGameProfile().getName()))
				.count();
		int max = ClientLocationData.getMaxLocationsPerPlayer();

		this.addRenderableWidget(Button.builder(
				Component.literal("+ Add Location (" + myCount + "/" + max + ")"),
				btn -> {
					savedSearchText = searchBox != null ? searchBox.getValue() : "";
					mc.setScreen(new AddLocationScreen(this));
				}
		).bounds(centerX - 80, this.height - 50, 160, 20).build());

		// Close button
		this.addRenderableWidget(Button.builder(
				Component.literal("Close"),
				btn -> this.onClose()
		).bounds(centerX - 40, this.height - 26, 80, 20).build());

		updateFilteredLocations();
	}

	@Override
	public void tick() {
		super.tick();
		if (searchBox != null) savedSearchText = searchBox.getValue();
		int ver = ClientLocationData.getDataVersion();
		if (lastDataVersion == -1) { lastDataVersion = ver; return; }
		if (lastDataVersion != ver) { lastDataVersion = ver; rebuildWidgets(); }
	}

	private void updateFilteredLocations() {
		String search = searchBox != null ? searchBox.getValue().toLowerCase().trim() : "";
		filteredLocations.clear();
		for (Location loc : ClientLocationData.getLocations()) {
			if (selectedCategory != null && !loc.getCategoryId().equals(selectedCategory))
				continue;
			if (!search.isEmpty() && !loc.getName().toLowerCase().contains(search)
					&& !loc.getOwnerName().toLowerCase().contains(search))
				continue;
			filteredLocations.add(loc);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, "Locations", this.width / 2, 10, 0xFFFFFF);

		super.render(graphics, mouseX, mouseY, partialTick);

		int listX = this.width / 2 - 140;
		int listY = 78;
		int listWidth = 280;

		if (filteredLocations.isEmpty()) {
			graphics.drawCenteredString(this.font, "No locations found.", this.width / 2, listY + 40, 0x888888);
		} else {
			int maxScroll = Math.max(0, filteredLocations.size() - LIST_VISIBLE_COUNT);
			if (scrollOffset > maxScroll) scrollOffset = maxScroll;

			for (int i = 0; i < LIST_VISIBLE_COUNT; i++) {
				int index = i + scrollOffset;
				if (index >= filteredLocations.size()) break;

				Location loc = filteredLocations.get(index);
				int entryY = listY + i * LIST_ENTRY_HEIGHT;

				boolean hovered = mouseX >= listX && mouseX <= listX + listWidth
						&& mouseY >= entryY && mouseY <= entryY + LIST_ENTRY_HEIGHT - 2;
				int bgColor = hovered ? 0x60FFFFFF : 0x40000000;
				graphics.fill(listX, entryY, listX + listWidth, entryY + LIST_ENTRY_HEIGHT - 2, bgColor);

				LocationCategory cat = ClientLocationData.getCategoryById(loc.getCategoryId());
				if (cat != null) {
					ItemStack iconStack = getItemStackFromId(cat.getIcon());
					graphics.renderItem(iconStack, listX + 4, entryY + 3);
				}

				String displayName = loc.getName();
				if (displayName.length() > 20) displayName = displayName.substring(0, 18) + "..";
				graphics.drawString(this.font, displayName, listX + 24, entryY + 3, 0xFFFFFF);
				graphics.drawString(this.font, "by " + loc.getOwnerName(), listX + 24, entryY + 13, 0xAAAAAA);

				int trackBtnX = listX + listWidth - 50;
				int trackBtnW = 40;
				boolean trackHovered = mouseX >= trackBtnX && mouseX <= trackBtnX + trackBtnW
						&& mouseY >= entryY + 2 && mouseY <= entryY + 16;
				Location tracked = ClientLocationData.getTrackedLocation();
				boolean isTracked = tracked != null && tracked.getId().equals(loc.getId());
				String trackText = isTracked ? "Stop" : "Track";
				int trackColor;
				if (isTracked) trackColor = trackHovered ? 0xFFFF5555 : 0xFFAA0000;
				else trackColor = trackHovered ? 0xFF55FF55 : 0xFF00AA00;
				graphics.fill(trackBtnX, entryY + 2, trackBtnX + trackBtnW, entryY + 16, trackColor);
				graphics.drawCenteredString(this.font, trackText, trackBtnX + trackBtnW / 2, entryY + 5, 0xFFFFFF);

				if (hovered && mouseX < trackBtnX) {
					List<Component> tooltip = new ArrayList<>();
					tooltip.add(Component.literal("\u00A7e" + loc.getName()));
					if (!loc.getDescription().isEmpty())
						tooltip.add(Component.literal("\u00A7f" + loc.getDescription()));
					tooltip.add(Component.literal("\u00A77X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ()));
					tooltip.add(Component.literal("\u00A77Dimension: " + formatDimension(loc.getDimension())));
					tooltip.add(Component.literal("\u00A77Owner: " + loc.getOwnerName()));
					if (cat != null) tooltip.add(Component.literal("\u00A77Category: " + cat.getName()));
					graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
				}
			}

			if (scrollOffset > 0)
				graphics.drawCenteredString(this.font, "\u25B2", this.width / 2, listY - 10, 0xAAAAAA);
			if (scrollOffset < maxScroll)
				graphics.drawCenteredString(this.font, "\u25BC", this.width / 2, listY + LIST_VISIBLE_COUNT * LIST_ENTRY_HEIGHT, 0xAAAAAA);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			int listX = this.width / 2 - 140;
			int listY = 78;
			int listWidth = 280;

			for (int i = 0; i < LIST_VISIBLE_COUNT; i++) {
				int index = i + scrollOffset;
				if (index >= filteredLocations.size()) break;

				Location loc = filteredLocations.get(index);
				int entryY = listY + i * LIST_ENTRY_HEIGHT;

				int trackBtnX = listX + listWidth - 50;
				if (mouseX >= trackBtnX && mouseX <= trackBtnX + 40
						&& mouseY >= entryY + 2 && mouseY <= entryY + 16) {
					Location tracked = ClientLocationData.getTrackedLocation();
					if (tracked != null && tracked.getId().equals(loc.getId()))
						ClientLocationData.clearTracking();
					else
						ClientLocationData.setTrackedLocation(loc);
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int maxScroll = Math.max(0, filteredLocations.size() - LIST_VISIBLE_COUNT);
		if (delta > 0) scrollOffset = Math.max(0, scrollOffset - 1);
		else if (delta < 0) scrollOffset = Math.min(maxScroll, scrollOffset + 1);
		return true;
	}

	@Override
	public boolean isPauseScreen() { return false; }

	private static ItemStack getItemStackFromId(String itemId) {
		try {
			ResourceLocation rl = new ResourceLocation(itemId);
			Item item = ForgeRegistries.ITEMS.getValue(rl);
			if (item != null) return new ItemStack(item);
		} catch (Exception ignored) {}
		return ItemStack.EMPTY;
	}

	private static String formatDimension(String dim) {
		if (dim.contains("overworld")) return "Overworld";
		if (dim.contains("the_nether")) return "Nether";
		if (dim.contains("the_end")) return "The End";
		return dim;
	}
}