package net.xenrao.playerplaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminScreen extends Screen {
	private final Screen parent;
	private int currentTab = 0;
	private int scrollOffset = 0;
	private EditBox searchBox;
	private EditBox maxLocationsBox;
	private EditBox maxNameLenBox;
	private EditBox maxDescLenBox;
	private EditBox newCatIdBox;
	private EditBox newCatNameBox;
	private EditBox newCatIconBox;

	private List<Location> filteredLocations = new ArrayList<>();
	private static final int ENTRY_HEIGHT = 22;
	private static final int VISIBLE_COUNT = 6;

	private int lastDataVersion = -1;
	private String savedSearchText = "";

	public AdminScreen(Screen parent) {
		super(Component.literal("Admin Panel"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;

		this.addRenderableWidget(Button.builder(Component.literal("Locations"), btn -> {
			currentTab = 0; scrollOffset = 0; rebuildWidgets();
		}).bounds(centerX - 120, 25, 75, 16).build());

		this.addRenderableWidget(Button.builder(Component.literal("Categories"), btn -> {
			currentTab = 1; scrollOffset = 0; rebuildWidgets();
		}).bounds(centerX - 40, 25, 80, 16).build());

		this.addRenderableWidget(Button.builder(Component.literal("Settings"), btn -> {
			currentTab = 2; scrollOffset = 0; rebuildWidgets();
		}).bounds(centerX + 45, 25, 75, 16).build());

		if (currentTab == 0) initLocationsTab(centerX);
		else if (currentTab == 1) initCategoriesTab(centerX);
		else initSettingsTab(centerX);

		this.addRenderableWidget(Button.builder(
				Component.literal("Back"),
				btn -> Minecraft.getInstance().setScreen(parent)
		).bounds(centerX - 40, this.height - 26, 80, 20).build());
	}

	private void initLocationsTab(int centerX) {
		searchBox = new EditBox(this.font, centerX - 100, 48, 200, 16, Component.literal("Search..."));
		searchBox.setMaxLength(32);
		searchBox.setValue(savedSearchText);
		searchBox.setResponder(text -> { scrollOffset = 0; updateFilteredLocations(); });
		this.addRenderableWidget(searchBox);
		updateFilteredLocations();
	}

	private void initCategoriesTab(int centerX) {
		int inputY = this.height - 80;
		newCatIdBox = new EditBox(this.font, centerX - 120, inputY, 70, 16, Component.literal("ID"));
		newCatIdBox.setMaxLength(16);
		this.addRenderableWidget(newCatIdBox);

		newCatNameBox = new EditBox(this.font, centerX - 44, inputY, 80, 16, Component.literal("Name"));
		newCatNameBox.setMaxLength(24);
		this.addRenderableWidget(newCatNameBox);

		newCatIconBox = new EditBox(this.font, centerX + 42, inputY, 100, 16, Component.literal("Icon"));
		newCatIconBox.setMaxLength(48);
		newCatIconBox.setValue("minecraft:paper");
		this.addRenderableWidget(newCatIconBox);

		this.addRenderableWidget(Button.builder(Component.literal("+ Add"), btn -> {
			String id = newCatIdBox.getValue().trim();
			String name = newCatNameBox.getValue().trim();
			String icon = newCatIconBox.getValue().trim();
			if (!id.isEmpty() && !name.isEmpty()) {
				PlayerPlacesMod.PACKET_HANDLER.sendToServer(
						new AdminActionPacket(AdminActionPacket.ACTION_ADD_CATEGORY, id, name, icon));
			}
		}).bounds(centerX - 30, inputY + 20, 60, 16).build());
	}

	private void initSettingsTab(int centerX) {
		int leftX = centerX - 120;
		int rightX = centerX + 50;
		int y = 60;
		int rowH = 28;

		// Row 1: Max locations per player
		maxLocationsBox = new EditBox(this.font, rightX, y, 40, 16, Component.empty());
		maxLocationsBox.setMaxLength(3);
		maxLocationsBox.setValue(String.valueOf(ClientLocationData.getMaxLocationsPerPlayer()));
		this.addRenderableWidget(maxLocationsBox);
		this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
			PlayerPlacesMod.PACKET_HANDLER.sendToServer(
					new AdminActionPacket(AdminActionPacket.ACTION_SET_MAX_LOCATIONS, "maxLocations", maxLocationsBox.getValue().trim(), ""));
		}).bounds(rightX + 46, y, 40, 16).build());

		// Row 2: Max name length
		y += rowH;
		maxNameLenBox = new EditBox(this.font, rightX, y, 40, 16, Component.empty());
		maxNameLenBox.setMaxLength(3);
		maxNameLenBox.setValue(String.valueOf(ClientLocationData.getMaxNameLength()));
		this.addRenderableWidget(maxNameLenBox);
		this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
			PlayerPlacesMod.PACKET_HANDLER.sendToServer(
					new AdminActionPacket(AdminActionPacket.ACTION_SET_MAX_LOCATIONS, "maxNameLength", maxNameLenBox.getValue().trim(), ""));
		}).bounds(rightX + 46, y, 40, 16).build());

		// Row 3: Max desc length
		y += rowH;
		maxDescLenBox = new EditBox(this.font, rightX, y, 40, 16, Component.empty());
		maxDescLenBox.setMaxLength(3);
		maxDescLenBox.setValue(String.valueOf(ClientLocationData.getMaxDescLength()));
		this.addRenderableWidget(maxDescLenBox);
		this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
			PlayerPlacesMod.PACKET_HANDLER.sendToServer(
					new AdminActionPacket(AdminActionPacket.ACTION_SET_MAX_LOCATIONS, "maxDescLength", maxDescLenBox.getValue().trim(), ""));
		}).bounds(rightX + 46, y, 40, 16).build());
	}

	private void updateFilteredLocations() {
		String search = searchBox != null ? searchBox.getValue().toLowerCase().trim() : "";
		filteredLocations.clear();
		for (Location loc : ClientLocationData.getLocations()) {
			if (!search.isEmpty()
					&& !loc.getOwnerName().toLowerCase().contains(search)
					&& !loc.getName().toLowerCase().contains(search))
				continue;
			filteredLocations.add(loc);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (searchBox != null) savedSearchText = searchBox.getValue();
		int ver = ClientLocationData.getDataVersion();
		if (lastDataVersion == -1) { lastDataVersion = ver; return; }
		if (lastDataVersion != ver) { lastDataVersion = ver; rebuildWidgets(); }
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, "Admin Panel", this.width / 2, 8, 0xFFAA00);
		super.render(graphics, mouseX, mouseY, partialTick);
		if (currentTab == 0) renderLocationsTab(graphics, mouseX, mouseY);
		else if (currentTab == 1) renderCategoriesTab(graphics, mouseX, mouseY);
		else renderSettingsTab(graphics, mouseX, mouseY);
	}

	private void renderLocationsTab(GuiGraphics graphics, int mouseX, int mouseY) {
		int listX = this.width / 2 - 140;
		int listY = 72;
		int listWidth = 280;

		if (filteredLocations.isEmpty()) {
			graphics.drawCenteredString(this.font, "No locations found.", this.width / 2, listY + 30, 0x888888);
			return;
		}
		int maxScroll = Math.max(0, filteredLocations.size() - VISIBLE_COUNT);
		if (scrollOffset > maxScroll) scrollOffset = maxScroll;

		for (int i = 0; i < VISIBLE_COUNT; i++) {
			int index = i + scrollOffset;
			if (index >= filteredLocations.size()) break;
			Location loc = filteredLocations.get(index);
			int entryY = listY + i * ENTRY_HEIGHT;

			boolean hovered = mouseX >= listX && mouseX <= listX + listWidth
					&& mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT - 2;
			graphics.fill(listX, entryY, listX + listWidth, entryY + ENTRY_HEIGHT - 2, hovered ? 0x60FFFFFF : 0x40000000);

			String display = loc.getOwnerName() + " - " + loc.getName();
			if (display.length() > 30) display = display.substring(0, 28) + "..";
			graphics.drawString(this.font, display, listX + 4, entryY + 3, 0xFFFFFF);

			LocationCategory cat = ClientLocationData.getCategoryById(loc.getCategoryId());
			graphics.drawString(this.font, "(" + (cat != null ? cat.getName() : "?") + ")", listX + 4, entryY + 12, 0x888888);

			int editX = listX + listWidth - 70;
			boolean editHov = mouseX >= editX && mouseX <= editX + 30 && mouseY >= entryY + 2 && mouseY <= entryY + 16;
			int buttonY = entryY + 3;
			
			graphics.fill(editX, buttonY, editX + 30, buttonY + 14, editHov ? 0xFF5555FF : 0xFF3333AA);
			graphics.drawCenteredString(this.font, "Edit", editX + 15, buttonY + 3, 0xFFFFFF);

			int delX = listX + listWidth - 34;
			boolean delHov = mouseX >= delX && mouseX <= delX + 30 && mouseY >= entryY + 2 && mouseY <= entryY + 16;
			graphics.fill(delX, buttonY, delX + 30, buttonY + 14, delHov ? 0xFFFF3333 : 0xFFAA0000);
			graphics.drawCenteredString(this.font, "Del", delX + 15, buttonY + 3, 0xFFFFFF);

			if (hovered && mouseX < editX) {
				List<Component> tooltip = new ArrayList<>();
				tooltip.add(Component.literal("\u00A7e" + loc.getName()));
				if (!loc.getDescription().isEmpty()) tooltip.add(Component.literal("\u00A7f" + loc.getDescription()));
				tooltip.add(Component.literal("\u00A77X:" + loc.getX() + " Y:" + loc.getY() + " Z:" + loc.getZ()));
				tooltip.add(Component.literal("\u00A77Dim: " + loc.getDimension()));
				tooltip.add(Component.literal("\u00A77Owner: " + loc.getOwnerName()));
				graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
			}
		}
	}

	private void renderCategoriesTab(GuiGraphics graphics, int mouseX, int mouseY) {
		int listX = this.width / 2 - 120;
		int listY = 55;
		int listWidth = 240;
		List<LocationCategory> cats = ClientLocationData.getCategories();
		for (int i = 0; i < cats.size(); i++) {
			LocationCategory cat = cats.get(i);
			int entryY = listY + i * ENTRY_HEIGHT;
			graphics.fill(listX, entryY, listX + listWidth, entryY + ENTRY_HEIGHT - 2, 0x40000000);
			graphics.drawString(this.font, cat.getName() + " (" + cat.getId() + ")", listX + 4, entryY + 3, 0xFFFFFF);
			graphics.drawString(this.font, "Icon: " + cat.getIcon(), listX + 4, entryY + 12, 0x888888);
			int delX = listX + listWidth - 30;
			boolean delHov = mouseX >= delX && mouseX <= delX + 26 && mouseY >= entryY + 2 && mouseY <= entryY + 16;
			int buttonY = entryY + 3;
			graphics.fill(delX, buttonY, delX + 26, buttonY + 14, delHov ? 0xFFFF3333 : 0xFFAA0000);
			graphics.drawCenteredString(this.font, "Del", delX + 13, buttonY + 3, 0xFFFFFF);
		}
		graphics.drawString(this.font, "Add New Category:", this.width / 2 - 120, this.height - 96, 0xAAAAAA);
	}

	private void renderSettingsTab(GuiGraphics graphics, int mouseX, int mouseY) {
		int centerX = this.width / 2;
		int leftX = centerX - 120;
		int y = 60;
		int rowH = 28;

		graphics.drawString(this.font, "Max locations per player:", leftX -15, y + 4, 0xFFFFFF);
		graphics.drawString(this.font, "Max name length:", leftX-15, y + rowH + 4, 0xFFFFFF);
		graphics.drawString(this.font, "Max description length:", leftX-15, y + rowH * 2 + 4, 0xFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && currentTab == 0) {
			int listX = this.width / 2 - 140;
			int listY = 72;
			int listWidth = 280;
			for (int i = 0; i < VISIBLE_COUNT; i++) {
				int index = i + scrollOffset;
				if (index >= filteredLocations.size()) break;
				Location loc = filteredLocations.get(index);
				int entryY = listY + i * ENTRY_HEIGHT;
				int editX = listX + listWidth - 70;
				int buttonY = entryY + 3;
				if (mouseX >= editX && mouseX <= editX + 30 && mouseY >= buttonY && mouseY <= buttonY + 14){
					Minecraft.getInstance().setScreen(new AdminEditScreen(this, loc));
					return true;
				}
				int delX = listX + listWidth - 34;
				if (mouseX >= delX && mouseX <= delX + 30 && mouseY >= buttonY && mouseY <= buttonY + 14) {
					PlayerPlacesMod.PACKET_HANDLER.sendToServer(
							new AdminActionPacket(AdminActionPacket.ACTION_DELETE_LOCATION, loc.getId(), "", ""));
					return true;
				}
			}
		}
		if (button == 0 && currentTab == 1) {
			int listX = this.width / 2 - 120;
			int listY = 55;
			int listWidth = 240;
			List<LocationCategory> cats = ClientLocationData.getCategories();
			for (int i = 0; i < cats.size(); i++) {
				int entryY = listY + i * ENTRY_HEIGHT;
				int delX = listX + listWidth - 30;
				int buttonY = entryY + 4;
				if (mouseX >= delX && mouseX <= delX + 26 && mouseY >= buttonY && mouseY <= buttonY + 14){
					PlayerPlacesMod.PACKET_HANDLER.sendToServer(
							new AdminActionPacket(AdminActionPacket.ACTION_REMOVE_CATEGORY, cats.get(i).getId(), "", ""));
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (currentTab == 0) {
			int maxScroll = Math.max(0, filteredLocations.size() - VISIBLE_COUNT);
			if (delta > 0) scrollOffset = Math.max(0, scrollOffset - 1);
			else scrollOffset = Math.min(maxScroll, scrollOffset + 1);
		}
		return true;
	}

	@Override
	public boolean isPauseScreen() { return false; }
}