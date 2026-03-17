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

	// Max render distance — beyond this we pull the visuals closer
	private static final double MAX_RENDER_DIST = 200.0;

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

		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
		float cameraYaw = mc.gameRenderer.getMainCamera().getYRot();

		double dx = targetX - cameraPos.x();
		double dy = targetY - cameraPos.y();
		double dz = targetZ - cameraPos.z();
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (distance < 10)
			return;

		// === KEY FIX: If too far, pull render position closer along the direction vector ===
		// This keeps the visual inside the frustum / render range
		float relX, relY, relZ;
		double renderDist = distance;
		if (distance > MAX_RENDER_DIST) {
			// Normalize direction and place at MAX_RENDER_DIST
			double scale = MAX_RENDER_DIST / distance;
			relX = (float) (dx * scale);
			relY = (float) (dy * scale);
			relZ = (float) (dz * scale);
			renderDist = MAX_RENDER_DIST;
		} else {
			relX = (float) dx;
			relY = (float) dy;
			relZ = (float) dz;
		}

		// Distance-based color
		float[] color = getDistanceColor(distance);
		float cr = color[0];
		float cg = color[1];
		float cb = color[2];

		PoseStack poseStack = event.getPoseStack();
		Matrix4f projectionMatrix = event.getProjectionMatrix();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();

		// Scale factors — when pulled closer, make things smaller proportionally
		// so they don't look gigantic. But still visible.
		// Scale factors
		float distFactor = (float) (distance / renderDist);

		// Calculate icon height FIRST (needed by beam)
		float iconBaseHeight;
		float iconScale;
		if (distance <= MAX_RENDER_DIST) {
			iconBaseHeight = 25.0f + (float) (distance * 0.05);
			iconScale = (float) (5 + distance * 0.07);
		} else {
			iconBaseHeight = (float) (25.0 + MAX_RENDER_DIST * 0.05) / distFactor;
			iconBaseHeight = Math.max(8.0f, iconBaseHeight);
			iconScale = (float) ((5 + distance * 0.07) / distFactor);
			iconScale = Math.max(3.0f, Math.min(iconScale, 20.0f));
		}

		// 1. Ring
		if (distance <= MAX_RENDER_DIST) {
			renderRing(poseStack, projectionMatrix, relX, relY + 0.52f, relZ, cr, cg, cb, 60.0f);
		} else {
			float ringScale = (float) (60.0 / distFactor * 0.3);
			renderRing(poseStack, projectionMatrix, relX, relY + 0.02f, relZ, cr, cg, cb, ringScale);
		}

		// 2. Beam — starts above icon
		if (distance > 100) {
			float beamWidthScale;
			float beamHeightScale;
			if (distance <= MAX_RENDER_DIST) {
				beamWidthScale = (float) (3 + distance * 0.04);
				beamHeightScale = 1.0f;
			} else {
				beamWidthScale = (float) (3 + MAX_RENDER_DIST * 0.02);
				beamHeightScale = (float) (1.0 / distFactor);
			}
			renderBeam(poseStack, projectionMatrix, relX, relY + iconBaseHeight + 6, relZ, cameraYaw, beamWidthScale, beamHeightScale, cr, cg, cb);
		}

		// 3. Icon — depth test disabled so it renders on top
		renderIcon(poseStack, projectionMatrix, relX, relY + iconBaseHeight, relZ, cameraYaw, iconScale, cr, cg, cb);

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
}

	private static float[] getDistanceColor(double distance) {
		float r, g, b = 0;

		if (distance <= 500) {
			r = (float) (distance / 500.0 * 0.8);
			g = 1.0f;
		} else if (distance <= 1500) {
			double t = (distance - 500.0) / 1000.0;
			r = Math.min(1.0f, (float) (0.8 + t * 0.2));
			g = Math.max(0.0f, (float) (1.0 - t));
		} else {
			double t = (distance - 1500.0) / 2000.0;
			r = Math.max(0.4f, (float) (1.0 - t * 0.6));
			g = 0;
		}

		return new float[]{r, g, b};
	}

	private static void renderRing(PoseStack poseStack, Matrix4f projection, float x, float y, float z, float cr, float cg, float cb, float ringScale) {
	    VertexBuffer ring = LocationRing.get();
	    if (ring == null) return;
	
	    poseStack.pushPose();
	    poseStack.translate(x, y, z);
	    // Flat ring — Y scale = 1 (ring is already flat, Y=0 in geometry)
	    poseStack.scale(ringScale, 1, ringScale);
	
	    RenderSystem.setShaderColor(cr, cg, cb, 0.6f);
	
	    // Render both sides
	    RenderSystem.disableCull();
	
	    ring.bind();
	    ring.drawWithShader(poseStack.last().pose(), projection, GameRenderer.getPositionColorShader());
	    VertexBuffer.unbind();
	
	    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	    poseStack.popPose();
	}

	private static void renderBeam(PoseStack poseStack, Matrix4f projection, float x, float y, float z, float cameraYaw, float widthScale, float heightScale, float cr, float cg, float cb) {
		VertexBuffer beam = getBeamBuffer();
		if (beam == null) return;

		poseStack.pushPose();
		poseStack.translate(x, y, z);
		poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(cameraYaw));
		poseStack.scale(widthScale, heightScale, 0);

		RenderSystem.setShaderColor(cr, cg, cb, 0.5f);

		beam.bind();
		beam.drawWithShader(poseStack.last().pose(), projection, GameRenderer.getPositionColorShader());
		VertexBuffer.unbind();

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
	}

	private static void renderIcon(PoseStack poseStack, Matrix4f projection, float x, float y, float z, float cameraYaw, float iconScale, float cr, float cg, float cb) {
		VertexBuffer icon = LocationMark.get();
		if (icon == null) return;

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.setShaderTexture(0, LOCATION_ICON);

		poseStack.pushPose();
		poseStack.translate(x, y, z);
		poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(cameraYaw));
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