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
	private EditBox descBox;
	private EditBox ownerNameBox;
	private EditBox xBox;
	private EditBox yBox;
	private EditBox zBox;
	private EditBox dimensionBox;
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
		int leftCol = centerX - 150;
		int rightCol = centerX + 10;
		int y = 32;

		// Left column
		nameBox = new EditBox(this.font, leftCol, y, 140, 16, Component.literal("Name"));
		nameBox.setMaxLength(64);
		nameBox.setValue(location.getName());
		this.addRenderableWidget(nameBox);

		descBox = new EditBox(this.font, leftCol, y + 28, 140, 16, Component.literal("Desc"));
		descBox.setMaxLength(128);
		descBox.setValue(location.getDescription());
		this.addRenderableWidget(descBox);

		ownerNameBox = new EditBox(this.font, leftCol, y + 56, 140, 16, Component.literal("Owner"));
		ownerNameBox.setMaxLength(32);
		ownerNameBox.setValue(location.getOwnerName());
		this.addRenderableWidget(ownerNameBox);

		// Right column - Position
		xBox = new EditBox(this.font, rightCol, y, 40, 16, Component.literal("X"));
		xBox.setMaxLength(7);
		xBox.setValue(String.valueOf(location.getX()));
		this.addRenderableWidget(xBox);

		yBox = new EditBox(this.font, rightCol + 50, y, 40, 16, Component.literal("Y"));
		yBox.setMaxLength(4);
		yBox.setValue(String.valueOf(location.getY()));
		this.addRenderableWidget(yBox);

		zBox = new EditBox(this.font, rightCol + 100, y, 40, 16, Component.literal("Z"));
		zBox.setMaxLength(7);
		zBox.setValue(String.valueOf(location.getZ()));
		this.addRenderableWidget(zBox);

		dimensionBox = new EditBox(this.font, rightCol, y + 28, 140, 16, Component.literal("Dimension"));
		dimensionBox.setMaxLength(64);
		dimensionBox.setValue(location.getDimension());
		this.addRenderableWidget(dimensionBox);

		// Category buttons
		List<LocationCategory> cats = ClientLocationData.getCategories();
		int catY = y + 60;
		for (int i = 0; i < cats.size(); i++) {
			LocationCategory cat = cats.get(i);
			final String catId = cat.getId();
			this.addRenderableWidget(Button.builder(
					Component.literal(cat.getName()),
					btn -> { selectedCategory = catId; rebuildWidgets(); }
			).bounds(rightCol + 10, catY + i * 20 + 4, 110, 16).build());
		}

		// Save
		this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
			String newName = nameBox.getValue().trim();
			if (newName.isEmpty()) return;
			String newDesc = descBox.getValue().trim();
			String newOwnerName = ownerNameBox.getValue().trim();
			String newX = xBox.getValue().trim();
			String newY = yBox.getValue().trim();
			String newZ = zBox.getValue().trim();
			String newDim = dimensionBox.getValue().trim();

			// Pack: name|desc|category|ownerName|x|y|z|dimension
			String packed = newName + "|" + newDesc + "|" + selectedCategory + "|"
					+ newOwnerName + "|"
					+ newX + "|" + newY + "|" + newZ + "|" + newDim;

			PlayerPlacesMod.PACKET_HANDLER.sendToServer(
					new AdminActionPacket(AdminActionPacket.ACTION_FULL_EDIT_LOCATION,
							location.getId(), packed, ""));
			Minecraft.getInstance().setScreen(parent);
		}).bounds(centerX - 80, this.height - 30, 70, 20).build());

		// Cancel
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
			Minecraft.getInstance().setScreen(parent);
		}).bounds(centerX + 10, this.height - 30, 70, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, "Edit Location (Admin)", this.width / 2, 8, 0xFFAA00);

		int centerX = this.width / 2;
		int leftCol = centerX - 150;
		int rightCol = centerX + 10;
		int y = 32;

		graphics.drawString(this.font, "Name:", leftCol, y - 9, 0xAAAAAA);
		graphics.drawString(this.font, "Description:", leftCol, y + 19, 0xAAAAAA);
		graphics.drawString(this.font, "Owner Name:", leftCol, y + 47, 0xAAAAAA);

		graphics.drawString(this.font, "X:", rightCol - 10, y + 4, 0xAAAAAA);
		graphics.drawString(this.font, "Y:", rightCol + 40, y + 4, 0xAAAAAA);
		graphics.drawString(this.font, "Z:", rightCol + 90, y + 4, 0xAAAAAA);
		graphics.drawString(this.font, "Dimension:", rightCol, y + 19, 0xAAAAAA);
		graphics.drawString(this.font, "Category:", rightCol, y + 49, 0xAAAAAA);

		List<LocationCategory> cats = ClientLocationData.getCategories();
		int catY = y + 60;
		for (int i = 0; i < cats.size(); i++) {
			if (cats.get(i).getId().equals(selectedCategory)) {
				graphics.drawString(this.font, ">>", rightCol + 2, catY + i * 20 + 8, 0x55FF55);
			}
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() { return false; }
}