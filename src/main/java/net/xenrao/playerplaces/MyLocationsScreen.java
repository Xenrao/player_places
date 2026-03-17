package net.xenrao.playerplaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MyLocationsScreen extends Screen {
	private final Screen parent;
	private int scrollOffset = 0;
	private List<Location> myLocations = new ArrayList<>();
	private static final int ENTRY_HEIGHT = 24;
	private static final int VISIBLE_COUNT = 6;
	private int lastDataVersion = -1;

	public MyLocationsScreen(Screen parent) {
		super(Component.literal("My Locations"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		Minecraft mc = Minecraft.getInstance();

		myLocations.clear();
		if (mc.player != null) {
			String myName = mc.player.getGameProfile().getName();
			for (Location loc : ClientLocationData.getLocations()) {
				if (loc.getOwnerName().equalsIgnoreCase(myName)) {
					myLocations.add(loc);
				}
			}
		}

		int max = ClientLocationData.getMaxLocationsPerPlayer();
		this.addRenderableWidget(Button.builder(
				Component.literal("+ Add Location (" + myLocations.size() + "/" + max + ")"),
				btn -> mc.setScreen(new AddLocationScreen(this))
		).bounds(centerX - 80, this.height - 50, 160, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Back"),
				btn -> mc.setScreen(parent)
		).bounds(centerX - 40, this.height - 26, 80, 20).build());
	}

	@Override
	public void tick() {
		super.tick();
		int ver = ClientLocationData.getDataVersion();
		if (lastDataVersion == -1) { lastDataVersion = ver; return; }
		if (lastDataVersion != ver) { lastDataVersion = ver; rebuildWidgets(); }
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		graphics.drawCenteredString(this.font, "My Locations", this.width / 2, 10, 0xFFFFFF);
		super.render(graphics, mouseX, mouseY, partialTick);

		int listX = this.width / 2 - 140;
		int listY = 35;
		int listWidth = 280;

		if (myLocations.isEmpty()) {
			graphics.drawCenteredString(this.font, "You have no locations.", this.width / 2, listY + 40, 0x888888);
		} else {
			int maxScroll = Math.max(0, myLocations.size() - VISIBLE_COUNT);
			if (scrollOffset > maxScroll) scrollOffset = maxScroll;

			for (int i = 0; i < VISIBLE_COUNT; i++) {
				int index = i + scrollOffset;
				if (index >= myLocations.size()) break;
				Location loc = myLocations.get(index);
				int entryY = listY + i * ENTRY_HEIGHT;

				boolean hovered = mouseX >= listX && mouseX <= listX + listWidth
						&& mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT - 2;
				graphics.fill(listX, entryY, listX + listWidth, entryY + ENTRY_HEIGHT - 2, hovered ? 0x60FFFFFF : 0x40000000);

				String displayName = loc.getName();
				if (displayName.length() > 22) displayName = displayName.substring(0, 20) + "..";
				graphics.drawString(this.font, displayName, listX + 4, entryY + 3, 0xFFFFFF);

				LocationCategory cat = ClientLocationData.getCategoryById(loc.getCategoryId());
				String catName = cat != null ? cat.getName() : "?";
				graphics.drawString(this.font, catName + " | X:" + loc.getX() + " Z:" + loc.getZ(), listX + 4, entryY + 13, 0xAAAAAA);

				int editX = listX + listWidth - 70;
				boolean editHov = mouseX >= editX && mouseX <= editX + 30 && mouseY >= entryY + 2 && mouseY <= entryY + 16;
				graphics.fill(editX, entryY + 2, editX + 30, entryY + 16, editHov ? 0xFF5555FF : 0xFF3333AA);
				graphics.drawCenteredString(this.font, "Edit", editX + 15, entryY + 5, 0xFFFFFF);

				int delX = listX + listWidth - 34;
				boolean delHov = mouseX >= delX && mouseX <= delX + 30 && mouseY >= entryY + 2 && mouseY <= entryY + 16;
				graphics.fill(delX, entryY + 2, delX + 30, entryY + 16, delHov ? 0xFFFF3333 : 0xFFAA0000);
				graphics.drawCenteredString(this.font, "Del", delX + 15, entryY + 5, 0xFFFFFF);

				if (hovered && mouseX < editX) {
					List<Component> tooltip = new ArrayList<>();
					tooltip.add(Component.literal("\u00A7e" + loc.getName()));
					if (!loc.getDescription().isEmpty()) tooltip.add(Component.literal("\u00A7f" + loc.getDescription()));
					tooltip.add(Component.literal("\u00A77X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ()));
					tooltip.add(Component.literal("\u00A77Dimension: " + formatDimension(loc.getDimension())));
					if (cat != null) tooltip.add(Component.literal("\u00A77Category: " + cat.getName()));
					graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
				}
			}

			if (scrollOffset > 0) graphics.drawCenteredString(this.font, "\u25B2", this.width / 2, listY - 10, 0xAAAAAA);
			int maxSc = Math.max(0, myLocations.size() - VISIBLE_COUNT);
			if (scrollOffset < maxSc) graphics.drawCenteredString(this.font, "\u25BC", this.width / 2, listY + VISIBLE_COUNT * ENTRY_HEIGHT, 0xAAAAAA);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			int listX = this.width / 2 - 140;
			int listY = 35;
			int listWidth = 280;
			for (int i = 0; i < VISIBLE_COUNT; i++) {
				int index = i + scrollOffset;
				if (index >= myLocations.size()) break;
				Location loc = myLocations.get(index);
				int entryY = listY + i * ENTRY_HEIGHT;

				int editX = listX + listWidth - 70;
				if (mouseX >= editX && mouseX <= editX + 30 && mouseY >= entryY + 2 && mouseY <= entryY + 16) {
					Minecraft.getInstance().setScreen(new PlayerEditScreen(this, loc));
					return true;
				}
				int delX = listX + listWidth - 34;
				if (mouseX >= delX && mouseX <= delX + 30 && mouseY >= entryY + 2 && mouseY <= entryY + 16) {
					PlayerPlacesMod.PACKET_HANDLER.sendToServer(new RemoveLocationPacket(loc.getId()));
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int maxScroll = Math.max(0, myLocations.size() - VISIBLE_COUNT);
		if (delta > 0) scrollOffset = Math.max(0, scrollOffset - 1);
		else scrollOffset = Math.min(maxScroll, scrollOffset + 1);
		return true;
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