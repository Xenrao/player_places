package net.xenrao.playerplaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PlayerEditScreen extends Screen {
	private final Screen parent;
	private final Location location;
	private EditBox nameBox;
	private EditBox descBox;
	private String selectedCategory;
	private String savedName;
	private String savedDesc;

	public PlayerEditScreen(Screen parent, Location location) {
		super(Component.literal("Edit Location"));
		this.parent = parent;
		this.location = location;
		this.selectedCategory = location.getCategoryId();
		this.savedName = location.getName();
		this.savedDesc = location.getDescription();
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int startY = 45;

		nameBox = new EditBox(this.font, centerX - 80, startY, 160, 18, Component.literal("Name"));
		nameBox.setMaxLength(ClientLocationData.getMaxNameLength());
		nameBox.setValue(savedName);
		this.addRenderableWidget(nameBox);

		descBox = new EditBox(this.font, centerX - 80, startY + 30, 160, 18, Component.literal("Description"));
		descBox.setMaxLength(ClientLocationData.getMaxDescLength());
		descBox.setValue(savedDesc);
		this.addRenderableWidget(descBox);

		List<LocationCategory> cats = ClientLocationData.getCategories();
		int catY = startY + 68;
		for (int i = 0; i < cats.size(); i++) {
			LocationCategory cat = cats.get(i);
			final String catId = cat.getId();
			this.addRenderableWidget(Button.builder(
					Component.literal(cat.getName()),
					btn -> {
						savedName = nameBox.getValue();
						savedDesc = descBox.getValue();
						selectedCategory = catId;
						rebuildWidgets();
					}
			).bounds(centerX - 60, catY + i * 22, 120, 18).build());
		}

		this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
			String newName = nameBox.getValue().trim();
			if (newName.isEmpty()) return;
			String newDesc = descBox.getValue().trim();
			PlayerPlacesMod.PACKET_HANDLER.sendToServer(
					new EditLocationPacket(location.getId(), newName, newDesc, selectedCategory));
			Minecraft.getInstance().setScreen(parent);
		}).bounds(centerX - 80, this.height - 50, 70, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
			Minecraft.getInstance().setScreen(parent);
		}).bounds(centerX + 10, this.height - 50, 70, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, "Edit My Location", this.width / 2, 15, 0xFFFFFF);

		int centerX = this.width / 2;
		int startY = 45;

		graphics.drawString(this.font, "Name:", centerX - 80, startY - 10, 0xAAAAAA);
		graphics.drawString(this.font, "Description:", centerX - 80, startY + 20, 0xAAAAAA);
		graphics.drawString(this.font, "Category:", centerX - 80, startY + 56, 0xAAAAAA);

		List<LocationCategory> cats = ClientLocationData.getCategories();
		int catY = startY + 68;
		for (int i = 0; i < cats.size(); i++) {
			if (cats.get(i).getId().equals(selectedCategory)) {
				graphics.drawString(this.font, ">>", centerX - 75, catY + i * 22 + 5, 0x55FF55);
			}
		}

		int infoY = catY + cats.size() * 22 + 8;
		graphics.drawString(this.font, "Pos: X:" + location.getX() + " Y:" + location.getY() + " Z:" + location.getZ(),
				centerX - 80, infoY, 0x888888);
		graphics.drawString(this.font, "Dim: " + formatDimension(location.getDimension()), centerX - 80, infoY + 12, 0x888888);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	private static String formatDimension(String dim) {
		if (dim.contains("overworld")) return "Overworld";
		if (dim.contains("the_nether")) return "Nether";
		if (dim.contains("the_end")) return "The End";
		return dim;
	}
}