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
	private EditBox descBox;
	private String selectedCategory = null;
	private String savedName = "";
	private String savedDesc = "";

	public AddLocationScreen(Screen parent) {
		super(Component.literal("Add Location"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int startY = 45;
		Minecraft mc = Minecraft.getInstance();

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

		if (selectedCategory == null && !cats.isEmpty()) {
			selectedCategory = cats.get(0).getId();
		}

		this.addRenderableWidget(Button.builder(
				Component.literal("Save"),
				btn -> {
					String name = nameBox.getValue().trim();
					if (name.isEmpty()) return;
					if (selectedCategory == null) return;
					String desc = descBox.getValue().trim();
					PlayerPlacesMod.PACKET_HANDLER.sendToServer(new AddLocationPacket(name, desc, selectedCategory));
					mc.setScreen(parent);
				}
		).bounds(centerX - 80, this.height - 50, 70, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Cancel"),
				btn -> mc.setScreen(parent)
		).bounds(centerX + 10, this.height - 50, 70, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, "Add New Location", this.width / 2, 15, 0xFFFFFF);

		int centerX = this.width / 2;
		int startY = 45;

		graphics.drawString(this.font, "Name:", centerX - 80, startY - 10, 0xAAAAAA);
		graphics.drawString(this.font, "Description (optional):", centerX - 80, startY + 20, 0xAAAAAA);
		graphics.drawString(this.font, "Category:", centerX - 80, startY + 56, 0xAAAAAA);

		List<LocationCategory> cats = ClientLocationData.getCategories();
		int catY = startY + 68;
		for (int i = 0; i < cats.size(); i++) {
			if (cats.get(i).getId().equals(selectedCategory)) {
				graphics.drawString(this.font, ">>", centerX - 75, catY + i * 22 + 4, 0x55FF55);
			}
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			int infoY = catY + cats.size() * 22 + 8;
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
	public boolean isPauseScreen() { return false; }

	private static String formatDimension(String dim) {
		if (dim.contains("overworld")) return "Overworld";
		if (dim.contains("the_nether")) return "Nether";
		if (dim.contains("the_end")) return "The End";
		return dim;
	}
}