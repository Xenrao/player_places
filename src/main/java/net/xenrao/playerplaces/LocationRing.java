package net.xenrao.playerplaces;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

public class LocationRing {
	private static VertexBuffer vertexBuffer = null;

	public static VertexBuffer get() {
		if (vertexBuffer == null) {
			build();
		}
		return vertexBuffer;
	}

	private static void build() {
		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		double majorRadius = 0.55;
		double minorRadius = 0.08;
		int majorSteps = 90;
		int minorSteps = 16;
		double majorStep = 360.0 / majorSteps;
		double minorStep = 360.0 / minorSteps;

		for (int i = 0; i < majorSteps; i++) {
			double a0 = Math.toRadians(i * majorStep);
			double a1 = Math.toRadians((i + 1) * majorStep);

			for (int j = 0; j < minorSteps; j++) {
				double b0 = Math.toRadians(j * minorStep);
				double b1 = Math.toRadians((j + 1) * minorStep);

				int c0 = clamp((int) (208 + 48 * Math.sin(b1)));
				int c1 = clamp((int) (208 + 48 * Math.sin(b0)));

				int color0 = 255 << 24 | c0 << 16 | c0 << 8 | c0;
				int color1 = 255 << 24 | c1 << 16 | c1 << 8 | c1;

				double x00 = Math.sin(a0) * (majorRadius + minorRadius * Math.cos(b1));
				double y00 = minorRadius * Math.sin(b1);
				double z00 = Math.cos(a0) * (majorRadius + minorRadius * Math.cos(b1));

				double x01 = Math.sin(a0) * (majorRadius + minorRadius * Math.cos(b0));
				double y01 = minorRadius * Math.sin(b0);
				double z01 = Math.cos(a0) * (majorRadius + minorRadius * Math.cos(b0));

				double x10 = Math.sin(a1) * (majorRadius + minorRadius * Math.cos(b0));
				double y10 = minorRadius * Math.sin(b0);
				double z10 = Math.cos(a1) * (majorRadius + minorRadius * Math.cos(b0));

				double x11 = Math.sin(a1) * (majorRadius + minorRadius * Math.cos(b1));
				double y11 = minorRadius * Math.sin(b1);
				double z11 = Math.cos(a1) * (majorRadius + minorRadius * Math.cos(b1));

				builder.vertex(x00, y00, z00).color(color0).endVertex();
				builder.vertex(x01, y01, z01).color(color1).endVertex();
				builder.vertex(x10, y10, z10).color(color1).endVertex();
				builder.vertex(x11, y11, z11).color(color0).endVertex();
			}
		}

		if (vertexBuffer != null) vertexBuffer.close();
		vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		vertexBuffer.bind();
		vertexBuffer.upload(builder.end());
		VertexBuffer.unbind();
	}

	private static int clamp(int value) {
		return Math.max(0, Math.min(255, value));
	}

	public static void cleanup() {
		if (vertexBuffer != null) {
			vertexBuffer.close();
			vertexBuffer = null;
		}
	}
}