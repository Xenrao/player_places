package net.xenrao.playerplaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AdminEditScreen extends Screen {
	private final Screen parent;
	private final Location location;
	private EditBox nameBox;
	private String selectedCategory;

	public AdminEditScreen(Screen parent, Location location) {
		super(Component.literal("Edit Location"));
		this.parent = parent;
		this.location = location;
		this.selectedCategory = location.getCategoryId();
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;

		// Name edit
		nameBox = new EditBox(this.font, centerX - 80, 55, 160, 18, Component.literal("Name"));
		nameBox.setMaxLength(32);
		nameBox.setValue(location.getName());
		this.addRenderableWidget(nameBox);

		// Category selection
		List<LocationCategory> cats = ClientLocationData.getCategories();
		int catY = 95;
		for (int i = 0; i < cats.size(); i++) {
			LocationCategory cat = cats.get(i);
			final String catId = cat.getId();
			this.addRenderableWidget(Button.builder(
					Component.literal(cat.getName()),
					btn -> {
						selectedCategory = catId;
						rebuildWidgets();
					}
			).bounds(centerX - 60, catY + i * 22, 120, 18).build());
		}

		// Save
		this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
			String newName = nameBox.getValue().trim();
			if (newName.isEmpty()) return;
			PlayerPlacesMod.PACKET_HANDLER.sendToServer(
					new AdminActionPacket(AdminActionPacket.ACTION_EDIT_LOCATION,
							location.getId(), newName, selectedCategory));
			Minecraft.getInstance().setScreen(parent);
		}).bounds(centerX - 80, this.height - 50, 70, 20).build());

		// Cancel
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
			Minecraft.getInstance().setScreen(parent);
		}).bounds(centerX + 10, this.height - 50, 70, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, "Edit Location", this.width / 2, 15, 0xFFAA00);

		int centerX = this.width / 2;
		graphics.drawString(this.font, "Name:", centerX - 80, 44, 0xAAAAAA);
		graphics.drawString(this.font, "Category:", centerX - 80, 83, 0xAAAAAA);

		// Selected indicator
		List<LocationCategory> cats = ClientLocationData.getCategories();
		int catY = 95;
		for (int i = 0; i < cats.size(); i++) {
			if (cats.get(i).getId().equals(selectedCategory)) {
				graphics.drawString(this.font, ">>", centerX - 75, catY + i * 22 + 4, 0x55FF55);
			}
		}

		// Location info
		int infoY = catY + cats.size() * 22 + 10;
		graphics.drawString(this.font, "Owner: " + location.getOwnerName(), centerX - 80, infoY, 0x888888);
		graphics.drawString(this.font, "Pos: X:" + location.getX() + " Y:" + location.getY() + " Z:" + location.getZ(),
				centerX - 80, infoY + 12, 0x888888);
		graphics.drawString(this.font, "Dim: " + location.getDimension(), centerX - 80, infoY + 24, 0x888888);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}