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
	private String selectedCategory = null; // null = all
	private int scrollOffset = 0;
	private List<Location> filteredLocations = new ArrayList<>();
	private static final int LIST_ENTRY_HEIGHT = 24;
	private static final int LIST_VISIBLE_COUNT = 6;

	public LocationListScreen() {
		super(Component.literal("Locations"));
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int startY = 30;

		// Search box
		searchBox = new EditBox(this.font, centerX - 100, startY, 200, 18, Component.literal("Search..."));
		searchBox.setMaxLength(32);
		searchBox.setResponder(text -> {
			scrollOffset = 0;
			updateFilteredLocations();
		});
		this.addRenderableWidget(searchBox);

		// Category filter buttons
		int catY = startY + 24;
		int catX = centerX - 120;

		this.addRenderableWidget(Button.builder(Component.literal("All"), btn -> {
			selectedCategory = null;
			scrollOffset = 0;
			updateFilteredLocations();
		}).bounds(catX, catY, 40, 16).build());

		List<LocationCategory> cats = ClientLocationData.getCategories();
		int btnX = catX + 44;
		for (LocationCategory cat : cats) {
			final String catId = cat.getId();
			String label = cat.getName();
			int w = Math.max(font.width(label) + 10, 40);
			this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
				selectedCategory = catId;
				scrollOffset = 0;
				updateFilteredLocations();
			}).bounds(btnX, catY, w, 16).build());
			btnX += w + 4;
			if (btnX > centerX + 120) break;
		}

		// Add location button
		Minecraft mc = Minecraft.getInstance();
		long myCount = ClientLocationData.getLocations().stream()
				.filter(l -> mc.player != null && l.getOwnerUUID().equals(mc.player.getUUID()))
				.count();
		int max = ClientLocationData.getMaxLocationsPerPlayer();

		this.addRenderableWidget(Button.builder(
				Component.literal("+ Add Location (" + myCount + "/" + max + ")"),
				btn -> mc.setScreen(new AddLocationScreen(this))
		).bounds(centerX - 80, this.height - 50, 160, 20).build());

		// Admin button - only for ops
		if (mc.player != null && mc.player.hasPermissions(2)) {
			this.addRenderableWidget(Button.builder(
					Component.literal("Admin"),
					btn -> mc.setScreen(new AdminScreen(this))
			).bounds(this.width - 70, 6, 60, 16).build());
		}

		// Close button
		this.addRenderableWidget(Button.builder(
				Component.literal("Close"),
				btn -> this.onClose()
		).bounds(centerX - 40, this.height - 26, 80, 20).build());

		updateFilteredLocations();
	}

	private void updateFilteredLocations() {
		String search = searchBox != null ? searchBox.getValue().toLowerCase().trim() : "";
		filteredLocations.clear();
		for (Location loc : ClientLocationData.getLocations()) {
			if (selectedCategory != null && !loc.getCategoryId().equals(selectedCategory))
				continue;
			if (!search.isEmpty() && !loc.getName().toLowerCase().contains(search))
				continue;
			filteredLocations.add(loc);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		// Title
		graphics.drawCenteredString(this.font, "Locations", this.width / 2, 10, 0xFFFFFF);

		super.render(graphics, mouseX, mouseY, partialTick);

		// Location list
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

				// Background
				boolean hovered = mouseX >= listX && mouseX <= listX + listWidth
						&& mouseY >= entryY && mouseY <= entryY + LIST_ENTRY_HEIGHT - 2;
				int bgColor = hovered ? 0x60FFFFFF : 0x40000000;
				graphics.fill(listX, entryY, listX + listWidth, entryY + LIST_ENTRY_HEIGHT - 2, bgColor);

				// Category icon
				LocationCategory cat = ClientLocationData.getCategoryById(loc.getCategoryId());
				if (cat != null) {
					ItemStack iconStack = getItemStackFromId(cat.getIcon());
					graphics.renderItem(iconStack, listX + 4, entryY + 3);
				}

				// Name
				String displayName = loc.getName();
				if (displayName.length() > 20) displayName = displayName.substring(0, 18) + "..";
				graphics.drawString(this.font, displayName, listX + 24, entryY + 3, 0xFFFFFF);

				// Owner
				graphics.drawString(this.font, "by " + loc.getOwnerName(), listX + 24, entryY + 13, 0xAAAAAA);

				// Track button area
				int trackBtnX = listX + listWidth - 50;
				int trackBtnW = 40;
				boolean trackHovered = mouseX >= trackBtnX && mouseX <= trackBtnX + trackBtnW
						&& mouseY >= entryY + 2 && mouseY <= entryY + 16;
				int trackColor = trackHovered ? 0xFF55FF55 : 0xFF00AA00;
				Location tracked = ClientLocationData.getTrackedLocation();
				boolean isTracked = tracked != null && tracked.getId().equals(loc.getId());
				String trackText = isTracked ? "Stop" : "Track";
				if (isTracked) trackColor = trackHovered ? 0xFFFF5555 : 0xFFAA0000;
				graphics.fill(trackBtnX, entryY + 2, trackBtnX + trackBtnW, entryY + 16, trackColor);
				graphics.drawCenteredString(this.font, trackText, trackBtnX + trackBtnW / 2, entryY + 5, 0xFFFFFF);

				// Delete button for own locations
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null && loc.getOwnerUUID().equals(mc.player.getUUID())) {
					int delBtnX = trackBtnX - 18;
					boolean delHovered = mouseX >= delBtnX && mouseX <= delBtnX + 14
							&& mouseY >= entryY + 2 && mouseY <= entryY + 16;
					int delColor = delHovered ? 0xFFFF3333 : 0xFFAA0000;
					graphics.fill(delBtnX, entryY + 2, delBtnX + 14, entryY + 16, delColor);
					graphics.drawCenteredString(this.font, "X", delBtnX + 7, entryY + 5, 0xFFFFFF);
				}

				// Hover tooltip
				if (hovered) {
					List<Component> tooltip = new ArrayList<>();
					tooltip.add(Component.literal("§e" + loc.getName()));
					tooltip.add(Component.literal("§7X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ()));
					tooltip.add(Component.literal("§7Dimension: " + formatDimension(loc.getDimension())));
					tooltip.add(Component.literal("§7Owner: " + loc.getOwnerName()));
					if (cat != null) {
						tooltip.add(Component.literal("§7Category: " + cat.getName()));
					}
					graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
				}
			}

			// Scroll indicators
			if (scrollOffset > 0) {
				graphics.drawCenteredString(this.font, "▲", this.width / 2, listY - 10, 0xAAAAAA);
			}
			if (scrollOffset < maxScroll) {
				graphics.drawCenteredString(this.font, "▼", this.width / 2, listY + LIST_VISIBLE_COUNT * LIST_ENTRY_HEIGHT, 0xAAAAAA);
			}
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

				// Track button click
				int trackBtnX = listX + listWidth - 50;
				if (mouseX >= trackBtnX && mouseX <= trackBtnX + 40
						&& mouseY >= entryY + 2 && mouseY <= entryY + 16) {
					Location tracked = ClientLocationData.getTrackedLocation();
					if (tracked != null && tracked.getId().equals(loc.getId())) {
						ClientLocationData.clearTracking();
					} else {
						ClientLocationData.setTrackedLocation(loc);
					}
					return true;
				}

				// Delete button click
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null && loc.getOwnerUUID().equals(mc.player.getUUID())) {
					int delBtnX = trackBtnX - 18;
					if (mouseX >= delBtnX && mouseX <= delBtnX + 14
							&& mouseY >= entryY + 2 && mouseY <= entryY + 16) {
						PlayerPlacesMod.PACKET_HANDLER.sendToServer(new RemoveLocationPacket(loc.getId()));
						return true;
					}
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int maxScroll = Math.max(0, filteredLocations.size() - LIST_VISIBLE_COUNT);
		if (delta > 0) {
			scrollOffset = Math.max(0, scrollOffset - 1);
		} else if (delta < 0) {
			scrollOffset = Math.min(maxScroll, scrollOffset + 1);
		}
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static ItemStack getItemStackFromId(String itemId) {
		try {
			ResourceLocation rl = new ResourceLocation(itemId);
			Item item = ForgeRegistries.ITEMS.getValue(rl);
			if (item != null) {
				return new ItemStack(item);
			}
		} catch (Exception ignored) {
		}
		return ItemStack.EMPTY;
	}

	private static String formatDimension(String dim) {
		if (dim.contains("overworld")) return "Overworld";
		if (dim.contains("the_nether")) return "Nether";
		if (dim.contains("the_end")) return "The End";
		return dim;
	}
}