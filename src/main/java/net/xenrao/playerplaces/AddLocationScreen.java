package net.xenrao.playerplaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AddLocationScreen extends Screen {
	private final Screen parent;
	private EditBox nameBox;
	private String selectedCategory = null;

	public AddLocationScreen(Screen parent) {
		super(Component.literal("Add Location"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int startY = 50;

		// Name input
		nameBox = new EditBox(this.font, centerX - 80, startY, 160, 18, Component.literal("Name"));
		nameBox.setMaxLength(32);
		this.addRenderableWidget(nameBox);

		// Category buttons
		List<LocationCategory> cats = ClientLocationData.getCategories();
		int catY = startY + 40;
		for (int i = 0; i < cats.size(); i++) {
			LocationCategory cat = cats.get(i);
			final String catId = cat.getId();
			int btnY = catY + i * 22;
			this.addRenderableWidget(Button.builder(
					Component.literal(cat.getName()),
					btn -> {
						selectedCategory = catId;
						rebuildWidgets();
					}
			).bounds(centerX - 60, btnY, 120, 18).build());
		}

		// Current position info
		Minecraft mc = Minecraft.getInstance();
		int infoY = catY + cats.size() * 22 + 10;
		if (mc.player != null) {
			int px = mc.player.blockPosition().getX();
			int py = mc.player.blockPosition().getY();
			int pz = mc.player.blockPosition().getZ();
			String dim = mc.player.level().dimension().location().toString();
			// We'll draw this in render()
		}

		// Save button
		this.addRenderableWidget(Button.builder(
				Component.literal("Save"),
				btn -> {
					String name = nameBox.getValue().trim();
					if (name.isEmpty()) return;
					if (selectedCategory == null) return;
					PlayerPlacesMod.PACKET_HANDLER.sendToServer(new AddLocationPacket(name, selectedCategory));
					mc.setScreen(parent);
				}
		).bounds(centerX - 80, this.height - 50, 70, 20).build());

		// Cancel button
		this.addRenderableWidget(Button.builder(
				Component.literal("Cancel"),
				btn -> mc.setScreen(parent)
		).bounds(centerX + 10, this.height - 50, 70, 20).build());

		if (selectedCategory == null && !cats.isEmpty()) {
			selectedCategory = cats.get(0).getId();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, "Add New Location", this.width / 2, 20, 0xFFFFFF);

		int centerX = this.width / 2;
		graphics.drawString(this.font, "Name:", centerX - 80, 40, 0xAAAAAA);

		// Category label
		graphics.drawString(this.font, "Category:", centerX - 80, 78, 0xAAAAAA);

		// Selected indicator
		List<LocationCategory> cats = ClientLocationData.getCategories();
		int catY = 90;
		for (int i = 0; i < cats.size(); i++) {
			LocationCategory cat = cats.get(i);
			int btnY = catY + i * 22;
			if (cat.getId().equals(selectedCategory)) {
				graphics.drawString(this.font, ">>", centerX - 75, btnY + 4, 0x55FF55);
			}
		}

		// Current position
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			int infoY = catY + cats.size() * 22 + 14;
			int px = mc.player.blockPosition().getX();
			int py = mc.player.blockPosition().getY();
			int pz = mc.player.blockPosition().getZ();
			String dim = mc.player.level().dimension().location().toString();
			graphics.drawString(this.font, "Position: X:" + px + " Y:" + py + " Z:" + pz, centerX - 80, infoY, 0x55FF55);
			graphics.drawString(this.font, "Dimension: " + formatDimension(dim), centerX - 80, infoY + 12, 0x55FF55);
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static String formatDimension(String dim) {
		if (dim.contains("overworld")) return "Overworld";
		if (dim.contains("the_nether")) return "Nether";
		if (dim.contains("the_end")) return "The End";
		return dim;
	}
}