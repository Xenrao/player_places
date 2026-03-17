package net.xenrao.playerplaces;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = PlayerPlacesMod.MODID, value = Dist.CLIENT)
public class WorldTrackingRenderer {

	private static final ResourceLocation LOCATION_ICON = new ResourceLocation(PlayerPlacesMod.MODID, "textures/location.png");
	private static VertexBuffer beamBuffer = null;

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Location tracked = ClientLocationData.getTrackedLocation();
		if (tracked == null)
			return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return;

		String playerDim = mc.player.level().dimension().location().toString();
		if (!playerDim.equals(tracked.getDimension()))
			return;

		double targetX = tracked.getX() + 0.5;
		double targetY = tracked.getY();
		double targetZ = tracked.getZ() + 0.5;

		double dx = targetX - mc.player.getX();
		double dy = targetY - mc.player.getY();
		double dz = targetZ - mc.player.getZ();
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (distance < 10)
			return;

		// Distance-based color: green → yellow → red
		float[] color = getDistanceColor(distance);
		float cr = color[0];
		float cg = color[1];
		float cb = color[2];

		PoseStack poseStack = event.getPoseStack();
		Matrix4f projectionMatrix = event.getProjectionMatrix();
		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
		float cameraYaw = mc.gameRenderer.getMainCamera().getYRot();

		float relX = (float) (targetX - cameraPos.x());
		float relY = (float) (targetY - cameraPos.y());
		float relZ = (float) (targetZ - cameraPos.z());

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();

		// 1. Ring — flat on the ground, Y+0.02 to avoid z-fighting
		renderRing(poseStack, projectionMatrix, relX, relY + 0.6f, relZ, cr, cg, cb);

		// 2. Beam — only when far
		if (distance > 300) {
			renderBeam(poseStack, projectionMatrix, relX, relY, relZ, cameraYaw, distance, cr, cg, cb);
		}

		// 3. Icon — high above target, scales with distance
		float iconHeight = 25.0f + (float) (distance * 0.05);
		renderIcon(poseStack, projectionMatrix, relX, relY + iconHeight, relZ, cameraYaw, distance, cr, cg, cb);

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/**
	 * Returns {R, G, B} in 0-1 range based on distance.
	 * Close (0-500): green
	 * Mid (500-1500): green → yellow → red
	 * Far (1500+): red (minimum 0.4)
	 */
	private static float[] getDistanceColor(double distance) {
		float r, g, b = 0;

		if (distance <= 500) {
			// Green
			r = (float) (distance / 500.0 * 0.8);
			g = 1.0f;
		} else if (distance <= 1500) {
			// Green → Yellow → Red
			double t = (distance - 500.0) / 1000.0;
			r = Math.min(1.0f, (float) (0.8 + t * 0.2));
			g = Math.max(0.0f, (float) (1.0 - t));
		} else {
			// Red, but not too dark
			double t = (distance - 1500.0) / 2000.0;
			r = Math.max(0.4f, (float) (1.0 - t * 0.6));
			g = 0;
		}

		return new float[]{r, g, b};
	}

	private static void renderRing(PoseStack poseStack, Matrix4f projection, float x, float y, float z, float cr, float cg, float cb) {
		VertexBuffer ring = LocationRing.get();
		if (ring == null) return;

		poseStack.pushPose();
		poseStack.translate(x, y, z);
		float ringScale = 60.0f;
		poseStack.scale(ringScale, 0, ringScale);

		RenderSystem.setShaderColor(cr, cg, cb, 0.3f);

		ring.bind();
		ring.drawWithShader(poseStack.last().pose(), projection, GameRenderer.getPositionColorShader());
		VertexBuffer.unbind();

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
	}

	private static void renderBeam(PoseStack poseStack, Matrix4f projection, float x, float y, float z, float cameraYaw, double distance, float cr, float cg, float cb) {
		VertexBuffer beam = getBeamBuffer();
		if (beam == null) return;

		poseStack.pushPose();
		poseStack.translate(x, y + 10, z);
		poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(cameraYaw));
		float widthScale = (float) (3 + distance * 0.04);
		poseStack.scale(widthScale, 1, 0);

		RenderSystem.setShaderColor(cr, cg, cb, 0.5f);

		beam.bind();
		beam.drawWithShader(poseStack.last().pose(), projection, GameRenderer.getPositionColorShader());
		VertexBuffer.unbind();

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
	}

	private static void renderIcon(PoseStack poseStack, Matrix4f projection, float x, float y, float z, float cameraYaw, double distance, float cr, float cg, float cb) {
		VertexBuffer icon = LocationMark.get();
		if (icon == null) return;

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.setShaderTexture(0, LOCATION_ICON);

		poseStack.pushPose();
		poseStack.translate(x, y, z);
		poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(cameraYaw));
		float iconScale = (float) (5 + distance * 0.07);
		poseStack.scale(iconScale, iconScale, 0);

		RenderSystem.setShaderColor(cr, cg, cb, 1.0f);

		icon.bind();
		icon.drawWithShader(poseStack.last().pose(), projection, GameRenderer.getPositionTexColorShader());
		VertexBuffer.unbind();

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		poseStack.popPose();
	}

	private static VertexBuffer getBeamBuffer() {
		if (beamBuffer == null) {
			buildBeam();
		}
		return beamBuffer;
	}

	private static void buildBeam() {
		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		int white = 0xFFFFFFFF;
		float halfWidth = 0.5f;
		float height = 500f;

		builder.vertex(halfWidth, height, 0).color(white).endVertex();
		builder.vertex(halfWidth, 0, 0).color(white).endVertex();
		builder.vertex(-halfWidth, 0, 0).color(white).endVertex();
		builder.vertex(-halfWidth, height, 0).color(white).endVertex();

		if (beamBuffer != null) beamBuffer.close();
		beamBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		beamBuffer.bind();
		beamBuffer.upload(builder.end());
		VertexBuffer.unbind();
	}

	public static void cleanup() {
		LocationMark.cleanup();
		LocationRing.cleanup();
		if (beamBuffer != null) {
			beamBuffer.close();
			beamBuffer = null;
		}
	}
}